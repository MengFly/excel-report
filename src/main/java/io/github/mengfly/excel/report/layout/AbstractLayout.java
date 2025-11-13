package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleHolder;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class AbstractLayout extends StyleHolder implements Layout {

    @Setter
    private ContainerTreeNode templateNode;
    protected final List<Container> containers = new ArrayList<>();
    protected Size measuredSize;

    @Override
    public final void export(ReportContext context, Point point) {
        Layout.super.export(context, point);
    }

    @Override
    public <T extends Container> T addItem(T item, Object constraint) {
        this.containers.add(item);
        return item;
    }
}
