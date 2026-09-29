package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.style.key.ColorStyleKey;
import io.github.mengfly.excel.report.style.key.SheetMarginStyleKey;
import io.github.mengfly.excel.report.style.key.StyleKey;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.function.BiConsumer;

/**
 * 工作表样式定义类
 */
public class SheetStyles {

    private static final StyleRegistry REGISTRY = new StyleRegistry();
    /**
     * 默认的工作表样式
     */
    public static final StyleMap DEFAULT_STYLE = new StyleMap();

    /**
     * 是否在单元格中显示其属性和样式。
     * 当设置 displayGuts 为 true 时。
     * 在打印或预览 Sheet 时，会在单元格中显示其属性和样式。
     * 例如单元格的合并范围、背景颜色、字体样式等信息。这有助于在设计和调试 Sheet 时进行检查和调整。
     */
    public static StyleKey<Boolean> displayGuts =
            register("displayGuts", Sheet.class, Boolean.class, Sheet::setDisplayGuts);

    /**
     * 用于控制是否在单元格中显示零值
     */
    public static StyleKey<Boolean> displayZeros =
            register("displayZeros", Sheet.class, Boolean.class, Sheet::setDisplayZeros);
    /**
     * 用于控制是否在下方显示行的总和
     */
    public static StyleKey<Boolean> rowSumsBelow =
            register("rowSumsBelow", Sheet.class, Boolean.class, Sheet::setRowSumsBelow);
    public static StyleKey<Boolean> rowSumsRight =
            register("rowSumsRight", Sheet.class, Boolean.class, Sheet::setRowSumsRight);
    public static StyleKey<Boolean> displayGridlines =
            register("displayGridlines", Sheet.class, Boolean.class, Sheet::setDisplayGridlines);

    public static StyleKey<Boolean> printGridlines =
            register("printGridlines", Sheet.class, Boolean.class, Sheet::setPrintGridlines);

    public static StyleKey<Boolean> printRowAndColumnHeadings =
            register("printRowAndColumnHeadings", Sheet.class, Boolean.class, Sheet::setPrintRowAndColumnHeadings);
    public static StyleKey<Boolean> autobreaks =
            register("autobreaks", Sheet.class, Boolean.class, Sheet::setAutobreaks);
    public static StyleKey<Boolean> forceFormulaRecalculation =
            register("forceFormulaRecalculation", Sheet.class, Boolean.class, Sheet::setForceFormulaRecalculation);
    public static StyleKey<Integer> defaultColumnWidth =
            register("defaultColumnWidth", Sheet.class, Integer.class, Sheet::setDefaultColumnWidth);

    public static StyleKey<Float> defaultRowHeight =
            register("defaultRowHeight", XSSFSheet.class, Float.class, XSSFSheet::setDefaultRowHeightInPoints);
    public static StyleKey<Boolean> displayRowColHeadings =
            register("displayRowColHeadings", Sheet.class, Boolean.class, Sheet::setDisplayRowColHeadings);
    public static StyleKey<Boolean> displayFormulas =
            register("displayFormulas", Sheet.class, Boolean.class, Sheet::setDisplayFormulas);
    public static StyleKey<Boolean> fitToPage =
            register("fitToPage", Sheet.class, Boolean.class, Sheet::setFitToPage);
    public static StyleKey<Boolean> horizontallyCenter =
            register("horizontallyCenter", Sheet.class, Boolean.class, Sheet::setHorizontallyCenter);
    public static StyleKey<Boolean> verticallyCenter =
            register("verticallyCenter", Sheet.class, Boolean.class, Sheet::setVerticallyCenter);
    public static StyleKey<Integer> zoom =
            register("zoom", Sheet.class, Integer.class, Sheet::setZoom);
    public static StyleKey<XSSFColor> tabColor =
            register(new ColorStyleKey("tabColor", XSSFSheet.class, XSSFSheet::setTabColor));
    public static StyleKey<Boolean> committed =
            register("committed", XSSFSheet.class, Boolean.class, XSSFSheet::setCommitted);

    public static StyleKey<Double> margin = register(new SheetMarginStyleKey("margin", null));
    public static StyleKey<Double> marginLeft = register(new SheetMarginStyleKey("marginLeft", Sheet.LeftMargin));
    public static StyleKey<Double> marginRight = register(new SheetMarginStyleKey("marginRight", Sheet.RightMargin));
    public static StyleKey<Double> marginTop = register(new SheetMarginStyleKey("marginTop", Sheet.TopMargin));
    public static StyleKey<Double> marginBottom = register(new SheetMarginStyleKey("marginBottom", Sheet.BottomMargin));
    public static StyleKey<Double> marginHeader = register(new SheetMarginStyleKey("marginHeader", Sheet.HeaderMargin));
    public static StyleKey<Double> marginFooter = register(new SheetMarginStyleKey("marginFooter", Sheet.FooterMargin));
    public static StyleKey<String> password = register("password", Sheet.class, String.class, Sheet::protectSheet);

    static {
        DEFAULT_STYLE.addStyle(defaultRowHeight, 20f);
    }

    public static <T> StyleKey<T> getStyleKey(String key) {
        return REGISTRY.getStyleKey(key);
    }


    private static <T> StyleKey<T> register(StyleKey<T> key) {
        return REGISTRY.register(key);
    }


    private static <T, S> StyleKey<S> register(String id, Class<T> targetType, Class<S> styleType, BiConsumer<T, S> applier) {
        return REGISTRY.register(id, targetType, styleType, applier);
    }

    public static <T> void initStyle(T target, StyleMap styleMap) {
        REGISTRY.initStyle(target, styleMap);
    }
}
