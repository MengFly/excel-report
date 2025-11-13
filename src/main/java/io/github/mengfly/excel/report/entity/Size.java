package io.github.mengfly.excel.report.entity;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class Size {
    public int width;
    public int height;

    public static Size of(int width, int height) {
        return new Size(width, height);
    }

    public static Size of(String size) {
        if (size == null) {
            return null;
        } else {
            String[] split = size.split(",");
            if (split.length != 2) {
                throw new IllegalArgumentException("size must be width,height");
            }
            try {
                int width = Convert.toInt(split[0].trim());
                int height = Convert.toInt(split[1].trim());
                return new Size(width, height);
            } catch (Exception e) {
                throw new IllegalArgumentException("size must be width,height");
            }
        }
    }

    @Override
    public String toString() {
        return width + "," + height;
    }
}
