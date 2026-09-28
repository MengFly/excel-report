package io.github.mengfly.excel.report.template.parse;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.Getter;

@Getter
public class TextParser extends ContainerParser {
    private final String tagName = "Text";

    @Override
    public Container parse(ContainerTreeNode node, DataContext context) {

        final TextComponent component = new TextComponent();
        component.setSize(getSize(node, context, Size.of(1, 1)));
        return component;
    }
}
