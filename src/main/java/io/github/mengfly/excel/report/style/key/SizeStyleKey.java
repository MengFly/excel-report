package io.github.mengfly.excel.report.style.key;

import io.github.mengfly.excel.report.entity.Size;

public class SizeStyleKey extends StyleKey<Size> {
    public SizeStyleKey(String id) {
        super(id, null, null);
    }

    @Override
    public Size getStyle(String property) {
        return Size.of(property);
    }

    @Override
    public String toString(Size property) {
        return property.toString();
    }

    @Override
    public void applyStyle(Object target, Object style) {

    }
}
