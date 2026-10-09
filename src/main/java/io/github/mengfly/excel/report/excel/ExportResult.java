package io.github.mengfly.excel.report.excel;


import io.github.mengfly.excel.report.Container;
import lombok.Getter;

@Getter
public class ExportResult {

    private final Container container;
    private final ReportContext context;

    ExportResult(Container container, ReportContext context) {
        this.container = container;
        this.context = context;
    }
}
