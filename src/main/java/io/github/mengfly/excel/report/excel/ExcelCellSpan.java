package io.github.mengfly.excel.report.excel;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.cell.CellUtil;
import io.github.mengfly.excel.report.component.text.RichText;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.util.ExcelUtil;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.util.Dimension2DDouble;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.awt.geom.Dimension2D;
import java.util.Map;
import java.util.Optional;


public class ExcelCellSpan {

    private final Point point;
    private final Size size;
    private final ReportContext context;

    private StyleMap styleMap;

    @Setter
    private Map<Integer, Double> cellAutoWidth;
    @Setter
    private Map<Integer, Double> cellAutoHeight;

    public ExcelCellSpan(ReportContext context, Point point, Size size) {
        this.context = context;
        this.point = point;
        this.size = size;
        for (int i = 0; i < size.height; i++) {
            final Row row = ExcelUtil.getRow(context.getSheet(), point.getY() + i);
            for (int j = 0; j < size.width; j++) {
                ExcelUtil.createOrGetCell(row, point.getX() + j);
            }
        }
    }

    public XSSFSheet getSheet() {
        return context.getSheet();
    }

    public ExcelCellSpan merge() {
        if (size.width <= 1 && size.height <= 1) {
            return this;
        }
        ExcelUtil.merge(getSheet(), point.getY(), point.getY() + size.height - 1, point.getX(), point.getX() + size.width - 1);
        return this;
    }

    public void setValue(Object value) {
        if (size.width <= 0 || size.height <= 0) {
            return;
        }
        if (value == null) {
            value = "";
        }

        // RichText String
        if (value instanceof String && RichText.isRichText(((String) value))) {
            value = RichText.parse(((String) value));
        }

        // RichText
        if (value instanceof RichText) {
            if (((RichText) value).isEmpty()) {
                value = "";
            } else {
                value = ((RichText) value).toRichTextString(context, styleMap);
            }
        }

        final Cell orGetCell = ExcelUtil.createOrGetCell(
                ExcelUtil.getRow(getSheet(), point.getY()), point.getX());

        CellUtil.setCellValue(orGetCell, value);

        calculateAndRecordCellWidthHeight();

    }

    public void setHyperLink(Hyperlink hyperlink) {
        final Cell orGetCell = ExcelUtil.createOrGetCell(ExcelUtil.getRow(getSheet(), point.getY()), point.getX());
        orGetCell.setHyperlink(hyperlink);
    }

    public void calculateAndRecordCellWidthHeight() {
        if (styleMap != null) {
            final Optional<String> widthStyle = styleMap.getStyle(CellStyles.width);
            if (widthStyle.isPresent() && cellAutoWidth != null) {
                for (int i = 0; i < size.width; i++) {
                    int col = point.getX() + i;
                    Double columnWidth;
                    if (StrUtil.equalsAnyIgnoreCase("auto", widthStyle.get())) {
                        columnWidth = SheetUtil.getColumnWidth(
                                getSheet(), col, true, point.getY(), point.getY() + size.height - 1) + 4;
                    } else {
                        columnWidth = Convert.toDouble(widthStyle.get(), null);
                    }
                    if (columnWidth != null) {
                        cellAutoWidth.compute(col, (integer, preValue) -> Math.max(preValue == null ? 0 : preValue, columnWidth));
                    }
                }
            }
            final Optional<String> style = styleMap.getStyle(CellStyles.height);
            if (style.isPresent() && cellAutoHeight != null) {
                for (int i = 0; i < size.height; i++) {
                    int row = point.getY() + i;
                    Double columnHeight = Convert.toDouble(style.get(), null);
                    if (columnHeight != null && columnHeight > 0) {
                        cellAutoHeight.compute(row, (integer, preValue) -> Math.max(preValue == null ? 0 : preValue, columnHeight + 2));
                    }
                }
            }
        }

    }


    public void setStyle(CellStyle style, StyleMap styleMap) {
        if (style != null) {
            for (int i = 0; i < size.height; i++) {
                final Row row = ExcelUtil.getRow(getSheet(), point.getY() + i);
                for (int j = 0; j < size.width; j++) {
                    ExcelUtil.createOrGetCell(row, point.getX() + j).setCellStyle(style);
                }
            }
        }
        this.styleMap = styleMap;
        calculateAndRecordCellWidthHeight();
    }

    public ClientAnchor getFillAnchor(ClientAnchor.AnchorType anchorType) {
        final XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, point.getX(), point.getY(), point.getX() + size.width, point.getY() + size.height);
        if (anchorType != null) {
            anchor.setAnchorType(anchorType);
        }
        return anchor;
    }

    public Dimension2D cellSpanDimension() {
        double widthPixel = 0;
        double heightPixel = 0;

        for (int i = 0; i < size.width; i++) {
            widthPixel += getColumnWidthInPixels(point.getX() + i);
        }

        for (int i = 0; i < size.height; i++) {
            heightPixel += getRowHeightInPoints(point.getY() + i);
        }
        return new Dimension2DDouble(widthPixel, heightPixel);
    }

    public double getColumnWidthInPixels(int col) {
        return getSheet().getColumnWidthInPixels(col);
    }

    public double getRowHeightInPoints(int row) {
        return getSheet().getRow(row).getHeightInPoints();
    }

}
