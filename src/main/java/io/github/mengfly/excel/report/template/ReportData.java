package io.github.mengfly.excel.report.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


@Target(ElementType.TYPE)
public @interface ReportData {

    String template();
}
