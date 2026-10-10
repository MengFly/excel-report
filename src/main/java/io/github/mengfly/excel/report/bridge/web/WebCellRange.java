package io.github.mengfly.excel.report.bridge.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 一个单元格区域引用（行/列下标从 0 起，含首尾）。
 * <p>
 * 用于图表引用工作表单元格范围作为数据源时，前端据此从 {@link WebExcel#getCells()} 取值。
 *
 * @author Mengfly
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebCellRange {

    private int firstRow;
    private int lastRow;
    private int firstCol;
    private int lastCol;
}