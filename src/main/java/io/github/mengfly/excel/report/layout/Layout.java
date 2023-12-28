package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;

import java.util.List;

public interface Layout extends Container {

    List<Container> getContainers();

    default <T extends Container> T addItem(T item) {
        return addItem(item, null);
    }

    <T extends Container> T addItem(T item, Object constraint);


    @Override
    default String print() {
        StringBuilder print = new StringBuilder(Container.super.print());
        print.append(" {\n");
        for (Container container : getContainers()) {
            final String[] split = container.print().split("\n");
            for (String s : split) {
                print.append("    ").append(s).append("\n");
            }
        }
        print.append("}");
        return print.toString();
    }
}
