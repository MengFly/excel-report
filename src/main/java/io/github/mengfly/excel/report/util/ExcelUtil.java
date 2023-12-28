package io.github.mengfly.excel.report.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

public class ExcelUtil {

    public static Row getRow(Sheet sheet, int row) {
        final Row r = sheet.getRow(row);
        if (r == null) {
            return sheet.createRow(row);
        } else {
            return r;
        }
    }

    public static Cell createOrGetCell(Row row, int col) {
        final Cell cell = row.getCell(col);
        if (cell == null) {
            return row.createCell(col);
        }
        return cell;
    }


    public static void merge(Sheet sheet, int startRow, int endRow, int startCol, int endCol) {
        CellRangeAddress region = new CellRangeAddress(startRow, endRow, startCol, endCol);
        sheet.addMergedRegion(region);
    }
}
