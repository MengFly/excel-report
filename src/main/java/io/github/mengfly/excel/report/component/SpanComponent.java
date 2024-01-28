package io.github.mengfly.excel.report.component;

import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.CellStyles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.usermodel.BorderStyle;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpanComponent extends AbstractComponent {

    private Size size = new Size(1, 1);

    {
        addStyle(CellStyles.borderBottom, BorderStyle.NONE);
        addStyle(CellStyles.borderLeft, BorderStyle.NONE);
        addStyle(CellStyles.borderRight, BorderStyle.NONE);
        addStyle(CellStyles.borderTop, BorderStyle.NONE);
    }


    @Override
    public void onExport(ReportContext context, Point point) {
        context.getCellSpan(point, size).merge();
    }
}
