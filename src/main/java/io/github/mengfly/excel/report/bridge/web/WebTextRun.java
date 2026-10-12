package io.github.mengfly.excel.report.bridge.web;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 富文本的一个分片：一段文字及其归一化后的样式。
 *
 * @author Mengfly
 */
@Getter
@Setter
public class WebTextRun {

    /**
     * 分片文字。
     */
    private String text;

    /**
     * 该分片的归一化样式（样式 key -> 归一化值），可为空 Map。
     */
    private Map<String, Object> style = new LinkedHashMap<>();
}