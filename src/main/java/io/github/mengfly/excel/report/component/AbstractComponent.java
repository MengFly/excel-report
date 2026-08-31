package io.github.mengfly.excel.report.component;

import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleHolder;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import lombok.Getter;
import lombok.Setter;


public abstract class AbstractComponent extends StyleHolder implements Component {

    private ContainerTreeNode templateNode;
    @Getter
    @Setter
    private Size measuredSize;
    @Getter
    @Setter
    private Point position;

    @Override
    public ContainerTreeNode templateNode() {
        return templateNode;
    }

    @Override
    public void templateNode(ContainerTreeNode templateNode) {
        this.templateNode = templateNode;
    }

    @Override
    public final void export(ReportContext context) {
        Component.super.export(context);
    }

    /**
     * 组件类型的子类不允许重写该方法
     */
    @Override
    public final void onLayout(Point relativePosition) {
        position = relativePosition;
    }

    /**
     * 组件类型的子类不允许重写该方法
     */
    @Override
    public final void onMeasure(Size suggestSize) {
        // 默认的测量大小等于推荐的大小
        measuredSize = suggestSize;
    }
}
