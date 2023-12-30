package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.layout.VLayout;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.Getter;

@Getter
public class VLayoutParser extends AbstractLayoutParser {
    private final String tagName = "VLayout";

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {

        final VLayout layout = new VLayout();

        doParseChildContainer(layout, containerTreeNode, context);

        return layout;
    }

}
