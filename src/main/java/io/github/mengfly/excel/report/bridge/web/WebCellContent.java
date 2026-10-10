package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

/**
 * 单元格内容的基类。
 * <p>
 * 内容不是单元格：它只描述"这个格子里装了什么"，不含行列/跨度/样式（这些属于 {@link WebCell}）。
 * 具体种类由 {@link #getKind()} 作为稳定判别标识（text / image / chart），供前端分发。
 *
 * @author Mengfly
 */
@Getter
@Setter
public abstract class WebCellContent {

    /**
     * 内容种类：text / image / chart。
     */
    private String kind;
}