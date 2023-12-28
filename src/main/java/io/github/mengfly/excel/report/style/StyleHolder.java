package io.github.mengfly.excel.report.style;

import lombok.Getter;
import io.github.mengfly.excel.report.style.key.StyleKey;

import java.util.Optional;

@Getter
public class StyleHolder implements StyleAble {
    private final StyleMap style = new StyleMap();


    public <T> void addStyle(StyleKey<T> key, T value) {
        style.addStyle(key, value);
    }

    @Override
    public void addStyle(StyleMap styleMap) {
        if (styleMap != null) {
            style.addStyle(styleMap);
        }
    }

    public <T> T getStyle(StyleKey<T> key, T defaultValue) {
        return style.getStyle(key, defaultValue);
    }

    public <T> Optional<T> getStyle(StyleKey<T> key) {
        return style.getStyle(key);
    }
}
