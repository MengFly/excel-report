package io.github.mengfly.excel.report.template.parse;

import lombok.Getter;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.layout.VLayout;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

@Getter
public class VLayoutParser extends ContainerParser {
    private final String tagName = "VLayout";

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {

        final VLayout layout = new VLayout();

        doParseChildContainer(layout, containerTreeNode, context);

        return layout;
    }

    private void doParseChildContainer(VLayout layout, ContainerTreeNode containerTreeNode, DataContext context) {
        for (ContainerTreeNode treeNode : containerTreeNode.getChild(null)) {
            final Container container = ParserFactory.doParseElement(treeNode, context);
            layout.addItem(container, null);
        }

    }
}
