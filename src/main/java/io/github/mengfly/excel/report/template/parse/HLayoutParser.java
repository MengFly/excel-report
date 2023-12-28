package io.github.mengfly.excel.report.template.parse;

import lombok.Getter;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

@Getter
public class HLayoutParser extends ContainerParser {
    private final String tagName = "HLayout";

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {
        final HLayout layout = new HLayout();

        doParseChildContainer(layout, containerTreeNode, context);

        return layout;
    }

    private void doParseChildContainer(HLayout layout, ContainerTreeNode containerTreeNode, DataContext context) {
        for (ContainerTreeNode treeNode : containerTreeNode.getChild(null)) {
            final Container container = treeNode.render(context);
            layout.addItem(container, null);
        }
    }

}
