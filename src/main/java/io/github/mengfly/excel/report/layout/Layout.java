package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;

import java.util.List;

public interface Layout extends Container {

    /**
     * 获取布局中包含的所有子组件列表
     *
     * @return 子组件列表
     */
    List<Container> getContainers();

    /**
     * 添加子组件
     *
     * @param item 子组件
     * @param <T>  子组件类型
     * @return 子组件
     */
    default <T extends Container> T addItem(T item) {
        return addItem(item, null);
    }

    /**
     * 添加子组件
     *
     * @param item       子组件
     * @param constraint 约束
     * @param <T>        子组件类型
     * @return 子组件
     */
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
