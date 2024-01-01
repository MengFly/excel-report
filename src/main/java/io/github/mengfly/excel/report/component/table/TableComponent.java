package io.github.mengfly.excel.report.component.table;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class TableComponent extends AbstractComponent {
    private List<?> dataList;
    private List<TableColumn> columns;
    private int headerHeight = 1;
    private boolean headerVisible = true;

    public TableComponent(List<?> dataList, List<TableColumn> columns) {
        this.dataList = dataList;
        this.columns = columns;
    }

    @Override
    public Size getSize() {
        int width = 0;
        int height = 0;
        if (headerVisible) {
            height += headerHeight;
        }

        if (columns != null) {
            for (TableColumn column : columns) {
                width += column.getColumnSpan();
            }
        }
        height += this.dataList == null ? 0 : this.dataList.size();
        return new Size(width, height);
    }

    @Override
    public void onExport(ReportContext context, Point point) {
        if (columns == null) {
            return;
        }
        int curColumn = 0;
        for (TableColumn column : columns) {
            StyleMap style = context.getCurrentChildStyle(column.getStyle());

            // draw header
            if (headerVisible) {
                final ExcelCellSpan headerCellSpan = context.getCellSpan(
                        point.add(curColumn, 0),
                        Size.of(column.getColumnSpan(), getHeaderHeight()),
                        style
                );
                headerCellSpan.merge().setValue(column.getName());
            }
            if (dataList == null) {
                continue;
            }
            // draw data
            int curRow = 0;
            if (headerVisible) {
                curRow += getHeaderHeight();
            }
            for (Object d : dataList) {
                final ExcelCellSpan dataCellSpan = context.getCellSpan(
                        point.add(curColumn, curRow),
                        Size.of(column.getColumnSpan(), 1),
                        context.getCurrentChildStyle(column.getDataStyle().getStyle())
                );
                curRow += 1;
                dataCellSpan.merge().setValue(column.getValue(d));
            }

            curColumn += column.getColumnSpan();
        }
    }
}
