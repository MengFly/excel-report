package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一张可由前端回显的 Excel 工作表结构（扁平单元格网格）。
 * <p>
 * 由 {@link WebJsonExporter} 从 Container 树的 position / measuredSize / finalStyle 反推得到；
 * 工作表名称取自导出 Sheet 的名称。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebExcel {

    /**
     * 工作表名称，取自导出时 Sheet 的名称；导出结果为空时为空。
     */
    private String sheetName;

    /**
     * 网格覆盖的总行数，行下标从 0 起。
     */
    private int rowCount;

    /**
     * 网格覆盖的总列数，列下标从 0 起。
     */
    private int colCount;

    /**
     * 归一化后的 Sheet 级样式（样式 key -> 归一化值），来源为 ReportContext#getSheetStyle。
     */
    private Map<String, Object> sheetStyle = new LinkedHashMap<>();

    /**
     * 扁平单元格列表，按 (row, col) 升序排列。
     */
    private List<WebCell> cells = new ArrayList<>();

    /**
     * 列宽：列号 -> 列宽（单位：字符数）。仅包含被显式设置过宽度的列（含 auto 的实测结果）。
     */
    private Map<Integer, Double> columnWidths = new LinkedHashMap<>();

    /**
     * 行高：行号 -> 行高（单位：磅）。仅包含被显式设置过高度的行。
     */
    private Map<Integer, Double> rowHeights = new LinkedHashMap<>();
}