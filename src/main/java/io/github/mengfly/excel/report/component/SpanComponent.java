package io.github.mengfly.excel.report.component;

import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpanComponent extends AbstractComponent {

    private Size size = new Size(1, 1);


    @Override
    public void onExport(ReportContext context, Point point) {
        context.getCellSpan(point, size).merge();
    }
}
