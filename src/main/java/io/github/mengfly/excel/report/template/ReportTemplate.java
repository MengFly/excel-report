package io.github.mengfly.excel.report.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.SheetStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.style.key.StyleKey;
import io.github.mengfly.excel.report.util.XmlUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    private TemplateManager templateManager;


    public ReportTemplate(InputStream stream) {
        final Document document = XmlUtil.readXML(stream);
        Element rootElement = XmlUtil.getRootElement(document);

        initProperty(rootElement);
        initSheetStyle(rootElement);

        rootNode = buildTemplateNodeTree(XmlUtil.getElement(rootElement, "container"));

        initStyle(rootNode, rootElement);
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
                StyleMap styleMap = new StyleMap();
                final List<Element> styleKeyElements = XmlUtil.getElements(styleElement, null);

                for (Element styleKeyElement : styleKeyElements) {
                    final String tagName = styleKeyElement.getTagName();
                    final StyleKey<Object> styleKey = CellStyles.getStyleKey(tagName);
                    if (styleKey != null) {
                        final String styleValue = styleKeyElement.getTextContent();
                        try {
                            styleMap.addStyle(styleKey, styleKey.getStyle(styleValue));
                        } catch (Exception e) {
                            log.error("无法解析 Id 为 {} 的 Style: {} ({})", id, tagName, styleValue);
                        }
                    }
                }
                rootNode.getSyleMap().put(id, styleMap);
            }
        }
    }

    private void initProperty(Element rootElement) {
        final Map<String, String> attributeMap = XmlUtil.getAttributeMap(rootElement);
        BeanUtil.fillBeanWithMap(attributeMap, this, true);
    }


    public Container render(DataContext context) {
        rootNode.setTemplateManager(templateManager);
        return rootNode.render(context);
    }


    private ContainerTreeNode buildTemplateNodeTree(Element element) {
        final Element rootContainer = XmlUtil.getElements(element, null).get(0);
        ContainerTreeNode root = new ContainerTreeNode();
        root.setElement(rootContainer);
        return root;
    }

}
