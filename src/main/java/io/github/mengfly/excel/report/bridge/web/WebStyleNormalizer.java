package io.github.mengfly.excel.report.bridge.web;

import cn.hutool.core.util.HexUtil;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.SheetStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.style.key.StyleKey;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 把框架样式表（样式 key -> 字符串值）归一化为前端友好形式。
 * <p>
 * 只做“值归一化”，保留原始样式 key 名：颜色 -> {@code #RRGGBB}、枚举 -> 小写名、
 * 布尔/数字 -> 原生类型；未注册或转换失败的 key 保留原始字符串。
 */
class WebStyleNormalizer {

    private WebStyleNormalizer() {
    }

    /**
     * 归一化整张样式表。
     *
     * @param styleMap 待归一化的样式表，可为 null
     * @return key -> 归一化值 的映射；入参为 null / 空时返回空 Map
     */
    static Map<String, Object> normalize(StyleMap styleMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (styleMap == null || styleMap.isEmpty()) {
            return result;
        }
        styleMap.getStyleMap().forEach((id, raw) -> result.put(id, normalizeValue(id, raw)));
        return result;
    }

    private static Object normalizeValue(String id, String raw) {
        // 用注册表反查 StyleKey，才能把字符串还原成强类型值再归一化；查不到则原样保留。
        // 单元格样式与 Sheet 样式分属两个独立注册表，需依次查（两表 key 无重名）。
        StyleKey<Object> key = CellStyles.getStyleKey(id);
        if (key == null) {
            key = SheetStyles.getStyleKey(id);
        }
        if (key == null) {
            return raw;
        }
        final Object typed;
        try {
            typed = key.getStyle(raw);
        } catch (Exception e) {
            return raw;
        }
        return normalizeTyped(typed, raw);
    }

    private static Object normalizeTyped(Object typed, String raw) {
        if (typed == null) {
            return raw;
        }
        if (typed instanceof XSSFColor) {
            final byte[] rgb = ((XSSFColor) typed).getRGB();
            return rgb == null ? raw : "#" + HexUtil.encodeHexStr(rgb);
        }
        if (typed instanceof Enum) {
            return ((Enum<?>) typed).name().toLowerCase();
        }
        if (typed instanceof Boolean || typed instanceof Number) {
            return typed;
        }
        return String.valueOf(typed);
    }
}