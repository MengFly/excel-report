package io.github.mengfly.excel.report.entity;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Border {

    public static final Border EMPTY = Border.builder().build();

    private final int top;
    private final int left;
    private final int right;
    private final int bottom;

    public static Border of(int top, int bottom, int left, int right) {
        return Border.builder()
                .top(top)
                .left(left)
                .right(right)
                .bottom(bottom)
                .build();
    }

    public static Border of(String str) {
        if (StrUtil.isEmpty(str)) {
            return EMPTY;
        }
        final String[] split = str.split(",");
        if (split.length == 0) {
            return EMPTY;
        }
        int top;
        int bottom;
        int left;
        int right;

        if (split.length == 1) {
            final Integer n1 = Convert.toInt(split[0], 0);
            top = n1;
            bottom = n1;
            left = n1;
            right = n1;
        } else if (split.length == 2) {
            final Integer n1 = Convert.toInt(split[0], 0);
            final Integer n2 = Convert.toInt(split[1], 0);
            top = n1;
            bottom = n1;
            left = n2;
            right = n2;
        } else if (split.length == 3) {
            final Integer n1 = Convert.toInt(split[0], 0);
            final Integer n2 = Convert.toInt(split[1], 0);
            final Integer n3 = Convert.toInt(split[2], 0);
            top = n1;
            bottom = n2;
            left = n3;
            right = n3;
        } else {
            final Integer n1 = Convert.toInt(split[0], 0);
            final Integer n2 = Convert.toInt(split[1], 0);
            final Integer n3 = Convert.toInt(split[2], 0);
            final Integer n4 = Convert.toInt(split[3], 0);
            top = n1;
            bottom = n2;
            left = n3;
            right = n4;
        }
        return Border.builder()
                .top(top)
                .left(left)
                .right(right)
                .bottom(bottom)
                .build();

    }


}
