package io.github.mengfly.excel.report.template.parse;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.exepression.process.ProcessControl;
import io.github.mengfly.excel.report.util.BeanUtil;
import io.github.mengfly.excel.report.util.XmlUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public abstract class ContainerParser {
    private static final List<String> IGNORE_PROPERTIES = Arrays.asList("style", "size", "for", "if");

    public final Container doParse(ContainerTreeNode containerTreeNode, DataContext context) {

        ProcessControl control = containerTreeNode.getProcessControl(context, "if");

        final Container container = control.fetchOne((processContext) -> parse(containerTreeNode, processContext));

        if (container != null) {
            initProperties(container, containerTreeNode, context);
            container.addStyle(containerTreeNode.getStyle("style", context));
        }
        return container;
    }


    public abstract String getTagName();

    /**
     * 忽略的属性
     * <p>
     * 这些属性在初始化Container属性的时候会被忽略
     *
     * @return 忽略的属性
     */
    protected List<String> getIgnoreProperties() {
        return Collections.emptyList();
    }


    protected abstract Container parse(ContainerTreeNode containerTreeNode, DataContext context);

    protected Size getSize(ContainerTreeNode containerTreeNode, DataContext context, Size defaultSize) {
        String size = containerTreeNode.getAttribute("size");

        if (StrUtil.isEmpty(size)) {
            return defaultSize;
        }
        size = context.doExpression(size, String.class);
        try {
            return ObjectUtil.defaultIfNull(Size.of(size), defaultSize);
        } catch (Exception e) {
            log.error("无法解析 {} 的 size: {}", getTagName(), size);
            return defaultSize;
        }
    }

    protected void initProperties(Object container, ContainerTreeNode element, DataContext context) {
        final ArrayList<String> ignoreProperties = new ArrayList<>(IGNORE_PROPERTIES);
        ignoreProperties.addAll(getIgnoreProperties());

        final Map<String, String> attributeMap = XmlUtil.getAttributeMap(element.getElement(), ignoreProperties.toArray(new String[0]));
        if (attributeMap.isEmpty()) {
            return;
        }

        attributeMap.replaceAll((k, v) -> context.doExpression(attributeMap.get(k), String.class));

        BeanUtil.initBeanProperties(container, attributeMap);

    }

}
