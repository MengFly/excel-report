package io.github.mengfly.excel.report.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.exception.ExcelReportException;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.SheetStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.style.key.StyleKey;
import io.github.mengfly.excel.report.template.validate.TemplateValidator;
import io.github.mengfly.excel.report.template.validate.ValidationIssue;
import io.github.mengfly.excel.report.util.XmlUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@Slf4j
public class ReportTemplate {

    private String name;
    private String version;
    private String author;
    private String description;
    private Date createAt;
    private final ContainerTreeNode rootNode;
    private StyleMap sheetStyle;

    /**
     * 解析期一次性算出的校验结果，宽松模式下不影响渲染。
     *
     * @see #validate()
     */
    private final List<ValidationIssue> issues;

    /**
     * 汇总日志只打印一次（同一个模板会被反复 render）。
     */
    private final AtomicBoolean issuesLogged = new AtomicBoolean(false);

    public ReportTemplate(InputStream stream) {
        // 先读成字节：DOM 不保留行号，结构校验需要原始字节才能报出准确行号
        final byte[] source = IoUtil.readBytes(stream);
        final Document document = XmlUtil.readXML(new ByteArrayInputStream(source));
        Element rootElement = XmlUtil.getRootElement(document);

        // 校验只依赖 DOM + 原始字节，先算出来：即使后续建树失败、或调用方从不 render，问题也不会丢
        issues = TemplateValidator.validate(document, source);

        initProperty(rootElement);
        initSheetStyle(rootElement);

        rootNode = buildTemplateNodeTree(XmlUtil.getElement(rootElement, "container"));

        initStyle(rootNode, rootElement);
    }

    /**
     * 模板校验结果（汇总 issue）。
     * <p>
     * 默认<b>宽松</b>：只返回问题、不抛异常，渲染照常进行。
     * 需要"发现错误就中断"时用 {@code TemplateManager#setStrict(boolean)}。
     *
     * @return 问题清单，空表示没发现问题
     */
    public List<ValidationIssue> validate() {
        return issues;
    }

    /**
     * @return 是否存在 ERROR 级问题
     */
    public boolean hasErrors() {
        for (ValidationIssue issue : issues) {
            if (issue.getLevel() == ValidationIssue.Level.ERROR) {
                return true;
            }
        }
        return false;
    }

    private void initSheetStyle(Element rootElement) {
        final Element style = XmlUtil.getElement(rootElement, "sheetStyle");
        if (style != null) {
            StyleMap styleMap = new StyleMap();
            final List<Element> styleKeyElements = XmlUtil.getElements(style, null);

            for (Element styleKeyElement : styleKeyElements) {
                final String tagName = styleKeyElement.getTagName();
                final StyleKey<Object> styleKey = SheetStyles.getStyleKey(tagName);
                if (styleKey != null) {
                    final String styleValue = styleKeyElement.getTextContent();
                    try {
                        styleMap.addStyle(styleKey, styleKey.getStyle(styleValue));
                    } catch (Exception e) {
                        log.error("无法解析 SheetStyle: {} ({})", tagName, styleValue);
                    }
                }
            }

            sheetStyle = styleMap;
        }
    }

    private void initStyle(ContainerTreeNode rootNode, Element rootElement) {
        // 解析Style
        final Element styles = XmlUtil.getElement(rootElement, "styles");

        if (styles != null) {
            for (Element styleElement : XmlUtil.getElements(styles, "style")) {
                final String id = styleElement.getAttribute("id");
                if (StrUtil.isEmpty(id)) {
                    continue;
                }
                final List<Element> styleKeyElements = XmlUtil.getElements(styleElement, null);
                final StyleMap style = CellStyles.createStyle(XmlUtil.getElementNameValueMap(styleKeyElements));
                rootNode.getStyleMap().put(id, style);
            }
        }
    }

    private void initProperty(Element rootElement) {
        final Map<String, String> attributeMap = XmlUtil.getAttributeMap(rootElement);
        BeanUtil.fillBeanWithMap(attributeMap, this, true);
    }


    public Container render(DataContext context) {
        logIssuesOnce();
        return rootNode.render(context);
    }

    /**
     * 宽松模式下的可见性出口：第一次渲染时把校验结果汇总成一条 WARN。
     * <p>
     * 只打一次；需要中断而不是提示时，用严格模式。
     */
    private void logIssuesOnce() {
        if (issues.isEmpty() || !issuesLogged.compareAndSet(false, true)) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (ValidationIssue issue : issues) {
            builder.append("\n  ").append(issue);
        }
        log.warn("模板 {} 有 {} 条校验问题（渲染继续；需要中断请用 TemplateManager#setStrict(true)）:{}",
                name, issues.size(), builder);
    }


    private ContainerTreeNode buildTemplateNodeTree(Element element) {
        if (element == null) {
            throw new ExcelReportException("<template> 缺少必填的 <container> 节点");
        }
        final List<Element> rootContainers = XmlUtil.getElements(element, null);
        if (rootContainers.isEmpty()) {
            throw new ExcelReportException("<container> 下必须恰好 1 个根组件，实际 0 个");
        }
        final Element rootContainer = rootContainers.get(0);
        ContainerTreeNode root = new ContainerTreeNode();
        root.setElement(rootContainer);
        return root;
    }

}
