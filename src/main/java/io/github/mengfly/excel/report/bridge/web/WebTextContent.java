package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本内容（文本 / 链接 / List 单元格等）。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebTextContent extends WebCellContent {

    /**
     * 文本值（普通文本取原值；富文本取其纯文本拼接）。
     */
    private Object value;

    /**
     * 超链接地址（仅链接组件），可为 null。
     */
    private String link;

    /**
     * 富文本分片；仅有富文本时非空，此时 {@link #value} 为其纯文本拼接。
     */
    private List<WebTextRun> richText = new ArrayList<>();

    public WebTextContent() {
        setKind("text");
    }
}