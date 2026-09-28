package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.layout.GridLayout;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.Getter;

@Getter
public class GridLayoutParser extends AbstractLayoutParser{

    private final String tagName = "GridLayout";

    @Override
    protected Container parse(ContainerTreeNode node, DataContext context) {
        final GridLayout layout = new GridLayout();

        doParseChildContainer(layout, node, context);

        return layout;
    }
}
