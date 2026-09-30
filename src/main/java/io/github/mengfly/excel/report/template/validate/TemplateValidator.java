package io.github.mengfly.excel.report.template.validate;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.SheetStyles;
import io.github.mengfly.excel.report.style.key.ColorStyleKey;
import io.github.mengfly.excel.report.style.key.StyleKey;
import io.github.mengfly.excel.report.template.validate.ValidationIssue.Level;
import io.github.mengfly.excel.report.util.StyleUtil;
import io.github.mengfly.excel.report.util.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.util.HSSFColor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 模板校验器：把"能跑但静默失效"的模板写法找出来。
 * <p>
 * 分两层，两层互补、互不重复：
 * <ul>
 *     <li><b>L1 结构层</b>：按 {@code schema/excel-report-1.0.0.xsd} 校验标签 / 属性 / 样式 key / 枚举取值。
 *         这一层覆盖"名字写错"，由 schema 定义，本类不复制任何白名单。</li>
 *     <li><b>L2 语义层</b>：XSD 结构上表达不了、只能靠交叉引用或运行期语义发现的问题——
 *         样式 id 是否已定义、{@code <container>} 是否恰好 1 个根组件、内联 JSON 样式的 key 与语法、
 *         样式值能否转换、{@code for} 是否带变量绑定。这一层只向既有注册表
 *         （{@link CellStyles}/{@link SheetStyles}）查存在性，同样不复制白名单。</li>
 * </ul>
 * <b>本类不抛异常</b>：XSD 加载失败、模板没有命名空间、模板本身很破，都只体现为 issue 条目。
 * 是否中断由调用方决定（见 {@code TemplateManager#setStrict(boolean)}）。
 *
 * @see ValidationIssue
 */
@Slf4j
public final class TemplateValidator {

    /*
     * 每条规则对应的静默失效行为、实测证据与取舍过程，见 doc/优化建议.md 二.2。
     */

    /**
     * 模板 XSD 的 targetNamespace，也是模板头部 {@code xmlns} 应有的值。
     */
    public static final String NAMESPACE = "http://mengfly.github.io/excel-report/1.0.0";

    private static final String XSD_RESOURCE = "/schema/excel-report-1.0.0.xsd";

    /** 单个模板最多返回多少条 XSD 错误，避免严重写错的模板产生海量条目。 */
    private static final int MAX_SCHEMA_ISSUES = 50;

    /** 单条 XSD 消息的最大长度（XSD 的候选列表会非常长）。 */
    private static final int MAX_SCHEMA_MESSAGE = 300;

    /**
     * 会被当作"样式 id 引用 / 内联样式"解析的属性。
     * 与运行期一致：{@code ContainerParser} 读 {@code style}，{@code TableParser} 额外读 {@code dataStyle}。
     */
    private static final String[] STYLE_ATTRIBUTES = {"style", "dataStyle"};

    public static final String RULE_SCHEMA = "schema.xsd";
    public static final String RULE_SCHEMA_UNAVAILABLE = "schema.unavailable";
    public static final String RULE_NAMESPACE = "template.namespace";
    public static final String RULE_CONTAINER_MISSING = "template.container-missing";
    public static final String RULE_CONTAINER_CARDINALITY = "template.container-cardinality";
    public static final String RULE_STYLE_UNDEFINED_ID = "style.undefined-id";
    public static final String RULE_STYLE_DUPLICATE_ID = "style.duplicate-id";
    public static final String RULE_STYLE_UNKNOWN_KEY = "style.unknown-key";
    public static final String RULE_STYLE_UNPARSABLE_VALUE = "style.unparsable-value";
    public static final String RULE_STYLE_MALFORMED_JSON = "style.malformed-json";
    public static final String RULE_STYLE_COLOR_FALLBACK = "style.color-fallback";
    public static final String RULE_FOR_MISSING_BINDING = "expression.for-missing-binding";

    private static volatile boolean schemaLoadAttempted;
    private static volatile Schema schema;
    private static volatile String schemaLoadError;

    private TemplateValidator() {
    }

    /**
     * 校验一个模板文件（预检 / 离线工具用，不构造 {@code ReportTemplate}）。
     * <p>
     * XML 本身无法解析时不会返回 issue，而是抛出运行期异常（与 {@code ReportTemplate} 一致）。
     * <p>
     * 走这条入口时 XSD 那一层直接校验原始字节，因此问题里带<b>真实行号</b>；
     * 只传 DOM（下面的重载）时行号为 -1。
     *
     * @param stream 模板输入流
     * @return 问题清单，空表示没发现问题
     */
    public static List<ValidationIssue> validate(InputStream stream) {
        byte[] source = IoUtil.readBytes(stream);
        return validate(XmlUtil.readXML(new ByteArrayInputStream(source)), source);
    }

    /**
     * 校验一份已解析的模板 DOM。
     * <p>
     * DOM 由 {@link javax.xml.parsers.DocumentBuilder} 建出，不保留行号，所以 XSD 问题里的行号是 -1。
     * 想要行号请用 {@link #validate(InputStream)} 或 {@link #validate(Document, byte[])}。
     *
     * @param document 模板文档（需为命名空间感知解析，{@link XmlUtil#readXML(InputStream)} 即如此）
     * @return 问题清单，空表示没发现问题
     */
    public static List<ValidationIssue> validate(Document document) {
        return validate(document, null);
    }

    /**
     * 校验模板：DOM 用于语义层，原始字节用于结构层（保留行号）。
     *
     * @param document 模板文档，为 null 时只做不了任何校验
     * @param xmlSource 模板原始字节；为 null 时结构层退化为校验 DOM（行号为 -1）
     * @return 问题清单，空表示没发现问题
     */
    public static List<ValidationIssue> validate(Document document, byte[] xmlSource) {
        List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        if (document == null) {
            issues.add(new ValidationIssue(Level.ERROR, RULE_SCHEMA, "/", "模板文档为空"));
            return issues;
        }
        Element root = document.getDocumentElement();
        if (root == null) {
            issues.add(new ValidationIssue(Level.ERROR, RULE_SCHEMA, "/", "模板没有根节点"));
            return issues;
        }

        validateAgainstSchema(document, xmlSource, root, issues);

        Map<String, String> styleIds = collectStyleIds(root, issues);
        checkContainers(root, "/" + nodeName(root), issues);
        walk(root, "/" + nodeName(root), styleIds.keySet(), issues);
        return issues;
    }

    // ------------------------------------------------------------------ L1 结构层

    private static void validateAgainstSchema(Document document, byte[] xmlSource, Element root,
                                              List<ValidationIssue> issues) {
        if (!NAMESPACE.equals(root.getNamespaceURI())) {
            issues.add(new ValidationIssue(Level.WARN, RULE_NAMESPACE, "/" + nodeName(root),
                    "模板未声明默认命名空间（或声明的不是本版本），已跳过 XSD 结构校验："
                            + "标签 / 属性 / 样式 key 的拼写错误这一层发现不了。"
                            + "请把根节点 xmlns 设为 " + NAMESPACE
                            + "，当前为 " + (root.getNamespaceURI() == null ? "未声明" : root.getNamespaceURI())));
            return;
        }
        Schema schema = schema();
        if (schema == null) {
            issues.add(new ValidationIssue(Level.WARN, RULE_SCHEMA_UNAVAILABLE, "/",
                    "内置 XSD 无法加载，已跳过结构校验（这是框架自身的缺陷，请反馈）：" + schemaLoadError));
            return;
        }

        final List<ValidationIssue> schemaIssues = new ArrayList<ValidationIssue>();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) {
                // XSD 的 warning 不作为问题返回
            }

            @Override
            public void error(SAXParseException exception) {
                schemaIssues.add(schemaIssue(exception));
            }

            @Override
            public void fatalError(SAXParseException exception) {
                schemaIssues.add(schemaIssue(exception));
            }
        });
        try {
            // 有原始字节时直接校验字节：XSD 的错误才会带上真实行号（DOM 已丢失行号信息）
            validator.validate(xmlSource == null
                    ? new DOMSource(document)
                    : new StreamSource(new ByteArrayInputStream(xmlSource)));
        } catch (Exception e) {
            issues.add(new ValidationIssue(Level.ERROR, RULE_SCHEMA, "/", "XSD 校验未能完成: " + e));
            return;
        }

        if (schemaIssues.size() > MAX_SCHEMA_ISSUES) {
            issues.addAll(schemaIssues.subList(0, MAX_SCHEMA_ISSUES));
            issues.add(new ValidationIssue(Level.ERROR, RULE_SCHEMA, "/",
                    "XSD 还报了 " + (schemaIssues.size() - MAX_SCHEMA_ISSUES) + " 条错误，已省略"));
        } else {
            issues.addAll(schemaIssues);
        }
    }

    private static ValidationIssue schemaIssue(SAXParseException exception) {
        String message = StrUtil.toString(exception.getMessage()).replace('\n', ' ').trim();
        if (message.length() > MAX_SCHEMA_MESSAGE) {
            // XSD 的"应以 {…几十个名字…} 之一开头"会非常长，截断以免日志不可读
            message = message.substring(0, MAX_SCHEMA_MESSAGE) + "…";
        }
        return new ValidationIssue(Level.ERROR, RULE_SCHEMA, "line " + exception.getLineNumber(), message);
    }

    /**
     * 惰性加载并缓存 XSD。{@link Schema} 是线程安全的（{@link Validator} 不是，每次新建）；
     * 加载失败也只记录一次，不让模板因框架自身的 schema 问题而不可用。
     */
    private static Schema schema() {
        if (!schemaLoadAttempted) {
            synchronized (TemplateValidator.class) {
                if (!schemaLoadAttempted) {
                    try {
                        schema = loadSchema();
                    } catch (Exception e) {
                        schemaLoadError = e.getClass().getSimpleName() + ": " + e.getMessage();
                        log.warn("加载模板 XSD 失败，模板结构校验不可用", e);
                    } finally {
                        schemaLoadAttempted = true;
                    }
                }
            }
        }
        return schema;
    }

    private static Schema loadSchema() throws Exception {
        try (InputStream stream = TemplateValidator.class.getResourceAsStream(XSD_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("classpath 上找不到 " + XSD_RESOURCE);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return factory.newSchema(new StreamSource(stream));
        }
    }

    // ------------------------------------------------------------------ L2 语义层

    /** 收集 {@code <style id>}，同时报告重复 id（XSD 的 xs:ID 也管，但无命名空间时这一层仍能发现）。 */
    private static Map<String, String> collectStyleIds(Element element, List<ValidationIssue> issues) {
        Map<String, String> styleIds = new HashMap<String, String>();
        collectStyleIds(element, "/" + nodeName(element), styleIds, issues);
        return styleIds;
    }

    private static void collectStyleIds(Element element, String path, Map<String, String> styleIds,
                                        List<ValidationIssue> issues) {
        if ("style".equals(nodeName(element)) && element.hasAttribute("id")) {
            String id = element.getAttribute("id");
            if (styleIds.containsKey(id)) {
                issues.add(new ValidationIssue(Level.ERROR, RULE_STYLE_DUPLICATE_ID, path,
                        "样式 id 重复定义（前一次在 " + styleIds.get(id) + "），后定义的一份会被采用: " + id));
            }
            styleIds.put(id, path);
        }
        for (Element child : childElements(element)) {
            collectStyleIds(child, path + "/" + nodeName(child), styleIds, issues);
        }
    }

    private static void checkContainers(Element element, String path, List<ValidationIssue> issues) {
        if ("template".equals(nodeName(element)) && firstChild(element, "container") == null) {
            issues.add(new ValidationIssue(Level.ERROR, RULE_CONTAINER_MISSING, path,
                    "模板缺少必填的 <container> 节点"));
        }
        if ("container".equals(nodeName(element))) {
            int count = childElements(element).size();
            if (count != 1) {
                issues.add(new ValidationIssue(Level.ERROR, RULE_CONTAINER_CARDINALITY, path,
                        "<container> 下必须恰好 1 个根组件，实际 " + count + " 个："
                                + (count == 0 ? "没有组件可用" : "运行期只取第一个，其余被静默丢弃")));
            }
        }
        for (Element child : childElements(element)) {
            checkContainers(child, path + "/" + nodeName(child), issues);
        }
    }

    private static void walk(Element element, String path, Set<String> styleIds, List<ValidationIssue> issues) {
        String name = nodeName(element);
        if ("style".equals(name) || "sheetStyle".equals(name)) {
            checkStyleKeys(element, path, "style".equals(name), issues);
            return;
        }

        checkStyleAttributes(element, path, styleIds, issues);
        checkForAttribute(element, path, issues);

        for (Element child : childElements(element)) {
            walk(child, childPath(element, path, child), styleIds, issues);
        }
    }

    /**
     * {@code style} / {@code dataStyle} 属性：样式 id 引用 + 内联 JSON。
     * <p>
     * 内联 JSON 只校验语法与 key 是否注册，<b>不校验取值</b>：那一层的值会先过 SpEL 求值，
     * 静态判断容易误报。
     */
    private static void checkStyleAttributes(Element element, String path, Set<String> styleIds,
                                             List<ValidationIssue> issues) {
        for (String attribute : STYLE_ATTRIBUTES) {
            if (!element.hasAttribute(attribute)) {
                continue;
            }
            String raw = element.getAttribute(attribute);
            if (StrUtil.isBlank(raw)) {
                continue;
            }
            String where = path + "@" + attribute;
            for (String token : StyleUtil.analysisStyle(raw)) {
                if (token.startsWith("{")) {
                    checkInlineStyle(where, token, issues);
                } else if (!styleIds.contains(token)) {
                    issues.add(new ValidationIssue(Level.ERROR, RULE_STYLE_UNDEFINED_ID, where,
                            "引用了未定义的样式 id（运行期会静默拿到空样式）: " + token));
                }
            }
        }
    }

    private static void checkInlineStyle(String where, String token, List<ValidationIssue> issues) {
        JSONObject object;
        try {
            object = JSONUtil.parseObj(token);
        } catch (Exception e) {
            issues.add(new ValidationIssue(Level.ERROR, RULE_STYLE_MALFORMED_JSON, where,
                    "内联样式不是合法 JSON，整段样式会被丢弃: " + token));
            return;
        }
        for (String key : object.keySet()) {
            if (CellStyles.getStyleKey(key) == null) {
                issues.add(new ValidationIssue(Level.ERROR, RULE_STYLE_UNKNOWN_KEY, where,
                        "内联样式的 key 未注册（运行期会被静默丢弃）: " + key));
            }
        }
    }

    /**
     * {@code <style>} / {@code <sheetStyle>} 的子元素：key 是否注册、取值能否落到类型上。
     * <p>
     * 这一层的取值是<b>字面量</b>（{@code ReportTemplate} 直接取 textContent，
     * 不走 SpEL）——所以 {@code <fontHeight>${h}</fontHeight>} 会在这里被抓出来，
     * 而它运行期确实只是被静默丢弃。
     */
    private static void checkStyleKeys(Element element, String path, boolean cellStyle, List<ValidationIssue> issues) {
        for (Element child : childElements(element)) {
            String keyName = nodeName(child);
            String where = path + "/" + keyName;
            StyleKey<?> key = cellStyle ? CellStyles.getStyleKey(keyName) : SheetStyles.getStyleKey(keyName);
            if (key == null) {
                issues.add(new ValidationIssue(Level.ERROR, RULE_STYLE_UNKNOWN_KEY, where,
                        (cellStyle ? "单元格" : "Sheet") + "样式 key 未注册（运行期会被静默丢弃）: " + keyName));
                continue;
            }
            String value = StrUtil.toString(child.getTextContent()).trim();
            if (key instanceof ColorStyleKey) {
                checkColor(where, keyName, value, issues);
                continue;
            }
            try {
                if (key.getStyle(value) == null) {
                    issues.add(new ValidationIssue(Level.ERROR, RULE_STYLE_UNPARSABLE_VALUE, where,
                            "样式值无法转换，运行期会被静默丢弃: " + keyName + " = " + value));
                }
            } catch (Exception e) {
                issues.add(new ValidationIssue(Level.ERROR, RULE_STYLE_UNPARSABLE_VALUE, where,
                        "样式值无法转换，运行期会被静默丢弃: " + keyName + " = " + value
                                + "（" + e.getClass().getSimpleName() + ": " + e.getMessage() + "）"));
            }
        }
    }

    /**
     * 颜色单独判定：{@link ColorStyleKey} 对认不出的颜色<b>不抛异常</b>，只静默回退成黑色。
     * <p>
     * 这里用与 {@code ColorStyleKey} 相同的两个来源（{@link HSSFColor.HSSFColorPredefined}、
     * {@link HexUtil}）复判一次，没有另建颜色名单。
     */
    private static void checkColor(String where, String keyName, String value, List<ValidationIssue> issues) {
        boolean recognized;
        try {
            if (value.startsWith("#")) {
                HexUtil.decodeHex(value.substring(1));
            } else {
                EnumUtil.fromString(HSSFColor.HSSFColorPredefined.class, value.toUpperCase());
            }
            recognized = true;
        } catch (Exception e) {
            recognized = false;
        }
        if (!recognized) {
            issues.add(new ValidationIssue(Level.WARN, RULE_STYLE_COLOR_FALLBACK, where,
                    "颜色无法识别（既不是 #RRGGBB 也不是 HSSFColor 颜色名），运行期会静默回退成黑色: " + value));
        }
    }

    /**
     * {@code for} 表达式必须带 {@code 变量:} 绑定，否则运行期只渲染一次。
     * 与 {@code ForExpression} 一致：冒号位置必须 &gt; 0，否则参数为空、退化成"渲染一次"。
     */
    private static void checkForAttribute(Element element, String path, List<ValidationIssue> issues) {
        if (!element.hasAttribute("for")) {
            return;
        }
        String expression = element.getAttribute("for");
        if (StrUtil.isEmpty(expression) || expression.indexOf(':') > 0) {
            return;
        }
        issues.add(new ValidationIssue(Level.ERROR, RULE_FOR_MISSING_BINDING, path + "@for",
                "for 缺少 `变量:` 绑定（形如 for=\"item,index: ${dataList}\"），运行期只会渲染一次: " + expression));
    }

    // ------------------------------------------------------------------ DOM 工具

    /** 与运行期一致地取节点名（{@code ParserFactory}/{@code XmlUtil} 用的都是 getTagName）。 */
    private static String nodeName(Node node) {
        return node.getNodeName();
    }

    private static List<Element> childElements(Element element) {
        List<Element> children = new ArrayList<Element>();
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) node);
            }
        }
        return children;
    }

    private static Element firstChild(Element element, String name) {
        for (Element child : childElements(element)) {
            if (name.equals(nodeName(child))) {
                return child;
            }
        }
        return null;
    }

    /** 同名兄弟多于一个时补 {@code [i]}，让路径定位到具体节点。 */
    private static String childPath(Element parent, String parentPath, Element child) {
        String name = nodeName(child);
        int total = 0;
        int index = 0;
        for (Element sibling : childElements(parent)) {
            if (name.equals(nodeName(sibling))) {
                total++;
                if (sibling == child) {
                    index = total;
                }
            }
        }
        return parentPath + "/" + name + (total > 1 ? "[" + index + "]" : "");
    }
}
