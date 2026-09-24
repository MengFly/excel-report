package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.split.PageColSplitComponent;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.TemplateManager;

public class PageColSplitParser extends ContainerParser {

    @Override
    public String getTagName() {
        return "PageColSplit";
    }

    @Override
    protected Container parse(TemplateManager manager, ContainerTreeNode node, DataContext context) {
        return new PageColSplitComponent();
    }
}
