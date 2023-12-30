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
}
