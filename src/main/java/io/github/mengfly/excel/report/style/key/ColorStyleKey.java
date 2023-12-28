package io.github.mengfly.excel.report.style.key;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.HexUtil;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.xssf.usermodel.XSSFColor;

public class ColorStyleKey extends StyleKey<XSSFColor> {
    private static final HSSFColor.HSSFColorPredefined defaultColor = HSSFColor.HSSFColorPredefined.BLACK;

    public ColorStyleKey(String id, String methodName) {
        super(id, methodName, XSSFColor.class);
    }

    @Override
    public XSSFColor getStyle(String property) {
        byte[] colorArray;
        if (property == null) {
            colorArray = toByteArray(defaultColor);
        } else {
            if (property.startsWith("#")) {
                colorArray = getColorArray(property);
            } else {
                colorArray = getDefinitionColor(property);
            }
        }

        return new XSSFColor(colorArray);
    }


    @Override
    public String toString(XSSFColor property) {
        return "#" + HexUtil.encodeHexStr(property.getRGB());
    }

    private byte[] getDefinitionColor(String property) {
        HSSFColor.HSSFColorPredefined color = defaultColor;
        try {
            color = EnumUtil.fromString(HSSFColor.HSSFColorPredefined.class, property.trim().toUpperCase());
        } catch (Exception ignored) {
        }
        return toByteArray(color);
    }

    private byte[] getColorArray(String property) {
        try {

            return HexUtil.decodeHex(property.substring(1));
        } catch (Exception e) {
            return toByteArray(defaultColor);
        }
    }

    private byte[] toByteArray(HSSFColor.HSSFColorPredefined colorPredefined) {
        final short[] triplet = colorPredefined.getTriplet();
        byte[] bytes = new byte[triplet.length];
        for (int i = 0; i < triplet.length; i++) {
            bytes[i] = (byte) triplet[i];
        }
        return bytes;
    }


}
