package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.list.ListComponent;
import io.github.mengfly.excel.report.component.list.ListHeader;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.util.BeanUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Slf4j
public class ListParser extends ContainerParser {
    private final String tagName = "List";

    @Override
    public Container parse(ContainerTreeNode node, DataContext context) {

        final ListComponent component = new ListComponent();

        final ContainerTreeNode headerNode = node.getChild("header");
        if (headerNode != null) {
            ListHeader header = new ListHeader();
            headerNode.initProperties(header, context, getIgnoreProperties().toArray(new String[0]));
            header.addStyle(headerNode.getStyle("style", context));
            component.setHeader(header);
        }

        component.setDataList(getDataList(node, context));
        return component;
    }

    @Override
    protected List<String> getIgnoreProperties() {
        // style 必须在这里忽略：<header> 的样式已在 parse 里显式 addStyle 应用，
        // 而 ListHeader 继承 StyleHolder，其 style 是 final 的只读属性（StyleMap），
        // 让 BeanUtil 去写它只会抛 ConvertException（被 catch 吞掉）。其它 Parser 都靠
        // ContainerParser.IGNORE_PROPERTIES 忽略了 style，这里原本漏了。
        return Arrays.asList("dataList", "style");
    }

    private List<?> getDataList(ContainerTreeNode element, DataContext context) {

        try {
            Object dataList = context.doExpression(element.getElement().getAttribute("dataList"));
            return BeanUtil.objectToList(dataList);
        } catch (Exception e) {
            log.error("无法解析数据", e);
            return Collections.emptyList();
        }

    }

}
