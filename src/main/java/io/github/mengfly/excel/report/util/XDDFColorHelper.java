package io.github.mengfly.excel.report.util;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;

public class XDDFColorHelper {

    public static XDDFColor createColor(String color, PresetColor defaultColor) {
        if (StrUtil.isEmpty(color)) {
            return XDDFColor.from(defaultColor);
        }
        if (color.startsWith("#")) {
            byte[] colorArray = getColorArray(color);
            return XDDFColor.from(colorArray);
        } else {
            return XDDFColor.from(getDefinitionColor(color, defaultColor));
        }
    }

    private static PresetColor getDefinitionColor(String colorStr, PresetColor defaultColor) {
        PresetColor color = defaultColor;
        try {
            color = EnumUtil.fromString(PresetColor.class, colorStr.trim().toUpperCase());
        } catch (Exception ignored) {
        }
        return color;
    }

    private static byte[] getColorArray(String property) {
        try {
            return HexUtil.decodeHex(property.substring(1));
        } catch (Exception e) {
            return null;
        }
    }


}
