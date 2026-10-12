package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.style.key.StyleKey;

import java.util.Optional;

public interface StyleAble {

    /**
     * 添加样式
     *
     * @param key   样式key
     * @param value 样式值
     * @param <T>   样式值类型
     * @see CellStyles 单元格样式Key定义类
     * @see SheetStyles 工作表样式Key定义类
     */
    <T> void addStyle(StyleKey<T> key, T value);

    /**
     * 添加样式
     * @param styleMap 样式map
     */
    void addStyle(StyleMap styleMap);

    /**
     * 获取样式
     * @param key 样式key
     * @param defaultValue 默认的样式
     * @return 样式信息
     * @param <T> 样式值类型
     */
    <T> T getStyle(StyleKey<T> key, T defaultValue);

    /**
     * 获取样式
     * @param key 样式key
     * @return 样式信息
     * @param <T> 样式值类型
     */
    <T> Optional<T> getStyle(StyleKey<T> key);

    /**
     * 获取样式
     * @return 样式信息
     */
    StyleMap getStyle();

    /**
     * 获取本次导出合并完成的完整样式（含从父组件继承的样式）。
     * <p>
     * 仅在导出流程执行完成后有效；未导出时返回 {@code null}。
     *
     * @return 合并后的完整样式
     */
    default StyleMap getFinalStyle() {
        return null;
    }

    /**
     * 记录本次导出合并完成的完整样式。由导出流程自动调用，调用方无需设置。
     *
     * @param styleMap 合并后的完整样式
     */
    default void setFinalStyle(StyleMap styleMap) {
    }
}
