package io.github.mengfly.excel.report.excel;

import lombok.Getter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.SheetStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;


@Getter
public class ExcelReport {
    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final Map<String, Integer> sheetNameSequence = new HashMap<>();

    public void save(File file) throws IOException {
        try (OutputStream stream = Files.newOutputStream(file.toPath())) {
            workbook.write(stream);
        }
    }

    public void exportSheet(String name, Container container, StyleMap sheetStyle) {
        XSSFSheet sheet = getSheet(name);
        StyleMap sheetStyleMap = SheetStyles.DEFAULT_STYLE.createChildStyleMap(sheetStyle);
        SheetStyles.initSheetStyle(sheet, sheetStyleMap);
        ReportContext context = new ReportContext(workbook, sheet);
        context.getStyleChain().onStyle(CellStyles.DEFAULT_STYLE, () -> container.export(context, new Point(0, 0)));
        context.applyCellWidthHeight(sheetStyleMap);


    }


    public void exportTemplate(ReportTemplate template, String name, DataContext context) {
        Container container = template.render(context);
        exportSheet(name, container, template.getSheetStyle());
    }

    private XSSFSheet getSheet(String name) {
        String sheetName = name;
        final int seq = sheetNameSequence.computeIfAbsent(name, s -> 0);
        if (seq > 0) {
            sheetName = name + "_" + seq;
        }
        final XSSFSheet sheet = workbook.createSheet(sheetName);
        sheetNameSequence.put(sheetName, seq + 1);
        return sheet;
    }


}
