package io.github.mengfly.excel.report.component.table;

import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleHolder;
import io.github.mengfly.excel.report.style.key.StyleKey;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;


@Getter
@Setter
public abstract class TableColumn extends StyleHolder {
    private String id;
    private String name;
    private int columnSpan = 1;

    private StyleHolder dataStyle = new StyleHolder();

    public TableColumn() {
        addStyle(CellStyles.fontBold, true);
        addStyle(CellStyles.fontHeight, 13.);
        addStyle(CellStyles.alignHorizontal, HorizontalAlignment.CENTER);
        addStyle(CellStyles.alignVertical, VerticalAlignment.CENTER);
    }

    public <T> void addDataStyle(StyleKey<T> key, T value) {
        dataStyle.addStyle(key, value);
    }

    public abstract Object getValue(Object object);

    @Override
    public String toString() {
        return id + ":" + name;
    }
}
