package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.style.key.StyleKey;

import java.util.Optional;

public interface StyleAble {

    <T> void addStyle(StyleKey<T> key, T value);

    void addStyle(StyleMap styleMap);

    <T> T getStyle(StyleKey<T> key, T defaultValue);

    <T> Optional<T> getStyle(StyleKey<T> key);

    StyleMap getStyle();
}
