package io.github.mengfly.excel.report.style.key;

import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.Sheet;

public class SheetMarginStyleKey extends StyleKey<Double> {
    private final Short marginType;

    public SheetMarginStyleKey(String id, Short marginType) {
        super(id, null, Double.class);
        this.marginType = marginType;
    }

    @Override
    public void applyStyle(Object target, Object style) {
        if (target instanceof Sheet) {
            Sheet sheet = (Sheet) target;
            if (marginType == null) {
                sheet.setMargin(PageMargin.LEFT, ((Double) style));
                sheet.setMargin(PageMargin.RIGHT, ((Double) style));
                sheet.setMargin(PageMargin.TOP, ((Double) style));
                sheet.setMargin(PageMargin.BOTTOM, ((Double) style));
                sheet.setMargin(PageMargin.FOOTER, ((Double) style));
                sheet.setMargin(PageMargin.HEADER, ((Double) style));
            } else {
                sheet.setMargin(PageMargin.getByShortValue(marginType), (Double) style);
            }
        }
    }
}
