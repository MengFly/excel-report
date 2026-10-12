package io.github.mengfly.excel.report.style;

import lombok.Getter;
import io.github.mengfly.excel.report.style.key.StyleKey;
import lombok.Setter;

import java.util.Optional;

@Getter
public class StyleHolder implements StyleAble {
    private final StyleMap style = new StyleMap();

    /**
     * 本次导出合并后的完整样式（含父级继承），仅在导出流程执行完成后有效。
     */
    @Setter
    private StyleMap finalStyle;


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
