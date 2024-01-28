package io.github.mengfly.excel.report.exception;

public class TemplateNotFoundException extends ExcelReportException {

    public TemplateNotFoundException(String templateId) {
        super("Template not found : " + templateId);
    }
}
