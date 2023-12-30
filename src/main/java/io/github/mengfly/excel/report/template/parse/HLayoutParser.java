package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.Getter;

@Getter
public class HLayoutParser extends AbstractLayoutParser {
    private final String tagName = "HLayout";

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {
        final HLayout layout = new HLayout();

        doParseChildContainer(layout, containerTreeNode, context);

        return layout;
    }


}
