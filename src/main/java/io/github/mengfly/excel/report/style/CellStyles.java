package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.key.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.util.HashMap;
import java.util.Map;

/**
 * 单元格样式定义类
 */
@Slf4j
public class CellStyles {
    private static final XSSFColor BLACK = createColor(0x000000);
    private static final XSSFColor WHITE = createColor(0xffffff);

    private static final Map<String, StyleKey<?>> cellStyleMap = new HashMap<>();
    private static final Map<String, StyleKey<?>> fontStyleMap = new HashMap<>();
    public static final StyleMap DEFAULT_STYLE = new StyleMap();

    /**
     * @see CellStyle#setBorderTop(BorderStyle)
     */
    public static final StyleKey<BorderStyle> borderTop = register("borderTop", "setBorderTop", BorderStyle.class);
    /**
     * @see CellStyle#setBorderBottom(BorderStyle)
     */
    public static final StyleKey<BorderStyle> borderBottom = register("borderBottom", "setBorderBottom", BorderStyle.class);
    /**
     * @see CellStyle#setBorderLeft(BorderStyle)
     */
    public static final StyleKey<BorderStyle> borderLeft = register("borderLeft", "setBorderLeft", BorderStyle.class);
    /**
     * @see CellStyle#setBorderRight(BorderStyle)
     */
    public static final StyleKey<BorderStyle> borderRight = register("borderRight", "setBorderRight", BorderStyle.class);
    /**
     * @see XSSFCellStyle#setTopBorderColor(XSSFColor)
     */
    public static final StyleKey<XSSFColor> topBorderColor = register(new ColorStyleKey("topBorderColor", "setTopBorderColor"));
    /**
     * @see XSSFCellStyle#setBottomBorderColor(XSSFColor)
     */
    public static final StyleKey<XSSFColor> bottomBorderColor = register(new ColorStyleKey("bottomBorderColor", "setBottomBorderColor"));
    /**
     * @see XSSFCellStyle#setLeftBorderColor(XSSFColor)
     */
    public static final StyleKey<XSSFColor> leftBorderColor = register(new ColorStyleKey("leftBorderColor", "setLeftBorderColor"));
    /**
     * @see XSSFCellStyle#setRightBorderColor(XSSFColor)
     */
    public static final StyleKey<XSSFColor> rightBorderColor = register(new ColorStyleKey("rightBorderColor", "setRightBorderColor"));
    /**
     * @see CellStyle#setHidden(boolean)
     */
    public static final StyleKey<Boolean> hidden = register("hidden", "setHidden", Boolean.class);
    /**
     * @see CellStyle#setLocked(boolean)
     */
    public static final StyleKey<Boolean> locked = register("locked", "setLocked", Boolean.class);
    /**
     * @see CellStyle#setAlignment(HorizontalAlignment)
     */
    public static final StyleKey<HorizontalAlignment> alignHorizontal = register("alignHorizontal", "setAlignment", HorizontalAlignment.class);
    /**
     * @see CellStyle#setVerticalAlignment(VerticalAlignment)
     */
    public static final StyleKey<VerticalAlignment> alignVertical = register("alignVertical", "setVerticalAlignment", VerticalAlignment.class);
    /**
     * @see CellStyle#setWrapText(boolean)
     */
    public static final StyleKey<Boolean> wrapText = register("wrapText", "setWrapText", Boolean.class);
    /**
     * @see CellStyle#setRotation(short)
     */
    public static final StyleKey<Short> rotation = register("rotation", "setRotation", Short.class);
    /**
     * @see CellStyle#setIndention(short)
     */
    public static final StyleKey<Short> indention = register("indention", "setIndention", Short.class);
    /**
     * @see CellStyle#setFillPattern(FillPatternType)
     */
    public static final StyleKey<FillPatternType> fillPattern = register("fillPattern", "setFillPattern", FillPatternType.class);
    /**
     * @see XSSFCellStyle#setFillForegroundColor(XSSFColor)
     */
    public static final StyleKey<XSSFColor> fillForegroundColor = register(new ColorStyleKey("fillForegroundColor", "setFillForegroundColor"));
    /**
     * @see XSSFCellStyle#setReadingOrder(ReadingOrder)
     */
    public static final StyleKey<ReadingOrder> readingOrder = register("readingOrder", "setReadingOrder", ReadingOrder.class);
    /**
     * @see XSSFCellStyle#setShrinkToFit(boolean)
     */
    public static final StyleKey<Boolean> shrinkToFit = register("shrinkToFit", "setShrinkToFit", Boolean.class);
    /**
     * 单元格宽度，设置为 auto 说明自动宽度，设置为数字则为固定宽度
     */
    public static final StyleKey<String> width = register(new CellWidthHeightKey("width"));
    /**
     * 单元格高度
     */
    public static final StyleKey<String> height = register(new CellWidthHeightKey("height"));
    public static final StyleKey<String> dataFormat = register(new NoOpStyleKey<>("dataFormat", String.class));
    /**
     * 组件权重，在自动调整组件大小的时候使用，例如
     * <p>
     * 在HLayout中，如果组件的宽度设置为了 -1, 那么该属性生效
     * <p>
     * 在VLayout中，如果组件的高度设置为了 -1, 那么该属性生效
     */
    public static final StyleKey<Double> weight = register(new NoOpStyleKey<>("weight", Double.class));

    // =================================================================================================================
    // Font Style
    // =================================================================================================================
    /**
     * @see XSSFFont#setFontName(String)
     */
    public static final StyleKey<String> fontName = registerFont("fontName", "setFontName", String.class);
    /**
     * @see XSSFFont#setFontHeight(double)
     */
    public static final StyleKey<Double> fontHeight = registerFont("fontHeight", "setFontHeight", Double.class);
    /**
     * @see XSSFFont#setItalic(boolean)
     */
    public static final StyleKey<Boolean> fontItalic = registerFont("fontItalic", "setItalic", Boolean.class);
    /**
     * @see XSSFFont#setStrikeout(boolean)
     */
    public static final StyleKey<Boolean> fontStrikeout = registerFont("fontStrikeout", "setStrikeout", Boolean.class);
    /**
     * @see XSSFFont#setColor(XSSFColor)
     */
    public static final StyleKey<XSSFColor> fontColor = registerFont(new ColorStyleKey("fontColor", "setColor"));
    /**
     * @see XSSFFont#setUnderline(FontUnderline)
     */
    public static final StyleKey<FontUnderline> fontUnderline = registerFont("fontUnderline", "setUnderline", FontUnderline.class);
    /**
     * @see XSSFFont#setBold(boolean)
     */
    public static final StyleKey<Boolean> fontBold = registerFont("fontBold", "setBold", Boolean.class);
    /**
     * @see XSSFFont#setFamily(FontFamily)
     */
    public static final StyleKey<FontFamily> fontFamily = registerFont("fontFamily", "setFamily", FontFamily.class);

    public static final StyleKey<Size> preferredSize = register(new SizeStyleKey("preferredSize"));


    static {
        setDefaultStyle(borderTop, BorderStyle.THIN);
        setDefaultStyle(borderBottom, BorderStyle.THIN);
        setDefaultStyle(borderLeft, BorderStyle.THIN);
        setDefaultStyle(borderRight, BorderStyle.THIN);
        setDefaultStyle(topBorderColor, BLACK);
        setDefaultStyle(bottomBorderColor, BLACK);
        setDefaultStyle(leftBorderColor, BLACK);
        setDefaultStyle(rightBorderColor, BLACK);
        setDefaultStyle(hidden, false);
        setDefaultStyle(locked, false);
        setDefaultStyle(alignHorizontal, HorizontalAlignment.LEFT);
        setDefaultStyle(alignVertical, VerticalAlignment.CENTER);
        setDefaultStyle(wrapText, false);
        setDefaultStyle(rotation, (short) 0);
        setDefaultStyle(indention, (short) 0);

        setDefaultStyle(fillPattern, FillPatternType.SOLID_FOREGROUND);
        setDefaultStyle(fillForegroundColor, WHITE);
        setDefaultStyle(readingOrder, ReadingOrder.LEFT_TO_RIGHT);
        setDefaultStyle(shrinkToFit, false);

        setDefaultStyle(fontName, "Arial");
        setDefaultStyle(fontHeight, 12.);
        setDefaultStyle(fontItalic, false);
        setDefaultStyle(fontStrikeout, false);
        setDefaultStyle(fontColor, BLACK);
        setDefaultStyle(fontUnderline, FontUnderline.NONE);
        setDefaultStyle(fontBold, false);
        setDefaultStyle(fontFamily, FontFamily.NOT_APPLICABLE);
    }

    @SuppressWarnings("unchecked")
    public static <T> StyleKey<T> getStyleKey(String key) {
        if (isFontStyle(key)) {
            return (StyleKey<T>) fontStyleMap.get(key);
        } else {
            return (StyleKey<T>) cellStyleMap.get(key);
        }
    }


    private static <T> StyleKey<T> register(StyleKey<T> key) {
        cellStyleMap.put(key.getId(), key);
        return key;
    }

    private static <T> StyleKey<T> register(String id, String methodName, Class<T> type) {
        return register(new StyleKey<>(id, methodName, type));
    }

    private static <T> StyleKey<T> registerFont(StyleKey<T> key) {
        fontStyleMap.put(key.getId(), key);
        return key;
    }

    private static <T> StyleKey<T> registerFont(String id, String methodName, Class<T> type) {
        return registerFont(new StyleKey<>(id, methodName, type));
    }


    public static CellStyle createCellStyle(Workbook workbook, StyleMap cellStyleMap) {
        CellStyle cellStyle = workbook.createCellStyle();

        for (StyleKey<?> key : CellStyles.cellStyleMap.values()) {
            if (cellStyleMap.containsKey(key)) {
                setCellStyleValue(cellStyle, cellStyleMap, key);
            }
        }
        return cellStyle;
    }

    public static Font createFont(Workbook workbook, StyleMap fontStyleMap) {
        Font font = workbook.createFont();

        for (StyleKey<?> value : CellStyles.fontStyleMap.values()) {
            if (fontStyleMap.containsKey(value)) {
                setFontStyleValue(font, fontStyleMap, value);
            }
        }
        return font;
    }


    public static boolean isFontStyle(StyleKey<?> key) {
        return isFontStyle(key.getId());
    }

    public static boolean isFontStyle(String key) {
        return fontStyleMap.containsKey(key);
    }

    public static <T> void setDefaultStyle(StyleKey<T> key, T defaultValue) {
        DEFAULT_STYLE.addStyle(key, defaultValue);
    }

    private static void setFontStyleValue(Font font, StyleMap fontStyleMap, StyleKey<?> key) {
        fontStyleMap.getStyle(key).ifPresent(style -> key.applyStyle(font, style));
    }

    private static void setCellStyleValue(CellStyle cellStyle, StyleMap styleMap, StyleKey<?> key) {
        styleMap.getStyle(key).ifPresent(style -> key.applyStyle(cellStyle, style));
    }


    public static StyleMap toFontStyle(StyleMap map) {
        return map.getStyleMap(fontStyleMap.values());
    }


    public static XSSFColor createColor(int rgb) {
        return new XSSFColor(new java.awt.Color(rgb), null);
    }

    public static StyleMap createStyle(Map<String, String> map) {
        StyleMap styleMap = new StyleMap();
        if (map == null || map.isEmpty()) {
            return styleMap;
        }
        map.forEach((key, value) -> {
            final StyleKey<Object> styleKey = getStyleKey(key);
            if (styleKey != null) {
                try {
                    styleMap.addStyle(styleKey, styleKey.getStyle(value));
                } catch (Exception ignore) {

                }
            }
        });
        return styleMap;
    }
}
