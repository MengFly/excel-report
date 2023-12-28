package io.github.mengfly.excel.report;


import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleAble;

public interface Container extends StyleAble {


    Size getSize();

    default void export(ReportContext context, Point point) {
        context.getStyleChain().onStyle(getStyle(), () -> onExport(context, point));
    }

    void onExport(ReportContext context, Point point);


    default String print() {
        return String.format("%s[%s]", getClass().getSimpleName(), getSize());
    }
}
