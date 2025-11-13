package io.github.mengfly.excel.report.component.list;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ListComponent extends AbstractComponent {


    private ListHeader header;

    private List<?> dataList = new ArrayList<>();
    private int span = 1;

    private Orientation orientation = Orientation.VERTICAL;

    @Override
    public Size getSize() {

        int dataSize = dataList == null ? 0 : dataList.size();

        if (orientation == Orientation.VERTICAL) {
            return Size.of(span, header == null ? dataSize : dataSize + header.getSpan());
        } else {
            return Size.of(header == null ? dataSize : dataSize + header.getSpan(), span);
        }
    }

    @Override
    public void onExport(ReportContext context, Point point) {
        // TODO  2024/6/3 调整大小？
        if (orientation == Orientation.VERTICAL) {
            exportVerticalList(context, point);
        } else {
            exportHorizontalList(context, point);
        }
    }

    private void exportHorizontalList(ReportContext context, Point point) {
        int startCol = 0;
        // export header
        if (header != null) {

            final ExcelCellSpan titleCellSpan = context.getCellSpan(
                    point, Size.of(this.getHeader().getSpan(), span), context.getCurrentChildStyle(this.header.getStyle()));
            titleCellSpan.merge().setValue(getHeader().getTitle());

            startCol += this.header.getSpan();
        }

        // export data
        for (Object data : dataList) {
            final ExcelCellSpan dataCellSpan = context.getCellSpan(
                    point.add(startCol, 0),
                    Size.of(1, span)
            );
            dataCellSpan.merge().setValue(data);

            startCol += 1;
        }
    }

    private void exportVerticalList(ReportContext context, Point point) {
        int startRow = 0;

        // export header
        if (header != null) {
            final ExcelCellSpan headerCellSpan = context.getCellSpan(point, Size.of(span, header.getSpan()));
            headerCellSpan.merge().setValue(header.getTitle());

            startRow += header.getSpan();
        }

        // export data
        for (Object data : dataList) {
            final ExcelCellSpan dataCellSpan = context.getCellSpan(point.add(0, startRow), Size.of(span, 1));
            dataCellSpan.merge().setValue(data);

            startRow += 1;
        }
    }
}
