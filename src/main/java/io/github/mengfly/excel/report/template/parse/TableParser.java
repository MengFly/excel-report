package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.table.TableColumn;
import io.github.mengfly.excel.report.component.table.TableComponent;
import io.github.mengfly.excel.report.component.table.TableObjFieldColumn;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.util.BeanUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Slf4j
public class TableParser extends ContainerParser {
    private final String tagName = "Table";

    @Override
    protected List<String> getIgnoreProperties() {
        return Arrays.asList("dataList", "columns", "dataStyle");
    }

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {

        final TableComponent component = new TableComponent();

        component.setColumns(getColumns(containerTreeNode, context));
        component.setDataList(getDataList(containerTreeNode, context));

        return component;
    }

    private List<TableColumn> getColumns(ContainerTreeNode containerTreeNode, DataContext context) {
        List<TableColumn> columns = new ArrayList<>();

        for (ContainerTreeNode columnNode : containerTreeNode.listChild("column")) {
            TableColumn column = new TableObjFieldColumn();
            column.addStyle(columnNode.getStyle("style", context));
            column.getDataStyle().addStyle(columnNode.getStyle("dataStyle", context));
            initProperties(column, columnNode, context);
            columns.add(column);
        }

        return columns;
    }

    private List<?> getDataList(ContainerTreeNode containerTreeNode, DataContext context) {

        try {
            Object dataList = context.doExpression(containerTreeNode.getElement().getAttribute("dataList"));
            return BeanUtil.objectToList(dataList);
        } catch (Exception e) {
            log.error("无法解析数据", e);
            return Collections.emptyList();
        }

    }

}
