package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.layout.Layout;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

import java.util.List;

public abstract class AbstractLayoutParser extends ContainerParser {

    protected void doParseChildContainer(Layout layout, ContainerTreeNode layoutNode, DataContext context) {
        final List<ContainerTreeNode> childNodes = layoutNode.listChild(null);

        layoutNode.getProcessControl(context, "for").onFetch(processContext -> {
            for (ContainerTreeNode treeNode : childNodes) {
                final Container container = treeNode.render(processContext);
                if (container != null) {
                    layout.addItem(container, null);
                }
            }
        });

    }
}
