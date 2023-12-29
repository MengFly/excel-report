package io.github.mengfly.excel.report.excel;

import cn.hutool.core.io.IoUtil;
import io.github.mengfly.excel.report.component.image.Image;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.SheetStyles;
import io.github.mengfly.excel.report.style.StyleChain;
import io.github.mengfly.excel.report.style.StyleMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


@RequiredArgsConstructor
public class ReportContext {

    private final Workbook workbook;
    @Getter
    private final XSSFSheet sheet;
    private final Map<StyleMap, CellStyle> cellStylePool = new HashMap<>();
    private final Map<StyleMap, Font> fontPool = new HashMap<>();
    @Getter
    private final StyleChain styleChain = new StyleChain();
    private final Map<Integer, Double> autoWidthColumn = new HashMap<>();
    private final Map<Integer, Double> autoHeightRow = new HashMap<>();

    public ExcelCellSpan getCellSpan(Point point, Size size, StyleMap cellStyle) {
        final ExcelCellSpan cellSpan = new ExcelCellSpan(sheet, point, size);
        cellSpan.setStyle(getCellStyle(cellStyle), cellStyle);
        cellSpan.setCellAutoWidth(autoWidthColumn);
        cellSpan.setCellAutoHeight(autoHeightRow);
        return cellSpan;
    }

    public ExcelCellSpan getCellSpan(Point point, Size size) {
        return getCellSpan(point, size, styleChain.getStyle());
    }

    private CellStyle getCellStyle(StyleMap map) {
        if (map == null) {
            return null;
        }
        if (cellStylePool.containsKey(map)) {
            return cellStylePool.get(map);
        } else {
            final CellStyle cellStyle = CellStyles.createCellStyle(workbook, map);

            final Font font = getFont(map);
            if (font != null) {
                cellStyle.setFont(font);
            }
            cellStylePool.put(map, cellStyle);
            return cellStyle;
        }
    }

    private Font getFont(StyleMap map) {
        if (map == null) {
            return null;
        }
        return fontPool.computeIfAbsent(CellStyles.toFontStyle(map), styleMap -> CellStyles.createFont(workbook, styleMap));
    }

    public XSSFDrawing createDrawingPatriarch() {
        return (XSSFDrawing) sheet.createDrawingPatriarch();
    }

    public int addPicture(Image image) throws IOException {
        try (InputStream stream = image.openStream()) {
            return workbook.addPicture(IoUtil.readBytes(stream), image.deduceExcelImageType());
        }
    }

    public StyleMap getCurrentChildStyle(StyleMap style) {
        return styleChain.getStyle(style);
    }

    public void applyCellWidthHeight(StyleMap sheetStyle) {
        autoWidthColumn.forEach((column, width) -> sheet.setColumnWidth(column, (int) (width * 256)));

        autoHeightRow.forEach((row, height) -> sheet.getRow(row).setHeightInPoints(height.floatValue()));

        if (autoHeightRow.isEmpty()) {
            return;
        }
        sheetStyle.getStyle(SheetStyles.defaultRowHeight).ifPresent(height -> {
            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                final Row row = sheet.getRow(i);
                if (row != null) {
                    if (!autoHeightRow.containsKey(i)) {
                        row.setHeightInPoints(height);
                    }
                }
            }
        });

    }
}
