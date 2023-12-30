package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.LinkComponent;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

public class LinkParser extends ContainerParser {
    @Override
    public String getTagName() {
        return "Link";
    }

    @Override
    protected Container parse(ContainerTreeNode containerTreeNode, DataContext context) {
        LinkComponent component = new LinkComponent();
        component.setSize(getSize(containerTreeNode, context, Size.of(1, 1)));
        return component;
    }
}
