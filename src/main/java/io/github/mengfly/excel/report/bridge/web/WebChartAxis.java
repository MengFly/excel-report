package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 图表的一个坐标轴。
 * <p>
 * 分类轴（labelAxis）用 {@link #values} / {@link #reference} 承载分类数据；
 * 数值轴（valueAxes）用 {@link #series} 承载各条数据系列。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebChartAxis {

    /**
     * 轴标题。
     */
    private String title;

    /**
     * 数值格式。
     */
    private String numberFormat;

    /**
     * 轴类型（AxisType 枚举名小写：category / value / date）。
     */
    private String axisType;

    /**
     * 轴自身的数据（分类轴用）。
     */
    private List<Object> values;

    /**
     * 轴自身引用的单元格区域（分类轴用）。
     */
    private WebCellRange reference;

    /**
     * 数值轴的数据系列。
     */
    private List<WebChartSeries> series = new ArrayList<>();
}