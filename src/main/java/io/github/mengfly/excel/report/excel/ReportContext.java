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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ReportContext {

    @Getter
    private final Workbook workbook;
    @Getter
    private final XSSFSheet sheet;
    private final Map<StyleMap, CellStyle> cellStylePool = new HashMap<>();
    private final Map<StyleMap, Font> fontPool = new HashMap<>();
    @Getter
    private final StyleChain styleChain = new StyleChain();
    private final Map<Integer, Double> autoWidthColumn = new HashMap<>();
    private final Map<Integer, Double> autoHeightRow = new HashMap<>();
    private XSSFDataFormat format;
    private final List<Runnable> onExportFinalizer = new ArrayList<>();

    public ExcelCellSpan getCellSpan(Point point, Size size, StyleMap cellStyle) {
        final ExcelCellSpan cellSpan = new ExcelCellSpan(this, point, size);
        cellSpan.setCellAutoWidth(autoWidthColumn);
        cellSpan.setCellAutoHeight(autoHeightRow);
        cellSpan.setStyle(getCellStyle(cellStyle), cellStyle);
        return cellSpan;
    }

    public ExcelCellSpan getCellSpan(Point point, Size size) {
        return getCellSpan(point, size, styleChain.getStyle());
    }

    /**
     * 根据样式表获取Cell样式
     * @param map 样式表
     * @return Cell样式
     */
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
            map.getStyle(CellStyles.dataFormat).ifPresent(dataFormat -> {
                if (format == null) {
                    format = ((XSSFDataFormat) workbook.createDataFormat());
                }
                final short index = format.getFormat(dataFormat);
                cellStyle.setDataFormat(index);
            });
            cellStylePool.put(map, cellStyle);
            return cellStyle;
        }
    }

    /**
     * 根据样式表获取字体
     * @param map 样式表
     * @return 字体
     */
    public Font getFont(StyleMap map) {
        if (map == null) {
            return null;
        }
        final StyleMap fontStyle = CellStyles.toFontStyle(map);
        if (fontStyle.isEmpty()) {
            return null;
        }
        return fontPool.computeIfAbsent(fontStyle, styleMap -> CellStyles.createFont(workbook, styleMap));
    }

    public XSSFDrawing createDrawingPatriarch() {
        return sheet.createDrawingPatriarch();
    }

    /**
     * 添加图片
     * @param image 图片
     * @return 图片的索引
     * @throws IOException 如果图片读取失败，抛出异常
     */
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

        if (!autoHeightRow.isEmpty()) {
            // 调整大小
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

        onExportFinalizer.forEach(runnable -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("onExportFinalizer fail", e);
            }
        });


    }

    /**
     * 添加导出时的回调,该回调函数会在导出Excel的最后执行
     * @param runnable 回调
     */
    public void addOnExportFinalizer(Runnable runnable) {
        onExportFinalizer.add(runnable);
    }
}
