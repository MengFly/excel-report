package io.github.mengfly.excel.report.style;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import io.github.mengfly.excel.report.style.key.CellWidthHeightKey;
import io.github.mengfly.excel.report.style.key.ColorStyleKey;
import io.github.mengfly.excel.report.style.key.StyleKey;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CellStyles {
    private static final XSSFColor BLACK = createColor(0x000000);
    private static final XSSFColor WHITE = createColor(0xffffff);

    private static final Map<String, StyleKey<?>> cellStyleMap = new HashMap<>();
    private static final Map<String, StyleKey<?>> fontStyleMap = new HashMap<>();
    public static final StyleMap DEFAULT_STYLE = new StyleMap();

    public static final StyleKey<BorderStyle> borderTop = register("borderTop", "setBorderTop", BorderStyle.class);
    public static final StyleKey<BorderStyle> borderBottom = register("borderBottom", "setBorderBottom", BorderStyle.class);
    public static final StyleKey<BorderStyle> borderLeft = register("borderLeft", "setBorderLeft", BorderStyle.class);
    public static final StyleKey<BorderStyle> borderRight = register("borderRight", "setBorderRight", BorderStyle.class);
    public static final StyleKey<XSSFColor> topBorderColor = register(new ColorStyleKey("topBorderColor", "setTopBorderColor"));
    public static final StyleKey<XSSFColor> bottomBorderColor = register(new ColorStyleKey("bottomBorderColor", "setBottomBorderColor"));
    public static final StyleKey<XSSFColor> leftBorderColor = register(new ColorStyleKey("leftBorderColor", "setLeftBorderColor"));
    public static final StyleKey<XSSFColor> rightBorderColor = register(new ColorStyleKey("rightBorderColor", "setRightBorderColor"));
    public static final StyleKey<Boolean> hidden = register("hidden", "setHidden", Boolean.class);
    public static final StyleKey<Boolean> locked = register("locked", "setLocked", Boolean.class);
    public static final StyleKey<HorizontalAlignment> alignHorizontal = register("alignHorizontal", "setAlignment", HorizontalAlignment.class);
    public static final StyleKey<VerticalAlignment> alignVertical = register("alignVertical", "setVerticalAlignment", VerticalAlignment.class);
    public static final StyleKey<Boolean> wrapText = register("wrapText", "setWrapText", Boolean.class);
    public static final StyleKey<Short> rotation = register("rotation", "setRotation", Short.class);
    public static final StyleKey<Short> indention = register("indention", "setIndention", Short.class);
    public static final StyleKey<FillPatternType> fillPattern = register("fillPattern", "setFillPattern", FillPatternType.class);
    public static final StyleKey<XSSFColor> fillForegroundColor = register(new ColorStyleKey("fillForegroundColor", "setFillForegroundColor"));
    public static final StyleKey<ReadingOrder> readingOrder = register("readingOrder", "setReadingOrder", ReadingOrder.class);
    public static final StyleKey<Boolean> shrinkToFit = register("shrinkToFit", "setShrinkToFit", Boolean.class);
    public static final StyleKey<String> width = register(new CellWidthHeightKey("width"));
    public static final StyleKey<String> height = register(new CellWidthHeightKey("height"));

    // =================================================================================================================
    // Font Style
    // =================================================================================================================
    public static final StyleKey<String> fontName = registerFont("fontName", "setFontName", String.class);
    public static final StyleKey<Double> fontHeight = registerFont("fontHeight", "setFontHeight", Double.class);
    public static final StyleKey<Boolean> fontItalic = registerFont("fontItalic", "setItalic", Boolean.class);
    public static final StyleKey<Boolean> fontStrikeout = registerFont("fontStrikeout", "setStrikeout", Boolean.class);
    public static final StyleKey<XSSFColor> fontColor = registerFont(new ColorStyleKey("fontColor", "setColor"));
    public static final StyleKey<FontUnderline> fontUnderline = registerFont("fontUnderline", "setUnderline", FontUnderline.class);
    public static final StyleKey<Boolean> fontBold = registerFont("fontBold", "setBold", Boolean.class);
    public static final StyleKey<FontFamily> fontFamily = registerFont("fontFamily", "setFamily", FontFamily.class);


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
}
