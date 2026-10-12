package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图表单元格的内容，携带前端重绘所需的类型、坐标轴与实际数据。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebChart extends WebCellContent {

    /**
     * 该图表支持使用的图表类型（ChartTypes 枚举名小写）。
     */
    private List<String> chartTypes = new ArrayList<>();

    /**
     * 图表标题文本。
     */
    private String title;

    /**
     * 图表标题是否覆盖在图上。
     */
    private Boolean titleOverlay;

    /**
     * 图例配置（position / overlay）。
     */
    private Map<String, Object> legend = new LinkedHashMap<>();

    /**
     * 数据标签配置（showVal / showPercent / showSerName 等）。
     */
    private Map<String, Object> marker = new LinkedHashMap<>();

    /**
     * 分类轴。
     */
    private WebChartAxis labelAxis;

    /**
     * 数值轴（默认类型图表最多两条：左/右轴）。
     */
    private List<WebChartAxis> valueAxes = new ArrayList<>();

    public WebChart() {
        setKind("chart");
    }
}