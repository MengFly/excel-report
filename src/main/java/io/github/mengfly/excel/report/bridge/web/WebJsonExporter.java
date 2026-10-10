package io.github.mengfly.excel.report.bridge.web;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.bridge.BridgeExporter;
import io.github.mengfly.excel.report.component.chart.ChartComponent;
import io.github.mengfly.excel.report.component.chart.axis.ChartAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartLabelAxis;
import io.github.mengfly.excel.report.component.chart.axis.ChartValueAxis;
import io.github.mengfly.excel.report.component.chart.component.ChartMarker;
import io.github.mengfly.excel.report.component.chart.component.ChartTitle;
import io.github.mengfly.excel.report.component.chart.component.Legend;
import io.github.mengfly.excel.report.component.chart.data.ChartLabelAxisData;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.data.resolver.AxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.resolver.RelationAxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.resolver.ValueAxisDataResolver;
import io.github.mengfly.excel.report.component.chart.type.ChartDataType;
import io.github.mengfly.excel.report.component.chart.type.DefaultChartDataType;
import io.github.mengfly.excel.report.component.chart.type.PieChartType;
import io.github.mengfly.excel.report.component.image.Image;
import io.github.mengfly.excel.report.component.image.ImageComponent;
import io.github.mengfly.excel.report.component.list.ListComponent;
import io.github.mengfly.excel.report.component.list.ListHeader;
import io.github.mengfly.excel.report.component.text.LinkComponent;
import io.github.mengfly.excel.report.component.text.RichText;
import io.github.mengfly.excel.report.component.text.TextComponent;
import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExportResult;
import io.github.mengfly.excel.report.layout.Layout;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.InputStream;
import java.util.*;

/**
 * 把导出结果 {@link ExportResult} 转换为可前端传输的 {@link WebExcel}（扁平单元格网格），
 * 用于在前端回显匹配样式的 Excel。
 * <p>
 * 取值约定（全程不读取 Sheet 的 POI 属性）：
 * <ul>
 *     <li>几何：来自 Container 的 {@code position}（x=列, y=行）与 {@code measuredSize}（宽=列跨度, 高=行跨度）；</li>
 *     <li>样式：来自 Container 的 {@link Container#getFinalStyle()}（合并后完整样式）与
 *         {@code ReportContext#getSheetStyle()}，经 {@link WebStyleNormalizer} 值归一化；</li>
 *     <li>列宽/行高：来自 {@code ReportContext#getColumnWidths()} / {@code #getRowHeights()}（已算出的实测值）；</li>
 *     <li>类型：优先模板标签名，无模板信息时取组件类简名；</li>
 *     <li>图片：内嵌 base64 dataURL + 渲染参数；图表：结构化类型/坐标轴/数据；富文本：分片输出。</li>
 * </ul>
 * 需在导出（measure → layout → export）完成后调用，否则 {@code finalStyle} 与宽高均为空。
 *
 * @author Mengfly
 */
public final class WebJsonExporter implements BridgeExporter<WebExcel> {

    private WebJsonExporter() {
    }

    public static final WebJsonExporter instance = new WebJsonExporter();

    /**
     * 导出为结构化对象，工作表名缺省为空。
     *
     * @param result 导出结果
     * @return 扁平单元格网格结构；入参为 null 时返回空结构，不抛异常
     */
    public WebExcel export(ExportResult result) {
        final WebExcel webExcel = new WebExcel();
        if (result == null || result.getContainer() == null) {
            return webExcel;
        }
        webExcel.setSheetName(result.getContext().getSheet().getSheetName());
        webExcel.setSheetStyle(WebStyleNormalizer.normalize(result.getContext().getSheetStyle()));
        webExcel.setColumnWidths(new LinkedHashMap<>(result.getContext().getColumnWidths()));
        webExcel.setRowHeights(new LinkedHashMap<>(result.getContext().getRowHeights()));

        final List<WebCell> collected = new ArrayList<>();
        collect(result.getContainer(), collected);

        final List<WebCell> cells = dedupe(collected);
        cells.sort(Comparator.comparingInt(WebCell::getRow).thenComparingInt(WebCell::getCol));
        webExcel.setCells(cells);

        int rowCount = 0;
        int colCount = 0;
        for (WebCell cell : cells) {
            rowCount = Math.max(rowCount, cell.getRow() + cell.getRowSpan());
            colCount = Math.max(colCount, cell.getCol() + cell.getColSpan());
        }
        webExcel.setRowCount(rowCount);
        webExcel.setColCount(colCount);
        return webExcel;
    }

    /**
     * 遍历组件树收集单元格：布局递归子节点，List 特判展开，其余叶子组件各产出一个单元格。
     */
    private static void collect(Container container, List<WebCell> out) {
        // 布局容器自身不占单元格，递归其子组件即可（TableComponentNew 的子节点就是单元格 TextComponent）
        if (container instanceof Layout) {
            final List<Container> children = ((Layout) container).getContainers();
            if (children != null) {
                for (Container child : children) {
                    collect(child, out);
                }
            }
            return;
        }
        // ListComponent 不是 Layout 且无子 Container，其表头/数据单元格在 onExport 内联生成，必须在此重建
        if (container instanceof ListComponent) {
            expandList((ListComponent) container, out);
            return;
        }
        final WebCell cell = toCell(container);
        if (cell != null) {
            out.add(cell);
        }
    }

    private static WebCell toCell(Container container) {
        final Point position = container.getPosition();
        final Size size = container.getMeasuredSize();
        // 无尺寸的组件（如分页标记 PageRowSplit/PageColSplit，尺寸为 0）不占单元格
        if (position == null || size == null || size.width <= 0 || size.height <= 0) {
            return null;
        }
        final WebCell cell = new WebCell();
        cell.setRow(position.getY());
        cell.setCol(position.getX());
        cell.setRowSpan(size.height);
        cell.setColSpan(size.width);
        cell.setType(resolveType(container));
        cell.setStyle(WebStyleNormalizer.normalize(container.getFinalStyle()));
        cell.setContent(toContent(container));
        return cell;
    }

    /**
     * 组件 -> 单元格内容；SpanComponent 等只定义合并区域的组件返回 null。
     */
    private static WebCellContent toContent(Container container) {
        // LinkComponent 继承 TextComponent，必须先判 Link
        if (container instanceof LinkComponent) {
            final LinkComponent link = (LinkComponent) container;
            final WebTextContent content = textContent(link.getText());
            content.setLink(link.getLink());
            return content;
        }
        if (container instanceof TextComponent) {
            return textContent(((TextComponent) container).getText());
        }
        if (container instanceof ImageComponent) {
            return toImage((ImageComponent) container);
        }
        if (container instanceof ChartComponent) {
            return toChart((ChartComponent) container);
        }
        return null;
    }

    /**
     * 展开 ListComponent 的表头与数据单元格，坐标规则与 {@link ListComponent#onExport} 保持一致。
     */
    private static void expandList(ListComponent list, List<WebCell> out) {
        final Point position = list.getPosition();
        if (position == null) {
            return;
        }
        final int span = list.getSpan();
        final ListHeader header = list.getHeader();
        final List<?> dataList = list.getDataList() == null ? Collections.emptyList() : list.getDataList();
        final Map<String, Object> baseStyle = WebStyleNormalizer.normalize(list.getFinalStyle());
        // List 单元格类型同样遵循"模板标签名优先、否则类简名"的规则
        final String type = resolveType(list);

        // 坐标与跨度严格对齐 ListComponent#onExport 里 getCellSpan(position, Size.of(...)) 的写法：Size 为 (宽=列跨度, 高=行跨度)
        if (list.getOrientation() == Orientation.VERTICAL) {
            int startRow = 0;
            if (header != null) {
                // 纵向表头在 ListComponent#exportVerticalList 里未额外叠加 header.getStyle()，此处保持一致
                out.add(listCell(position.getY(), position.getX(), header.getSpan(), span,
                        header.getTitle(), baseStyle, type));
                startRow += header.getSpan();
            }
            for (Object data : dataList) {
                out.add(listCell(position.getY() + startRow, position.getX(), 1, span,
                        data, baseStyle, type));
                startRow += 1;
            }
        } else {
            int startCol = 0;
            if (header != null) {
                // 横向表头在 ListComponent#exportHorizontalList 里叠加了 header.getStyle()
                out.add(listCell(position.getY(), position.getX(), span, header.getSpan(),
                        header.getTitle(), mergeStyle(baseStyle, header.getStyle()), type));
                startCol += header.getSpan();
            }
            for (Object data : dataList) {
                out.add(listCell(position.getY(), position.getX() + startCol, span, 1,
                        data, baseStyle, type));
                startCol += 1;
            }
        }
    }

    private static WebCell listCell(int row, int col, int rowSpan, int colSpan, Object value,
                                    Map<String, Object> style, String type) {
        final WebCell cell = new WebCell();
        cell.setRow(row);
        cell.setCol(col);
        cell.setRowSpan(rowSpan);
        cell.setColSpan(colSpan);
        cell.setType(type);
        cell.setContent(textContent(value));
        // 每个单元格持有独立的样式 Map，避免去重合并时相互污染
        cell.setStyle(new LinkedHashMap<>(style));
        return cell;
    }

    private static Map<String, Object> mergeStyle(Map<String, Object> baseStyle, StyleMap headerStyle) {
        final Map<String, Object> merged = new LinkedHashMap<>(baseStyle);
        merged.putAll(WebStyleNormalizer.normalize(headerStyle));
        return merged;
    }

    /**
     * 构造文本内容；富文本（RichText 实例或 {@code <html>} 字符串）拆成分片，value 存其纯文本拼接。
     */
    private static WebTextContent textContent(Object text) {
        final WebTextContent content = new WebTextContent();
        Object value = text;
        if (value instanceof String && RichText.isRichText((String) value)) {
            value = RichText.parse((String) value);
        }
        if (!(value instanceof RichText)) {
            content.setValue(value);
            return content;
        }
        final RichText richText = (RichText) value;
        final List<WebTextRun> runs = new ArrayList<>();
        final StringBuilder plain = new StringBuilder();
        for (RichText.RichTextItem item : richText.getItems()) {
            final WebTextRun run = new WebTextRun();
            run.setText(item.getText());
            run.setStyle(WebStyleNormalizer.normalize(item.getStyle()));
            runs.add(run);
            plain.append(item.getText());
        }
        content.setValue(plain.toString());
        content.setRichText(runs);
        return content;
    }

    private static WebImage toImage(ImageComponent component) {
        final WebImage image = new WebImage();
        image.setDataUrl(toDataUrl(component.getImage()));
        image.setScaleType(enumName(component.getScaleType()));
        image.setScaleHeight(component.getScaleHeight());
        image.setPadding(component.getPadding());
        image.setAnchorType(enumName(component.getAnchorType()));
        return image;
    }

    private static WebChart toChart(ChartComponent component) {
        final WebChart chart = new WebChart();
        final ChartDataType type = component.getType();
        chart.setChartTypes(enumNames(type.supportChartTypes()));

        final ChartTitle title = component.getChartTitle();
        if (title != null) {
            chart.setTitle(title.getText());
            chart.setTitleOverlay(title.isOverLay());
        }
        chart.setLegend(legendMap(component.getLegend()));

        // 数据标签：组件自身未设时回退到图表类型自带的 marker
        ChartMarker marker = component.getMarker();
        final List<WebChartAxis> valueAxes = new ArrayList<>();
        if (type instanceof PieChartType) {
            final PieChartType pie = (PieChartType) type;
            chart.setLabelAxis(toLabelAxis(pie.getLabelAxis()));
            valueAxes.add(toValueAxis(pie.getValueAxis()));
            if (marker == null) {
                marker = pie.getMarker();
            }
        } else if (type instanceof DefaultChartDataType) {
            final DefaultChartDataType data = (DefaultChartDataType) type;
            chart.setLabelAxis(toLabelAxis(data.getLabelAxis()));
            valueAxes.add(toValueAxis(data.getValueAxis1()));
            valueAxes.add(toValueAxis(data.getValueAxis2()));
            if (marker == null) {
                marker = data.getMarker();
            }
        }
        chart.setMarker(markerMap(marker));
        valueAxes.removeIf(Objects::isNull);
        chart.setValueAxes(valueAxes);
        return chart;
    }

    private static WebChartAxis toLabelAxis(ChartLabelAxis axis) {
        if (axis == null) {
            return null;
        }
        final WebChartAxis web = baseAxis(axis);
        final ChartLabelAxisData data = axis.getData();
        if (data != null) {
            fillAxisData(web, data.getResolver());
        }
        return web;
    }

    private static WebChartAxis toValueAxis(ChartValueAxis axis) {
        if (axis == null) {
            return null;
        }
        final WebChartAxis web = baseAxis(axis);
        final List<WebChartSeries> series = new ArrayList<>();
        if (axis.getDataList() != null) {
            for (ChartValueAxisData data : axis.getDataList()) {
                final WebChartSeries item = new WebChartSeries();
                item.setTitle(data.getTitle());
                item.setChartType(enumName(data.getType()));
                item.setColor(data.getColor());
                item.setSmooth(data.isSmooth());
                fillSeriesData(item, data.getResolver());
                series.add(item);
            }
        }
        web.setSeries(series);
        return web;
    }

    private static WebChartAxis baseAxis(ChartAxis axis) {
        final WebChartAxis web = new WebChartAxis();
        web.setTitle(axis.getTitle());
        web.setNumberFormat(axis.getNumberFormat());
        web.setAxisType(enumName(axis.getType()));
        return web;
    }

    private static void fillAxisData(WebChartAxis web, AxisDataResolver resolver) {
        if (resolver instanceof ValueAxisDataResolver) {
            web.setValues(new ArrayList<>(((ValueAxisDataResolver) resolver).getDataList()));
        } else if (resolver instanceof RelationAxisDataResolver) {
            web.setReference(toRange(((RelationAxisDataResolver) resolver).getAddress()));
        }
    }

    private static void fillSeriesData(WebChartSeries series, AxisDataResolver resolver) {
        if (resolver instanceof ValueAxisDataResolver) {
            series.setValues(new ArrayList<>(((ValueAxisDataResolver) resolver).getDataList()));
        } else if (resolver instanceof RelationAxisDataResolver) {
            series.setReference(toRange(((RelationAxisDataResolver) resolver).getAddress()));
        }
    }

    private static WebCellRange toRange(CellRangeAddress address) {
        if (address == null) {
            return null;
        }
        return new WebCellRange(address.getFirstRow(), address.getLastRow(),
                address.getFirstColumn(), address.getLastColumn());
    }

    private static Map<String, Object> legendMap(Legend legend) {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (legend != null) {
            if (legend.getPosition() != null) {
                map.put("position", enumName(legend.getPosition()));
            }
            if (legend.getOverlay() != null) {
                map.put("overlay", legend.getOverlay());
            }
        }
        return map;
    }

    private static Map<String, Object> markerMap(ChartMarker marker) {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (marker != null) {
            map.put("showPercent", marker.isShowPercent());
            map.put("showBubbleSize", marker.isShowBubbleSize());
            map.put("showLeaderLines", marker.isShowLeaderLines());
            map.put("showSerName", marker.isShowSerName());
            map.put("showCatName", marker.isShowCatName());
            map.put("showVal", marker.isShowVal());
            map.put("showLegendKey", marker.isShowLegendKey());
        }
        return map;
    }

    /**
     * 组件类型：优先模板标签名，无模板信息时取组件类简名。
     */
    private static String resolveType(Container container) {
        final ContainerTreeNode node = container.templateNode();
        if (node != null) {
            final String tagName = node.getTagName();
            if (StrUtil.isNotEmpty(tagName)) {
                return tagName;
            }
        }
        return container.getTypeName();
    }

    private static String toDataUrl(Image image) {
        if (image == null) {
            return null;
        }
        try (InputStream stream = image.openStream()) {
            final byte[] bytes = IoUtil.readBytes(stream);
            String imageType = image.getImageType();
            if (StrUtil.isEmpty(imageType)) {
                imageType = "png";
            }
            return "data:image/" + imageType.toLowerCase() + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase();
    }

    private static List<String> enumNames(Collection<? extends Enum<?>> values) {
        final List<String> names = new ArrayList<>();
        if (values != null) {
            for (Enum<?> value : values) {
                names.add(enumName(value));
            }
        }
        return names;
    }

    /**
     * 同一左上角 (row, col) 的单元格合并为一条：value/image/link/chart 优先取非空，跨度取较大值，样式补全缺失 key。
     * <p>
     * 用于 <code>SpanComponent</code>（只定义合并区域、无值）与同位置文本组件的重叠场景。
     */
    private static List<WebCell> dedupe(List<WebCell> cells) {
        final Map<String, WebCell> byPosition = new LinkedHashMap<>();
        for (WebCell cell : cells) {
            final String key = cell.getRow() + "," + cell.getCol();
            final WebCell exist = byPosition.get(key);
            if (exist == null) {
                byPosition.put(key, cell);
            } else {
                mergeCell(exist, cell);
            }
        }
        return new ArrayList<>(byPosition.values());
    }

    private static void mergeCell(WebCell target, WebCell source) {
        if (target.getContent() == null) {
            target.setContent(source.getContent());
        }
        target.setRowSpan(Math.max(target.getRowSpan(), source.getRowSpan()));
        target.setColSpan(Math.max(target.getColSpan(), source.getColSpan()));
        source.getStyle().forEach(target.getStyle()::putIfAbsent);
    }
}