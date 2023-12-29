package io.github.mengfly.excel.report;

import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.layout.Layout;
import io.github.mengfly.excel.report.report.*;
import io.github.mengfly.excel.report.style.SheetStyles;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TestReport {

    private static ExcelReport report;

    @BeforeClass
    public static void before() {
        report = new ExcelReport();
    }

    @Test
    public void testLayout() {
        exportSheet(new TestLayoutReport());
        exportSheet(new TestTableReport());
        exportSheet(new TestListReport());
        exportSheet(new TestImage());
        exportSheet(new TestChart());
    }

    public void exportSheet(Layout layout) {
        System.out.println("Preparing export sheet");
        System.out.println(layout.print());
        report.exportSheet(layout.getClass().getSimpleName(), layout, SheetStyles.DEFAULT_STYLE);
        System.out.println();
    }

    @AfterClass
    public static void after() throws IOException {
        report.save(new File("test-report.xlsx"));
    }

}
