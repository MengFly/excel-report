package io.github.mengfly.excel.report.component;

import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleHolder;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public abstract class AbstractComponent extends StyleHolder implements Component {

    private ContainerTreeNode templateNode;
    private Size measuredSize;

    @Override
    public final void export(ReportContext context, Point point) {
        Component.super.export(context, point);
    }

    @Override
    public void onMeasure(Size suggestSize) {
        // 默认的测量大小等于推荐的大小
        measuredSize = suggestSize;
    }
}
