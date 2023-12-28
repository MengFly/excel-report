package io.github.mengfly.excel.report.style;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import io.github.mengfly.excel.report.style.key.ColorStyleKey;
import io.github.mengfly.excel.report.style.key.SheetMarginStyleKey;
import io.github.mengfly.excel.report.style.key.StyleKey;

import java.util.HashMap;
import java.util.Map;

public class SheetStyles {

    private static final Map<String, StyleKey<?>> styleMap = new HashMap<>();
    public static final StyleMap DEFAULT_STYLE = new StyleMap();

    /**
     * 是否在单元格中显示其属性和样式。
     * 当设置 displayGuts 为 true 时。
     * 在打印或预览 Sheet 时，会在单元格中显示其属性和样式。
     * 例如单元格的合并范围、背景颜色、字体样式等信息。这有助于在设计和调试 Sheet 时进行检查和调整。
     *
     * @see Sheet#setDisplayGuts(boolean)
     */
    public static StyleKey<Boolean> displayGuts = register("displayGuts", "setDisplayGuts", Boolean.class);
    /**
     * 用于控制是否在单元格中显示零值
     *
     * @see Sheet#setDisplayZeros(boolean)
     */
    public static StyleKey<Boolean> displayZeros = register("displayZeros", "setDisplayZeros", Boolean.class);
    /**
     * 用于控制是否在下方显示行的总和
     *
     * @see Sheet#setRowSumsRight(boolean)
     */
    public static StyleKey<Boolean> rowSumsBelow = register("rowSumsBelow", "setRowSumsBelow", Boolean.class);
    /**
     * @see Sheet#setRowSumsRight(boolean)
     */
    public static StyleKey<Boolean> rowSumsRight = register("rowSumsRight", "setRowSumsRight", Boolean.class);
    /**
     * @see Sheet#setDisplayGridlines(boolean)
     */
    public static StyleKey<Boolean> displayGridlines = register("displayGridlines", "setDisplayGridlines", Boolean.class);
    /**
     * @see Sheet#setPrintGridlines(boolean)
     */
    public static StyleKey<Boolean> printGridlines = register("printGridlines", "setPrintGridlines", Boolean.class);
    /**
     * @see Sheet#setPrintRowAndColumnHeadings(boolean)
     */
    public static StyleKey<Boolean> printRowAndColumnHeadings = register("printRowAndColumnHeadings", "setPrintRowAndColumnHeadings", Boolean.class);
    /**
     * @see Sheet#setAutobreaks(boolean)
     */
    public static StyleKey<Boolean> autobreaks = register("autobreaks", "setAutobreaks", Boolean.class);
    /**
     * @see Sheet#setForceFormulaRecalculation(boolean)
     */
    public static StyleKey<Integer> forceFormulaRecalculation = register("forceFormulaRecalculation", "setForceFormulaRecalculation", Integer.class);
    /**
     * @see Sheet#setDefaultColumnWidth(int)
     */
    public static StyleKey<Integer> defaultColumnWidth = register("defaultColumnWidth", "setDefaultColumnWidth", Integer.class);
    /**
     * @see XSSFSheet#setDefaultRowHeight(short)
     */
    public static StyleKey<Float> defaultRowHeight = register("defaultRowHeight", "setDefaultRowHeightInPoints", Float.class);
    /**
     * @see Sheet#setDisplayRowColHeadings(boolean)
     */
    public static StyleKey<Boolean> displayRowColHeadings = register("displayRowColHeadings", "setDisplayRowColHeadings", Boolean.class);
    /**
     * @see Sheet#setDisplayFormulas(boolean)
     */
    public static StyleKey<Boolean> displayFormulas = register("displayFormulas", "setDisplayFormulas", Boolean.class);
    /**
     * @see Sheet#setFitToPage(boolean)
     */
    public static StyleKey<Boolean> fitToPage = register("fitToPage", "setFitToPage", Boolean.class);
    /**
     * @see Sheet#setHorizontallyCenter(boolean)
     */
    public static StyleKey<Boolean> horizontallyCenter = register("horizontallyCenter", "setHorizontallyCenter", Boolean.class);
    /**
     * @see Sheet#setVerticallyCenter(boolean)
     */
    public static StyleKey<Boolean> verticallyCenter = register("verticallyCenter", "setVerticallyCenter", Boolean.class);
    /**
     * @see Sheet#setZoom(int)
     */
    public static StyleKey<Integer> zoom = register("zoom", "setZoom", Integer.class);
    /**
     * @see XSSFSheet#setTabColor(XSSFColor)
     */
    public static StyleKey<XSSFColor> tabColor = register(new ColorStyleKey("tabColor", "setTabColor"));
    /**
     * @see XSSFSheet#setCommitted(boolean)
     */
    public static StyleKey<Boolean> committed = register("committed", "setCommitted", Boolean.class);
    /**
     * @see Sheet#setMargin(short, double)
     */
    public static StyleKey<Double> margin = register(new SheetMarginStyleKey("margin", null));
    /**
     * @see Sheet#setMargin(short, double)
     * @see Sheet#LeftMargin
     */
    public static StyleKey<Double> marginLeft = register(new SheetMarginStyleKey("marginLeft", Sheet.LeftMargin));
    /**
     * @see Sheet#setMargin(short, double)
     * @see Sheet#RightMargin
     */
    public static StyleKey<Double> marginRight = register(new SheetMarginStyleKey("marginRight", Sheet.RightMargin));
    /**
     * @see Sheet#setMargin(short, double)
     * @see Sheet#TopMargin
     */
    public static StyleKey<Double> marginTop = register(new SheetMarginStyleKey("marginTop", Sheet.TopMargin));
    /**
     * @see Sheet#setMargin(short, double)
     * @see Sheet#BottomMargin
     */
    public static StyleKey<Double> marginBottom = register(new SheetMarginStyleKey("marginBottom", Sheet.BottomMargin));
    /**
     * @see Sheet#setMargin(short, double)
     * @see Sheet#HeaderMargin
     */
    public static StyleKey<Double> marginHeader = register(new SheetMarginStyleKey("marginHeader", Sheet.HeaderMargin));
    /**
     * @see Sheet#setMargin(short, double)
     * @see Sheet#FooterMargin
     */
    public static StyleKey<Double> marginFooter = register(new SheetMarginStyleKey("marginFooter", Sheet.FooterMargin));
    /**
     * @see Sheet#protectSheet(String)
     */
    public static StyleKey<String> password = register("password", "protectSheet", String.class);


    static {
        DEFAULT_STYLE.addStyle(defaultRowHeight, 20f);
    }

    public static <T> void setDefaultStyle(StyleKey<T> key, T defaultValue) {
        DEFAULT_STYLE.addStyle(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static <T> StyleKey<T> getStyleKey(String key) {
        return (StyleKey<T>) styleMap.get(key);
    }

    private static <T> StyleKey<T> register(String id, String methodName, Class<T> type) {
        return register(new StyleKey<>(id, methodName, type));
    }

    private static <T> StyleKey<T> register(StyleKey<T> key) {
        styleMap.put(key.getId(), key);
        return key;
    }

    public static void initSheetStyle(Sheet sheet, StyleMap sheetStyle) {

        for (StyleKey<?> value : styleMap.values()) {
            sheetStyle.getStyle(value).ifPresent(object -> value.applyStyle(sheet, object));
        }
    }
}
