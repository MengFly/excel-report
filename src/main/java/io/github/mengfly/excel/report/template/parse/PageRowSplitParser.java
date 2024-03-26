package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.split.PageRowSplitComponent;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

public class PageRowSplitParser extends ContainerParser {

    @Override
    public String getTagName() {
        return "PageRowSplit";
    }

    @Override
    protected Container parse(ContainerTreeNode containerTreeNode, DataContext context) {
        return new PageRowSplitComponent();
    }
}
