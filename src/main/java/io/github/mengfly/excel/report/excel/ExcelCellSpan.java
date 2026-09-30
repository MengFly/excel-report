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


/**
 * 一个矩形单元格区域（Sheet 上 {@code point, size} 覆盖的范围）。
 * <p>
 * 区域内的行与 cell 在构造时无条件建好；{@link #setStyle(CellStyle, StyleMap)} 只负责刷样式。
 * <p>
 * 注意本类不保证"整块区域都有值"：{@link #setValue(Object)} 只写左上角一格，其余格子仅作为合并区的一部分存在。
 */
public class ExcelCellSpan {

    private final Point point;
    private final Size size;
    private final ReportContext context;

    private StyleMap styleMap;

    /**
     * 宽高是否已登记过（用于导出末尾的补算判定，见 {@link #calculateAutoSizeIfAbsent()}）
     */
    private boolean autoSizeCalculated = false;

    @Setter
    private Map<Integer, Double> cellAutoWidth;
    @Setter
    private Map<Integer, Double> cellAutoHeight;

    public ExcelCellSpan(ReportContext context, Point point, Size size) {
        this.context = context;
        this.point = point;
        this.size = size;
        // 这一遍遍历与 setStyle 的遍历逐格重复，是刻意保留的：它让"区域已建好"这个不变量与样式无关。
        // 曾把它合并进 setStyle（省掉一遍遍历），实测收益在噪声内（n=20 万 span，best-of-5：457ms vs 447/457ms），
        // 却让该不变量变成"依赖样式非 null"（漏了就 NPE，见 ExcelCellSpanTest）⇒ 判定为负收益，已回退。
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
        if (size.width <= 0 || size.height <= 0) {
            return this;
        }
        if (size.width == 1 && size.height == 1) {
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
        autoSizeCalculated = true;
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


    /**
     * 补算：仅供 {@link ReportContext#applyCellWidthHeight} 在导出末尾调用。
     * <p>
     * 只对"从未登记过宽高"的 span 执行一次 —— 例如 {@code SpanComponent} / {@code ChartComponent}
     * 只调 {@code merge()} 而从不 {@code setValue()}，它们的 cell 始终是空白。
     */
    void calculateAutoSizeIfAbsent() {
        if (!autoSizeCalculated) {
            // 结果与旧实现"在 setStyle 时机计算"一致（实测均为 -1 + 4），语义由上述契约保证
            calculateAndRecordCellWidthHeight();
        }
    }

    /**
     * 给本区域内的每个 cell 应用样式。
     *
     * @param style    要应用的 Cell 样式；为 null 时只记录 {@code styleMap}，不刷样式
     * @param styleMap 本区域生效的样式表，供后续富文本解析与宽高计算使用
     */
    public void setStyle(CellStyle style, StyleMap styleMap) {
        // 本区域的行 / cell 由构造器无条件建好（与样式无关），这里只负责刷样式 ——
        // 故意不把"建 cell"合并进来省一遍遍历：那会让该不变量变成依赖 style 非 null（详见构造器注释与 ExcelCellSpanTest）
        if (style != null) {
            for (int i = 0; i < size.height; i++) {
                final Row row = ExcelUtil.getRow(getSheet(), point.getY() + i);
                for (int j = 0; j < size.width; j++) {
                    ExcelUtil.createOrGetCell(row, point.getX() + j).setCellStyle(style);
                }
            }
        }
        this.styleMap = styleMap;
        // 此处不再计算宽高：此时 cell 还没有值、也可能尚未 merge()，算出来只会是无效值（POI 返回 -1 ⇒ +4 = 3），
        // 必然被后续 setValue 时的终态值覆盖。真正有效的一次计算在 setValue()，漏算的由导出末尾补算。
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
