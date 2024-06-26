package io.github.mengfly.excel.report.template;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.style.key.StyleKey;
import io.github.mengfly.excel.report.template.exepression.process.ProcessControl;
import io.github.mengfly.excel.report.template.parse.ParserFactory;
import io.github.mengfly.excel.report.util.BeanUtil;
import io.github.mengfly.excel.report.util.StyleUtil;
import io.github.mengfly.excel.report.util.XmlUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Data
@Slf4j
public class ContainerTreeNode {

    /**
     * 父节点
     */
    private ContainerTreeNode parent;
    private Element element;
    private Map<String, StyleMap> syleMap = new HashMap<>();
    private TemplateManager templateManager;

    public String getTagName() {
        return element.getTagName();
    }

    /**
     * 渲染组件
     *
     * @param context 组件数据
     * @return 组件
     */
    public Container render(DataContext context) {
        final Container container = ParserFactory.doParseElement(this, context);
        if (container != null) {
            container.setTemplateNode(this);
        }
        return container;
    }

    public String getAttribute(String key) {
        return element.getAttribute(key);
    }

    public StyleMap getStyle(String styleAttr, DataContext context) {

        String attribute = getAttribute(styleAttr);
        if (StrUtil.isEmpty(attribute)) {
            return new StyleMap();
        }
        StyleMap map = new StyleMap();
        final List<String> styleSplit = StyleUtil.analysisStyle(attribute);

        for (String style : styleSplit) {
            // 判断是否是Style引用
            if (style.startsWith("{")) {
                map.addStyle(parseJsonStyle(style, context));
            } else {
                map.addStyle(getStyleById(style));
            }
        }
        return map;
    }

    private StyleMap getStyleById(String attribute) {
        ContainerTreeNode node = this;
        while (node != null) {
            if (node.syleMap.containsKey(attribute)) {
                return node.syleMap.get(attribute);
            } else {
                node = node.parent;
            }
        }
        return new StyleMap();
    }


    public List<ContainerTreeNode> listChild(String tagName) {
        final List<Element> elements = XmlUtil.getElements(element, tagName);
        return elements.stream().map(childElement -> {
            final ContainerTreeNode treeNode = new ContainerTreeNode();
            treeNode.setParent(this);
            treeNode.setElement(childElement);
            treeNode.setTemplateManager(templateManager);
            return treeNode;
        }).collect(Collectors.toList());
    }

    public ContainerTreeNode getChild(String tagName) {
        final Element childElement = XmlUtil.getElement(element, tagName);
        if (childElement == null) {
            return null;
        }
        final ContainerTreeNode treeNode = new ContainerTreeNode();
        treeNode.setParent(this);
        treeNode.setElement(childElement);
        return treeNode;
    }

    public <T> T getChild(DataContext context, String tagName, Supplier<T> supplier) {
        final ContainerTreeNode child = getChild(tagName);
        if (child != null) {
            final T t = supplier.get();
            child.initProperties(t, context);
            return t;
        }
        return null;
    }

    private StyleMap parseJsonStyle(String attribute, DataContext context) {
        StyleMap styleMap = new StyleMap();
        try {
            JSONObject styleJson = JSONUtil.parseObj(attribute);
            for (String key : styleJson.keySet()) {
                final StyleKey<Object> styleKey = CellStyles.getStyleKey(key);
                if (styleKey != null) {
                    try {
                        styleMap.addStyle(styleKey, styleKey.getStyle(context.doExpression(styleJson.getStr(key), String.class)));
                    } catch (Exception e) {
                        log.error("无法解析 {} 的 Style: {} ({})", getTagName(), key, styleJson.getStr(key));
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析 {} Style 失败", getTagName(), e);
        }
        return styleMap;
    }

    public ProcessControl getProcessControl(DataContext context, String tagName) {
        return ProcessControl.create(tagName, getElement().getAttribute(tagName), context);
    }

    public Map<String, String> getAttributeMap(String... ignoreProperties) {
        return XmlUtil.getAttributeMap(getElement(), ignoreProperties);
    }

    public void initProperties(Object result, DataContext context, String... ignoreProperties) {
        final Map<String, String> attributeMap = getAttributeMap(ignoreProperties);

        if (attributeMap.isEmpty()) {
            return;
        }
        Map<String, Object> propertiesMap = new HashMap<>();
        attributeMap.forEach((k, v) -> propertiesMap.put(k, context.doExpression(attributeMap.get(k), Object.class)));
        BeanUtil.initBeanProperties(result, propertiesMap);
    }
}
