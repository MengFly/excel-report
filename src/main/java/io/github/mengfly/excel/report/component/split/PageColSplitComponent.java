package io.github.mengfly.excel.report.component.split;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;

public class PageColSplitComponent extends AbstractComponent {
    @Override
    public Size getSize() {
        return Size.of(0, 0);
    }

    @Override
    public void onExport(ReportContext context, Point point, Size suggestSize) {
        context.getSheet().setColumnBreak(point.getX() - 1);
    }
}
