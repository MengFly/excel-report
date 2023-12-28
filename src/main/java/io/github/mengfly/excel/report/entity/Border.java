package io.github.mengfly.excel.report.entity;

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

}
