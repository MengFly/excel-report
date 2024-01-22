package io.github.mengfly.excel.report.exception;

/**
 * Base Exception
 */
public class ExcelReportException extends RuntimeException{

    public ExcelReportException(String message) {
        super(message);
    }

    public ExcelReportException(Throwable cause) {
        super(cause);
    }

    public ExcelReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
