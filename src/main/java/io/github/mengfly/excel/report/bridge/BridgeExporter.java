package io.github.mengfly.excel.report.bridge;

import io.github.mengfly.excel.report.excel.ExportResult;

public interface BridgeExporter<T> {


    T export(ExportResult export);


    /**
     * 导出结果 {@link ExportResult} 转换为可前端传输的 {@link T}。
     *
     * @param exporter 导出器
     * @param export   导出结果
     * @return 可前端传输的 {@link T}
     */
    static <T> T export(BridgeExporter<T> exporter, ExportResult export) {
        return exporter.export(export);
    }
}
