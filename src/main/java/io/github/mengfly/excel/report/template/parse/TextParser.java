package io.github.mengfly.excel.report.template.parse;

import lombok.Getter;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;

@Getter
public class TextParser extends ContainerParser {
    private final String tagName = "Text";

    @Override
    public Container parse(ContainerTreeNode containerTreeNode, DataContext context) {

        final TextComponent component = new TextComponent();
        component.setSize(getSize(containerTreeNode, context, Size.of(1, 1)));
        return component;
    }
}
