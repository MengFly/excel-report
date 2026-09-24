package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.TemplateManager;
import lombok.Getter;

@Getter
public class HLayoutParser extends AbstractLayoutParser {
    private final String tagName = "HLayout";

    @Override
    public Container parse(TemplateManager manager, ContainerTreeNode node, DataContext context) {
        final HLayout layout = new HLayout();

        doParseChildContainer(manager, layout, node, context);

        return layout;
    }


}
