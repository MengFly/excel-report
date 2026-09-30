package io.github.mengfly.excel.report.template.validate;

import io.github.mengfly.excel.report.template.validate.ValidationIssue.Level;
import io.github.mengfly.excel.report.util.XmlUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link TemplateValidator} 语义规则（L2）的回归测试。
 * <p>
 * 每条规则都对应一个<b>已实测</b>的静默失效行为：写错不报错，只是被丢弃、回退或只渲染一次。
 * 这里用最小模板逐条固定下来，避免以后改动把"能发现问题"的能力弄丢。
 */
public class TemplateValidatorTest {

    private static final String NS = "http://mengfly.github.io/excel-report/1.0.0";

    /** 一个完全合法的模板，用于确认规则不会误报。 */
    @Test
    public void validTemplateShouldProduceNoIssue() {
        String xml = template(
                "<styles><style id=\"title\"><fontBold>true</fontBold><fontColor>#FF0000</fontColor></style></styles>"
                        + "<sheetStyle><defaultRowHeight>24</defaultRowHeight></sheetStyle>"
                        + "<container><VLayout>"
                        + "<Text size=\"-1,1\" style=\"title\" text=\"${title}\"/>"
                        + "<Table dataList=\"${rows}\">"
                        + "<column id=\"name\" name=\"名称\" style=\"{alignHorizontal:center}\"/>"
                        + "</Table>"
                        + "</VLayout></container>");
        Assert.assertEquals("合法模板不应产生任何问题", Collections.emptyList(), validate(xml));
    }

    /** {@code <container>} 下多于 1 个根组件：运行期只取第一个，其余静默丢弃。 */
    @Test
    public void multipleContainerRootsShouldBeReported() {
        String xml = template("<container>"
                + "<Text size=\"1,1\" text=\"a\"/>"
                + "<Text size=\"1,1\" text=\"b-SILENTLY-DROPPED\"/>"
                + "</container>");
        assertReported(xml, TemplateValidator.RULE_CONTAINER_CARDINALITY);
    }

    /** 引用未定义的样式 id：运行期静默拿到空样式。 */
    @Test
    public void undefinedStyleIdShouldBeReported() {
        String xml = template("<container><Text size=\"1,1\" style=\"noSuchId\" text=\"a\"/></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_UNDEFINED_ID);
    }

    /** {@code dataStyle} 同样会引用样式 id。 */
    @Test
    public void undefinedDataStyleIdShouldBeReported() {
        String xml = template("<container><Table dataList=\"${rows}\">"
                + "<column id=\"c\" name=\"C\" dataStyle=\"noSuchId\"/>"
                + "</Table></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_UNDEFINED_ID);
    }

    /** 内联 JSON 样式里的未知 key：运行期被静默丢弃（XSD 管不到属性值内部）。 */
    @Test
    public void unknownInlineStyleKeyShouldBeReported() {
        String xml = template("<container><Text size=\"1,1\" style=\"{typoKey:1}\" text=\"a\"/></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_UNKNOWN_KEY);
    }

    /** 内联 JSON 语法错误：整段样式被丢弃。 */
    @Test
    public void malformedInlineStyleShouldBeReported() {
        String xml = template("<container><Text size=\"1,1\" style=\"{alignHorizontal:center\" text=\"a\"/></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_MALFORMED_JSON);
    }

    /**
     * {@code <style>} 的取值是字面量，不走 SpEL —— 写 {@code ${}} 只会被静默丢弃。
     */
    @Test
    public void unparsableStyleValueShouldBeReported() {
        String xml = template("<styles><style id=\"s\"><fontHeight>${h}</fontHeight></style></styles>"
                + "<container><Text size=\"1,1\" style=\"s\" text=\"a\"/></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_UNPARSABLE_VALUE);
    }

    /** 未注册的 {@code <sheetStyle>} key（拼错一个字母）。 */
    @Test
    public void unknownSheetStyleKeyShouldBeReported() {
        String xml = template("<sheetStyle><defaultRowHeigt>24</defaultRowHeigt></sheetStyle>"
                + "<container><Text size=\"1,1\" text=\"a\"/></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_UNKNOWN_KEY);
    }

    /** 颜色名不认识 ⇒ 运行期静默回退成黑色（不是错误，但要提示）。 */
    @Test
    public void unrecognizedColorShouldBeWarned() {
        String xml = template("<styles><style id=\"s\"><fontColor>notacolor</fontColor></style></styles>"
                + "<container><Text size=\"1,1\" style=\"s\" text=\"a\"/></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_COLOR_FALLBACK);
    }

    /** 合法的颜色写法不应触发回退告警（含 hex 与 HSSFColor 名字）。 */
    @Test
    public void recognizableColorsShouldNotBeWarned() {
        for (String color : Arrays.asList("#FF0000", "white", "grey_50_percent", "AUTOMATIC", "black")) {
            String xml = template("<styles><style id=\"s\"><fontColor>" + color + "</fontColor></style></styles>"
                    + "<container><Text size=\"1,1\" style=\"s\" text=\"a\"/></container>");
            Assert.assertFalse("颜色 " + color + " 是合法写法，不应告警: " + validate(xml),
                    hasRule(validate(xml), TemplateValidator.RULE_STYLE_COLOR_FALLBACK));
        }
    }

    /** 重复的样式 id：后定义的一份被采用。 */
    @Test
    public void duplicateStyleIdShouldBeReported() {
        String xml = template("<styles>"
                + "<style id=\"same\"><fontBold>true</fontBold></style>"
                + "<style id=\"same\"><fontBold>false</fontBold></style>"
                + "</styles>"
                + "<container><Text size=\"1,1\" style=\"same\" text=\"a\"/></container>");
        assertReported(xml, TemplateValidator.RULE_STYLE_DUPLICATE_ID);
    }

    /** {@code for} 缺少 {@code 变量:} 绑定 ⇒ 只渲染一次。 */
    @Test
    public void forWithoutBindingShouldBeReported() {
        String xml = template("<container><VLayout for=\"${rows}\">"
                + "<Text size=\"1,1\" text=\"a\"/>"
                + "</VLayout></container>");
        assertReported(xml, TemplateValidator.RULE_FOR_MISSING_BINDING);
    }

    /** 正常写法的 {@code for} 不应告警。 */
    @Test
    public void forWithBindingShouldNotBeReported() {
        String xml = template("<container><VLayout for=\"row,index: ${rows}\">"
                + "<Text size=\"1,1\" text=\"${row.name}\"/>"
                + "</VLayout></container>");
        Assert.assertFalse("正常的 for 不应告警: " + validate(xml),
                hasRule(validate(xml), TemplateValidator.RULE_FOR_MISSING_BINDING));
    }

    /** 缺少 {@code <container>}：构造模板会直接崩，必须在解析期就能看出来。 */
    @Test
    public void missingContainerShouldBeReported() {
        String xml = template("<styles><style id=\"s\"><fontBold>true</fontBold></style></styles>");
        assertReported(xml, TemplateValidator.RULE_CONTAINER_MISSING);
    }

    /**
     * 没有声明命名空间时跳过 XSD 结构校验，但要明确告知 —— 这类模板当前是能跑的，
     * 不能当成错误直接拒掉。
     */
    @Test
    public void templateWithoutNamespaceShouldWarnButNotFail() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<template name=\"NoNs\"><container><Text size=\"1,1\" text=\"a\"/></container></template>";
        List<ValidationIssue> issues = validate(xml);
        Assert.assertTrue("应提示缺少命名空间: " + issues, hasRule(issues, TemplateValidator.RULE_NAMESPACE));
        for (ValidationIssue issue : issues) {
            Assert.assertNotEquals("无命名空间不应升级为 ERROR: " + issue, Level.ERROR, issue.getLevel());
        }
    }

    /** 反向对照：确认有命名空间时 XSD 这一层真的在跑（属性 / 标签 / key 三种写法）。 */
    @Test
    public void schemaLayerShouldCatchUnknownNames() {
        assertReported(template("<container><Text size=\"1,1\" text=\"a\" unknownAttr=\"1\"/></container>"),
                TemplateValidator.RULE_SCHEMA);
        assertReported(template("<container><Bogus size=\"1,1\"/></container>"),
                TemplateValidator.RULE_SCHEMA);
        assertReported(template("<styles><style id=\"s\"><typoKey>1</typoKey></style></styles>"
                        + "<container><Text size=\"1,1\" style=\"s\" text=\"a\"/></container>"),
                TemplateValidator.RULE_SCHEMA);
    }

    /** 仓库自带的模板必须零问题 —— 校验器上线不能把既有模板判成有问题。 */
    @Test
    public void shippedTemplatesShouldHaveNoIssue() {
        String[] templates = {
                "Example1Template.xml", "IndexSensitivityTemplate.xml", "LargeExportTemplate.xml",
                "TestGridLayout.xml", "TestImageTemplate.xml", "TestLayoutReport.xml", "TestTemplate.xml",
        };
        List<String> failures = new ArrayList<String>();
        for (String name : templates) {
            InputStream stream = TemplateValidatorTest.class.getResourceAsStream("/" + name);
            Assert.assertNotNull("测试模板不在 classpath 上: " + name, stream);
            List<ValidationIssue> issues;
            try {
                issues = TemplateValidator.validate(stream);
            } finally {
                close(stream);
            }
            if (!issues.isEmpty()) {
                failures.add(name + " -> " + issues);
            }
        }
        Assert.assertTrue("自带模板不应有校验问题: " + failures, failures.isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    private static String template(String content) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<template xmlns=\"" + NS + "\" name=\"ValidatorTest\">"
                + content
                + "</template>";
    }

    private static List<ValidationIssue> validate(String xml) {
        return TemplateValidator.validate(
                XmlUtil.readXML(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }

    private static boolean hasRule(List<ValidationIssue> issues, String ruleId) {
        for (ValidationIssue issue : issues) {
            if (ruleId.equals(issue.getRuleId())) {
                return true;
            }
        }
        return false;
    }

    private static void assertReported(String xml, String ruleId) {
        List<ValidationIssue> issues = validate(xml);
        Assert.assertTrue("应报出规则 " + ruleId + "，实际: " + issues, hasRule(issues, ruleId));
    }

    private static void close(InputStream stream) {
        try {
            stream.close();
        } catch (Exception ignored) {
            // 关闭失败不影响测试结果
        }
    }
}
