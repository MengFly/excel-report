package io.github.mengfly.excel.report.layout;

import lombok.Getter;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.style.StyleHolder;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class AbstractLayout extends StyleHolder implements Layout {

    protected final List<Container> containers = new ArrayList<>();

    @Override
    public <T extends Container> T addItem(T item, Object constraint) {
        this.containers.add(item);
        return item;
    }
}
