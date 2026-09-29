package io.github.mengfly.excel.report.style.key;

import lombok.Getter;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.Sheet;

@Getter
public class SheetMarginStyleKey extends StyleKey<Double> {
    private final Short marginType;

    public SheetMarginStyleKey(String id, Short marginType) {
        super(id, Sheet.class, Double.class, (sheet, margin) -> {
            if (marginType == null) {
                sheet.setMargin(PageMargin.LEFT, margin);
                sheet.setMargin(PageMargin.RIGHT, margin);
                sheet.setMargin(PageMargin.TOP, margin);
                sheet.setMargin(PageMargin.BOTTOM, margin);
                sheet.setMargin(PageMargin.FOOTER, margin);
                sheet.setMargin(PageMargin.HEADER, margin);
            } else {
                sheet.setMargin(PageMargin.getByShortValue(marginType), margin);
            }
        });
        this.marginType = marginType;
    }
    
}
