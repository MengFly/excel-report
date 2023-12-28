package io.github.mengfly.excel.report.component;

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

    private String title;
    private int titleSpan = 1;

    private List<?> dataList = new ArrayList<>();
    private int span = 1;

    private Orientation orientation = Orientation.VERTICAL;

    @Override
    public Size getSize() {

        int dataSize = dataList == null ? 0 : dataList.size();

        if (orientation == Orientation.VERTICAL) {
            return Size.of(span, title == null ? dataSize : dataSize + titleSpan);
        } else {
            return Size.of(title == null ? dataSize : dataSize + titleSpan, span);
        }
    }

    @Override
    public void onExport(ReportContext context, Point point) {
        if (orientation == Orientation.VERTICAL) {
            exportVerticalList(context, point);
        } else {
            exportHorizontalList(context, point);
        }
    }

    private void exportHorizontalList(ReportContext context, Point point) {
        int startCol = 0;
        // export header
        if (title != null) {
            final ExcelCellSpan titleCellSpan = context.getCellSpan(point, Size.of(this.titleSpan, span));
            titleCellSpan.merge().setValue(title);

            startCol += this.titleSpan;
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
        if (title != null) {
            final ExcelCellSpan headerCellSpan = context.getCellSpan(point, Size.of(span, titleSpan));
            headerCellSpan.merge().setValue(title);

            startRow += titleSpan;
        }

        // export data
        for (Object data : dataList) {
            final ExcelCellSpan dataCellSpan = context.getCellSpan(point.add(0, startRow), Size.of(span, 1));
            dataCellSpan.merge().setValue(data);

            startRow += 1;
        }
    }
}
