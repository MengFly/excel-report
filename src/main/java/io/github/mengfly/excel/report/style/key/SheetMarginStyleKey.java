package io.github.mengfly.excel.report.style.key;

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
                sheet.setMargin(Sheet.LeftMargin, ((Double) style));
                sheet.setMargin(Sheet.RightMargin, ((Double) style));
                sheet.setMargin(Sheet.TopMargin, ((Double) style));
                sheet.setMargin(Sheet.BottomMargin, ((Double) style));
                sheet.setMargin(Sheet.FooterMargin, ((Double) style));
                sheet.setMargin(Sheet.HeaderMargin, ((Double) style));
            } else {
                sheet.setMargin(marginType, (Double) style);
            }
        }
    }
}
