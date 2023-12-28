package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.style.key.StyleKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class StyleMap {
    private final Map<String, String> styleMap = new HashMap<>();

    public <T> void addStyle(StyleKey<T> key, T value) {
        styleMap.put(key.getId(), key.toString(value));
    }

    public StyleMap getStyleMap(Collection<StyleKey<?>> keys) {
        StyleMap style = new StyleMap();
        for (StyleKey<?> key : keys) {
            style.styleMap.put(key.getId(), styleMap.get(key.getId()));
        }
        return style;
    }

    public void addStyle(StyleMap styleMap) {
        if (styleMap != null) {
            this.styleMap.putAll(styleMap.toMap());
        }
    }

    private Map<String, String> toMap() {
        return styleMap;
    }

    public <T> T getStyle(StyleKey<T> key, T defaultValue) {
        final String value = styleMap.get(key.getId());
        if (value == null) {
            return defaultValue;
        }
        return key.getStyle(value);
    }

    public <T> Optional<T> getStyle(StyleKey<T> key) {
        if (containsKey(key)) {
            return Optional.ofNullable(getStyle(key, null));
        }
        return Optional.empty();
    }

    public boolean isEmpty() {
        return styleMap.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StyleMap) {
            return styleMap.equals(((StyleMap) obj).styleMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return styleMap.hashCode();
    }

    public boolean containsKey(StyleKey<?> key) {
        return styleMap.containsKey(key.getId());
    }

    public StyleMap createChildStyleMap(StyleMap childStyleMap) {
        StyleMap styleMap = new StyleMap();
        styleMap.addStyle(this);
        if (childStyleMap != null) {
            styleMap.addStyle(childStyleMap);
        }
        return styleMap;
    }
}
