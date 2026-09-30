package io.github.mengfly.excel.report.template;

import io.github.mengfly.excel.report.util.XmlUtil;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板规范（{@code schema/excel-report-1.0.0.xsd}）回归测试。
 * <p>
 * 框架运行期<b>不</b>做 XSD 校验，模板头部的 {@code xsi:schemaLocation} 只被编辑器使用，
 * 所以这份 XSD 长期没有被任何测试覆盖，逐渐积累了三处<b>语法错误</b>——任何一处都会让
 * 整份 XSD 无法被 {@link SchemaFactory} 编译，编辑器侧的校验与补全因此静默失效。
 * 本测试把它锁住：
 * <ol>
 *     <li>XSD 必须能被标准 JAXP 校验器编译；</li>
 *     <li>仓库自带的所有模板必须通过校验；</li>
 *     <li>负向对照：非法模板必须被拒绝（防止前两条因校验器空转而"假通过"）。</li>
 * </ol>
 * XSD 是模板标签 / 属性 / 样式 key 的封闭定义，改动它会直接影响编辑器的校验与补全，
 * 因此这里失败时应修 XSD（或修模板），不要放宽断言。
 */
public class TemplateSchemaTest {

    private static final String XSD_RESOURCE = "/schema/excel-report-1.0.0.xsd";

    private static final String NS = "http://mengfly.github.io/excel-report/1.0.0";

    /**
     * 仓库自带的模板。这里刻意写死清单而不扫描目录：{@code src/test/resources} 下之后
     * 若新增"故意写错"的负向夹具，扫描会把夹具也当成必须通过的模板。
     */
    private static final String[] TEMPLATES = {
            "Example1Template.xml",
            "IndexSensitivityTemplate.xml",
            "LargeExportTemplate.xml",
            "TestGridLayout.xml",
            "TestImageTemplate.xml",
            "TestLayoutReport.xml",
            "TestTemplate.xml",
    };

    /**
     * XSD 必须能被编译 —— 本次回归的核心。XSD 曾有三处语法错误：
     * {@code GridLayout/count} 同时写 {@code type} 与 {@code simpleType}（互斥）、
     * {@code Table/headerHeight} 对 {@code xs:int} 给了 {@code default="true"}、
     * {@code ChartValueAxis/data} 与 {@code PieValueAxis/data} 重复声明 {@code title}。
     */
    @Test
    public void xsdShouldBeCompilable() throws Exception {
        Assert.assertNotNull("XSD 必须能被 JAXP 编译（SchemaFactory.newSchema 不抛异常）", newSchema());
    }

    /**
     * 仓库自带模板必须全部通过 XSD 校验。
     * <p>
     * 校验用的 DOM 与运行期走同一条解析路径（{@link XmlUtil#readXML(InputStream)}），
     * 以保证测试里的命名空间处理方式与真实模板一致。
     */
    @Test
    public void shippedTemplatesShouldValidate() throws Exception {
        Schema schema = newSchema();
        List<String> failures = new ArrayList<String>();
        for (String name : TEMPLATES) {
            InputStream stream = TemplateSchemaTest.class.getResourceAsStream("/" + name);
            Assert.assertNotNull("测试模板不在 classpath 上: " + name, stream);
            Document document;
            try {
                document = XmlUtil.readXML(stream);
            } finally {
                stream.close();
            }
            List<String> errors = validate(schema, document);
            if (!errors.isEmpty()) {
                failures.add(name + " -> " + errors);
            }
        }
        Assert.assertTrue("以下模板不符合 XSD 规范: " + failures, failures.isEmpty());
    }

    /**
     * 负向对照：证明上面的"全部通过"不是因为校验器空转。
     * <p>
     * 这三种写法在运行期都是<b>静默失效</b>（见 {@code doc/优化建议.md} 二.2）：
     * 未知属性被 {@code BeanUtil} 忽略、未注册的样式 key 被 {@code CellStyles} 丢弃、
     * 未知标签要到渲染时才抛异常。XSD 是唯一能在离线阶段一次抓住它们的手段。
     */
    @Test
    public void invalidTemplatesShouldBeRejected() throws Exception {
        Schema schema = newSchema();

        assertInvalid(schema, "组件上的未知属性",
                template("<container><Text size=\"1,1\" text=\"a\" unknownAttr=\"1\"/></container>"));
        assertInvalid(schema, "未注册的样式 key",
                template("<styles><style id=\"s\"><typoKey>1</typoKey></style></styles>"
                        + "<container><Text size=\"1,1\" style=\"s\" text=\"a\"/></container>"));
        assertInvalid(schema, "未知标签",
                template("<container><Bogus size=\"1,1\"/></container>"));
    }

    /**
     * 正向对照：{@code Span} 支持 {@code style}（运行期确实生效），XSD 必须允许它。
     * <p>
     * 回归背景：XSD 的 {@code Span} 曾漏掉 {@code AttrContainerDefault}（id / style / if），
     * 导致合法的 {@code <Span style="...">} 被误判为非法。
     */
    @Test
    public void spanStyleShouldBeAccepted() throws Exception {
        List<String> errors = validate(newSchema(),
                parse(template("<container><Span size=\"2,1\" style=\"{borderTop:none}\"/></container>")));
        Assert.assertTrue("Span 的 style/id/if 应当被允许，实际报错: " + errors, errors.isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    private static Schema newSchema() throws SAXException {
        InputStream stream = TemplateSchemaTest.class.getResourceAsStream(XSD_RESOURCE);
        Assert.assertNotNull("XSD 资源缺失: " + XSD_RESOURCE, stream);
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return factory.newSchema(new StreamSource(stream));
        } finally {
            try {
                stream.close();
            } catch (Exception ignored) {
                // 关闭失败不影响校验结果
            }
        }
    }

    /** @param content {@code <template>} 的子节点，通常是一个 {@code <container>} 加可选的 styles / sheetStyle */
    private static String template(String content) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<template xmlns=\"" + NS + "\" name=\"SchemaTest\">"
                + content
                + "</template>";
    }

    private static Document parse(String xml) {
        return XmlUtil.readXML(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static void assertInvalid(Schema schema, String what, String xml) throws SAXException {
        List<String> errors = validate(schema, parse(xml));
        Assert.assertFalse(what + " 应当被 XSD 拒绝，但校验通过了（校验器可能没生效）", errors.isEmpty());
    }

    /** 返回校验错误清单，空表示通过。每次新建 {@link Validator}（其非线程安全且会保留状态）。 */
    private static List<String> validate(Schema schema, Document document) throws SAXException {
        final List<String> errors = new ArrayList<String>();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
                // 警告不视为失败
            }

            @Override
            public void error(SAXParseException exception) {
                errors.add("line " + exception.getLineNumber() + ": " + exception.getMessage());
            }

            @Override
            public void fatalError(SAXParseException exception) {
                errors.add("line " + exception.getLineNumber() + ": " + exception.getMessage());
            }
        });
        try {
            validator.validate(new DOMSource(document));
        } catch (Exception e) {
            errors.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return errors;
    }
}
