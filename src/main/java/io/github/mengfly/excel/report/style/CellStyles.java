package io.github.mengfly.excel.report.style;

import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.key.ColorStyleKey;
import io.github.mengfly.excel.report.style.key.NoOpStyleKey;
import io.github.mengfly.excel.report.style.key.SizeStyleKey;
import io.github.mengfly.excel.report.style.key.StyleKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 单元格样式定义类
 */
@Slf4j
public class CellStyles {
    private static final XSSFColor BLACK = createColor(0x000000);
    private static final XSSFColor WHITE = createColor(0xffffff);

    private static final Map<String, StyleKey<?>> styleMap = new HashMap<>();
    public static final StyleMap DEFAULT_STYLE = new StyleMap();

    public static final StyleKey<BorderStyle> borderTop =
            register("borderTop", CellStyle.class, BorderStyle.class, CellStyle::setBorderTop);
    public static final StyleKey<BorderStyle> borderBottom =
            register("borderBottom", CellStyle.class, BorderStyle.class, CellStyle::setBorderBottom);
    public static final StyleKey<BorderStyle> borderLeft =
            register("borderLeft", CellStyle.class, BorderStyle.class, CellStyle::setBorderLeft);
    public static final StyleKey<BorderStyle> borderRight =
            register("borderRight", CellStyle.class, BorderStyle.class, CellStyle::setBorderRight);

    public static final StyleKey<XSSFColor> topBorderColor =
            register(new ColorStyleKey("topBorderColor", XSSFCellStyle.class, XSSFCellStyle::setTopBorderColor));
    public static final StyleKey<XSSFColor> bottomBorderColor =
            register(new ColorStyleKey("bottomBorderColor", XSSFCellStyle.class, XSSFCellStyle::setBottomBorderColor));
    public static final StyleKey<XSSFColor> leftBorderColor =
            register(new ColorStyleKey("leftBorderColor", XSSFCellStyle.class, XSSFCellStyle::setLeftBorderColor));
    public static final StyleKey<XSSFColor> rightBorderColor =
            register(new ColorStyleKey("rightBorderColor", XSSFCellStyle.class, XSSFCellStyle::setRightBorderColor));

    public static final StyleKey<Boolean> hidden =
            register("hidden", CellStyle.class, Boolean.class, CellStyle::setHidden);
    public static final StyleKey<Boolean> locked =
            register("locked", CellStyle.class, Boolean.class, CellStyle::setLocked);
    public static final StyleKey<HorizontalAlignment> alignHorizontal =
            register("alignHorizontal", CellStyle.class, HorizontalAlignment.class, CellStyle::setAlignment);
    public static final StyleKey<VerticalAlignment> alignVertical =
            register("alignVertical", CellStyle.class, VerticalAlignment.class, CellStyle::setVerticalAlignment);

    public static final StyleKey<Boolean> wrapText =
            register("wrapText", CellStyle.class, Boolean.class, CellStyle::setWrapText);
    public static final StyleKey<Short> rotation =
            register("rotation", CellStyle.class, Short.class, CellStyle::setRotation);
    public static final StyleKey<Short> indention =
            register("indention", CellStyle.class, Short.class, CellStyle::setIndention);

    public static final StyleKey<FillPatternType> fillPattern =
            register("fillPattern", CellStyle.class, FillPatternType.class, CellStyle::setFillPattern);
    public static final StyleKey<XSSFColor> fillForegroundColor =
            register(new ColorStyleKey("fillForegroundColor", XSSFCellStyle.class, XSSFCellStyle::setFillForegroundColor));
    public static final StyleKey<XSSFColor> fillBackgroundColor =
            register(new ColorStyleKey("fillBackgroundColor", XSSFCellStyle.class, XSSFCellStyle::setFillBackgroundColor));
    public static final StyleKey<ReadingOrder> readingOrder =
            register("readingOrder", XSSFCellStyle.class, ReadingOrder.class, XSSFCellStyle::setReadingOrder);
    public static final StyleKey<Boolean> shrinkToFit =
            register("shrinkToFit", CellStyle.class, Boolean.class, CellStyle::setShrinkToFit);
    public static final StyleKey<String> width =
            register(new NoOpStyleKey<>("width", CellStyle.class, String.class));
    /**
     * 单元格高度
     */
    public static final StyleKey<String> height = register(new NoOpStyleKey<>("height", CellStyle.class, String.class));
    public static final StyleKey<String> dataFormat = register(new NoOpStyleKey<>("dataFormat", CellStyle.class, String.class));
    /**
     * 组件权重，在自动调整组件大小的时候使用，例如
     * <p>
     * 在HLayout中，如果组件的宽度设置为了 -1, 那么该属性生效
     * <p>
     * 在VLayout中，如果组件的高度设置为了 -1, 那么该属性生效
     */
    public static final StyleKey<Double> weight = register(new NoOpStyleKey<>("weight", CellStyle.class, Double.class));

    // =================================================================================================================
    // Font Style
    // =================================================================================================================
    public static final StyleKey<String> fontName =
            register("fontName", XSSFFont.class, String.class, XSSFFont::setFontName);
    public static final StyleKey<Double> fontHeight =
            register("fontHeight", XSSFFont.class, Double.class, XSSFFont::setFontHeight);
    public static final StyleKey<Boolean> fontItalic =
            register("fontItalic", XSSFFont.class, Boolean.class, XSSFFont::setItalic);
    public static final StyleKey<Boolean> fontStrikeout =
            register("fontStrikeout", XSSFFont.class, Boolean.class, XSSFFont::setStrikeout);
    public static final StyleKey<XSSFColor> fontColor =
            register(new ColorStyleKey("fontColor", XSSFFont.class, XSSFFont::setColor));
    public static final StyleKey<FontUnderline> fontUnderline =
            register("fontUnderline", XSSFFont.class, FontUnderline.class, XSSFFont::setUnderline);
    public static final StyleKey<Boolean> fontBold =
            register("fontBold", XSSFFont.class, Boolean.class, XSSFFont::setBold);
    public static final StyleKey<FontFamily> fontFamily =
            register("fontFamily", XSSFFont.class, FontFamily.class, XSSFFont::setFamily);

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
        return (StyleKey<T>) styleMap.get(key);
    }


    private static <T> StyleKey<T> register(StyleKey<T> key) {
        styleMap.put(key.getId(), key);
        return key;
    }


    private static <T, S> StyleKey<S> register(String id, Class<T> targetType, Class<S> styleType, BiConsumer<T, S> applier) {
        return register(new StyleKey<>(id, targetType, styleType, applier));
    }


    public static CellStyle createCellStyle(Workbook workbook, StyleMap cellStyleMap) {
        CellStyle cellStyle = workbook.createCellStyle();

        for (StyleKey<?> key : styleMap.values()) {
            if (key.isSupportApplyTarget(cellStyle)) {
                cellStyleMap.getStyle(key).ifPresent(style -> key.applyStyle(cellStyle, style));
            }
        }
        return cellStyle;
    }

    public static Font createFont(Workbook workbook, StyleMap fontStyleMap) {
        Font font = workbook.createFont();

        for (StyleKey<?> key : styleMap.values()) {
            if (key.isSupportApplyTarget(font)) {
                fontStyleMap.getStyle(key).ifPresent(style -> key.applyStyle(font, style));
            }
        }
        return font;
    }
    public static <T> void setDefaultStyle(StyleKey<T> key, T defaultValue) {
        DEFAULT_STYLE.addStyle(key, defaultValue);
    }


    public static StyleMap filterStyle(StyleMap map, Class<?> targetType) {
        final List<StyleKey<?>> filterStyles = styleMap.values().stream().filter(
                key -> key.isSupportApplyTarget(targetType)
        ).collect(Collectors.toList());
        return map.getStyleMap(filterStyles);
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
