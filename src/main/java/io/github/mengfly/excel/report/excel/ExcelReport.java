package io.github.mengfly.excel.report.excel;

import cn.hutool.core.util.StrUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.SheetStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Excel Report
 * <br>
 * 导出Excel文件
 *
 * @author Mengfly
 */
@Slf4j
@Getter
public class ExcelReport {

    /**
     * 匿名 Sheet（{@code name} 为空）的基名
     */
    private static final String DEFAULT_SHEET_NAME = "sheet";

    private final XSSFWorkbook workbook = new XSSFWorkbook();

    /*
     * Sheet 基名 -> 已用掉的下一个序号。
     *
     * 只在 getSheet 的同步块内访问，所以 HashMap 就够：命名与 createSheet 必须整体互斥，
     * 单把 Map 换成并发容器解决不了这段的 TOCTOU；真正不能并发的其实是 XSSFWorkbook 本身，
     * 即"并行单元是 workbook 不是 sheet"（依据见 doc/优化建议.md 四.4）。
     */
    private final Map<String, Integer> sheetNameSequence = new HashMap<>();

    /**
     * 存储Excel文件
     *
     * @param file 要存储的文件位置
     * @throws IOException 写文件失败抛出此异常
     */
    public void save(File file) throws IOException {
        try (OutputStream stream = Files.newOutputStream(file.toPath())) {
            // 防止无Sheet页Excel无法打开
            if (workbook.getNumberOfSheets() == 0) {
                getSheet(null);
            }
            workbook.write(stream);
        }
    }

    /**
     * 导出组件到Sheet页面中
     *
     * @param name       sheet 页面名称， 如果名称重复则在名称后面自动添加序号
     * @param container  要导出的组件
     * @param sheetStyle Sheet页面的样式
     */
    public void exportSheet(String name, Container container, StyleMap sheetStyle) {
        XSSFSheet sheet = getSheet(name);
        StyleMap sheetStyleMap = SheetStyles.DEFAULT_STYLE.createChildStyleMap(sheetStyle);
        SheetStyles.initStyle(sheet, sheetStyleMap);
        ReportContext context = new ReportContext(workbook, sheet);
        context.getStyleChain().onStyle(CellStyles.DEFAULT_STYLE,
                () -> {
                    // 在导出数据之前，先进行测量
                    container.onMeasure();
                    container.onLayout();
                    container.export(context);
                });
        context.applyCellWidthHeight(sheetStyleMap);
    }

    /**
     * 导出模板到Sheet页面中
     *
     * @param template 模板
     * @param name     sheet 页面名称， 如果名称重复则在名称后面自动添加序号
     * @param context  模板数据
     * @return 导出的组件
     */
    public Container exportTemplate(ReportTemplate template, String name, DataContext context) {
        Container container = template.render(context);
        if (container == null) {
            log.warn("This template has not found any container.");
            return null;
        }
        exportSheet(name, container, template.getSheetStyle());
        return  container;
    }

    /**
     * 取一个 Sheet，名称重复时自动追加 {@code _序号}（{@code name} 为空时基名为 {@code sheet}）。
     */
    private synchronized XSSFSheet getSheet(String name) {
        /*
         * 计数器必须以「基名」为 key。这里曾经用 name 读、用补过后缀的 sheetName 写，
         * 于是同一基名的计数器永远停在 1 ⇒ 第 3 个同名 Sheet、或第 2 个匿名 Sheet 会抛
         * IllegalArgumentException: The workbook already contains a sheet named 'X'。
         */
        final String baseName = StrUtil.isEmpty(name) ? DEFAULT_SHEET_NAME : name;

        final Integer used = sheetNameSequence.get(baseName);
        int seq = used == null ? 0 : used;

        String sheetName = seq == 0 ? baseName : baseName + "_" + seq;
        // 生成的序号名可能已被占用（用户自己起的名，或并发下另一个线程刚建过），依次往后找
        while (workbook.getSheet(sheetName) != null) {
            seq++;
            sheetName = baseName + "_" + seq;
        }

        sheetNameSequence.put(baseName, seq + 1);
        return workbook.createSheet(sheetName);
    }


}
