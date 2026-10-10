package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 图表的一条数据系列。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebChartSeries {

    /**
     * 系列名称。
     */
    private String title;

    /**
     * 该系列的绘图类型（ChartTypes 枚举名小写，如 line / bar / area / scatter / pie）。
     */
    private String chartType;

    /**
     * 系列颜色（原始写法，如 rgb 串或颜色名）。
     */
    private String color;

    /**
     * 折线/散点是否平滑。
     */
    private Boolean smooth;

    /**
     * 直接给定的数据点；与 {@link #reference} 二选一。
     */
    private List<Object> values;

    /**
     * 引用工作表单元格区域的数据源；与 {@link #values} 二选一。
     */
    private WebCellRange reference;
}