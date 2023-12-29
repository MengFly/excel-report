package io.github.mengfly.excel.report.component;

import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleHolder;
import lombok.Getter;


@Getter
public abstract class AbstractComponent extends StyleHolder implements Component {

    @Override
    public final void export(ReportContext context, Point point) {
        Component.super.export(context, point);
    }
}
