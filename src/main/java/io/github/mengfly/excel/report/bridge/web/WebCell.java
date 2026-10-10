package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一个单元格（或合并区域）：只描述几何、组件类型、样式与内容。
 * <p>
 * 几何信息来自 Container 的 position / measuredSize，样式来自其 finalStyle，均不读取 POI；
 * 内容由 {@link #content} 承载（文本 / 图片 / 图表），内容种类见 {@link WebCellContent#getKind()}。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebCell {

    /**
     * 起始行下标（0 起）。
     */
    private int row;

    /**
     * 起始列下标（0 起）。
     */
    private int col;

    /**
     * 行跨度（合并行数），至少为 1。
     */
    private int rowSpan;

    /**
     * 列跨度（合并列数），至少为 1。
     */
    private int colSpan;

    /**
     * 组件类型：优先取模板标签名（如 Text / Image / Chart），无模板信息时取组件类简名。
     */
    private String type;

    /**
     * 归一化后的单元格样式：样式 key -> 归一化值。
     */
    private Map<String, Object> style = new LinkedHashMap<>();

    /**
     * 单元格内容；为 null 表示无内容（如 SpanComponent 只定义合并区域）。
     */
    private WebCellContent content;
}