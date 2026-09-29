package io.github.mengfly.excel.report.style.key;

import io.github.mengfly.excel.report.entity.Size;
import org.apache.poi.ss.usermodel.CellStyle;

public class SizeStyleKey extends StyleKey<Size> {
    public SizeStyleKey(String id) {
        super(id, CellStyle.class, Size.class, (o, size) -> {
        });
    }

    @Override
    public Size getStyle(String styleString) {
        return Size.of(styleString);
    }

    @Override
    public String toString(Size style) {
        return style.toString();
    }
}
