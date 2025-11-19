package io.github.mengfly.excel.report.component.table;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.layout.AbstractLayout;
import io.github.mengfly.excel.report.layout.GridLayout;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class TableComponentNew extends AbstractLayout {
    /**
     * 表格布局
     */
    private final GridLayout gridLayout;
    private final List<?> dataList;
    private final List<TableColumn> columns;

    private final int headerHeight;
    private final boolean headerVisible;

    public TableComponentNew(List<?> dataList,
                             List<TableColumn> columns) {
        this(dataList, columns, 1, true);
    }

    public TableComponentNew(List<?> dataList,
                             List<TableColumn> columns,
                             int headerHeight,
                             boolean headerVisible) {
        this.dataList = dataList;
        this.columns = columns;
        this.gridLayout = new GridLayout();
        this.gridLayout.setOrientation(Orientation.HORIZONTAL);
        this.gridLayout.setCount(columns.size());
        this.headerHeight = headerHeight;
        this.headerVisible = headerVisible;
        // 开始添加子组件
        if (headerVisible) {
            for (TableColumn column : columns) {
                gridLayout.addItem(
                        new TextComponent(
                                Size.of(column.getColumnSpan(), headerHeight),
                                column.getName()
                        )
                );
            }
        }
        for (Object d : dataList) {
            for (TableColumn column : columns) {
                gridLayout.addItem(
                        new TextComponent(
                                Size.of(column.getColumnSpan(), 1),
                                column.getValue(d)
                        )
                );
            }
        }
    }

    @Override
    public <T extends Container> T addItem(T item, Object constraint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onMeasure(Size suggestSize) {
        this.measuredSize = suggestSize;
        gridLayout.onMeasure(suggestSize);
    }

    @Override
    public void onLayout(Point relativePosition) {
        this.position = relativePosition;
        gridLayout.onLayout(relativePosition);
    }

    @Override
    public List<Container> getContainers() {
        return gridLayout.getContainers();
    }

    @Override
    public Size getSize() {
        return gridLayout.getSize();
    }


    @Override
    public void onExport(ReportContext context) {
        // TODO  2024/6/3 调整大小？
        if (columns == null) {
            return;
        }
        final List<Container> childContainers = gridLayout.getContainers();
        int offset = 0;
        if (headerVisible) {
            for (int i = 0; i < columns.size(); i++) {
                final TableColumn column = columns.get(i);
                final Container container = childContainers.get(i);
                context.getStyleChain().onStyle(column.getStyle(), () -> container.onExport(context));
            }
            offset = columns.size();
        }

        // 开始导出数据
        for (int i = offset; i < childContainers.size(); i++) {
            final Container container = childContainers.get(i);
            // 找到对应的Column
            final TableColumn column = columns.get(i % columns.size());
            context.getStyleChain()
                    .onStyle(column.getDataStyle().getStyle(), () -> container.onExport(context));
        }
    }
}
