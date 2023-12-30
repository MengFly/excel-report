package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.util.BeanUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.ListComponent;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

import java.util.Collections;
import java.util.List;

@Getter
@Slf4j
public class ListParser extends ContainerParser {
    private final String tagName = "List";

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {

        final ListComponent component = new ListComponent();

        component.setDataList(getDataList(containerTreeNode, context));
        return component;
    }

    @Override
    protected List<String> getIgnoreProperties() {
        return Collections.singletonList("dataList");
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
