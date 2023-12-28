package io.github.mengfly.excel.report.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextComponent extends AbstractComponent {

    private Size size;
    private String text;

    @Override
    public void onExport(ReportContext context, Point point) {

        ExcelCellSpan cellSpan = context.getCellSpan(point, size);
        cellSpan.merge().setValue(text);

    }
}
