package io.github.mengfly.excel.report.exapmle;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 大批量带样式导出示例（模板文件：{@code src/test/resources/LargeExportTemplate.xml}）
 * <p>
 * 演示点：
 * <ol>
 *     <li>大批量数据（默认 20000 行 * 11 列 = 220000 个单元格）通过模板导出</li>
 *     <li>样式全部定义在模板中：字体、填充色、边框、对齐、数字/日期格式、列宽</li>
 *     <li>把导出过程拆成 数据生成 / 模板解析 / 模板渲染 / 导出到 Sheet / 写入文件 五个阶段，分别统计耗时</li>
 *     <li>默认重复执行 {@value #DEFAULT_REPEAT} 次，输出每轮耗时以及平均值 / 最小值 / 最大值与平均吞吐</li>
 * </ol>
 * <p>
 * 运行方式（行数与重复次数均可调，数据量越大耗时越明显）：
 * <pre>
 *     # 1. 运行 main：默认 {@value #DEFAULT_ROWS} 行、重复 {@value #DEFAULT_REPEAT} 次
 *     # 2. 依次指定行数、重复次数
 *     java -cp ... TestLargeExport 100000 5
 *     # 3. 通过系统属性指定，JUnit 与 main 都生效
 *     mvn test -Dtest=TestLargeExport -Drows=50000 -Drepeat=5
 * </pre>
 * 说明：
 * <ul>
 *     <li>第 1 轮含 JIT 预热，通常偏慢，平均值包含该轮；只看稳定值可参考"最小"行</li>
 *     <li>轮次之间会调用一次 {@code System.gc()}，避免上一轮的 Workbook 占用堆影响下一轮</li>
 *     <li>大批量导出建议提高堆内存，如：{@code -Xmx2g}</li>
 * </ul>
 *
 * @author MengFly
 */
public class TestLargeExport {

    /**
     * 默认导出数据行数
     */
    private static final int DEFAULT_ROWS = 20000;

    /**
     * 默认重复次数（用于求平均值）
     */
    private static final int DEFAULT_REPEAT = 15;

    /**
     * 模板文件位置（classpath）
     */
    private static final String TEMPLATE_FILE = "LargeExportTemplate.xml";

    /**
     * 模板中的列数，用于计算单元格数量，需与 {@link #TEMPLATE_FILE} 中的 column 数量保持一致
     */
    private static final int COLUMN_COUNT = 11;

    private static final String[] REGIONS = {"华东", "华北", "华南", "西南", "西北", "东北"};
    private static final String[] PRODUCTS = {"笔记本电脑", "台式机", "显示器", "键盘", "鼠标", "路由器", "打印机", "硬盘"};
    private static final String[] STATUS = {"已完成", "进行中", "待审核", "已取消"};

    public static void main(String[] args) throws IOException {
        int rows = args.length > 0 ? Integer.parseInt(args[0]) : intProperty("rows", DEFAULT_ROWS);
        int repeat = args.length > 1 ? Integer.parseInt(args[1]) : intProperty("repeat", DEFAULT_REPEAT);
        export(rows, repeat, new File("example/large-export.xlsx"));
    }

    /**
     * 重复导出大批量数据，并输出每轮耗时与平均耗时
     *
     * @param rows   数据行数
     * @param repeat 重复次数（求平均值）
     * @param target 目标文件
     */
    public static void export(int rows, int repeat, File target) throws IOException {
        if (rows <= 0) {
            rows = DEFAULT_ROWS;
        }
        if (repeat <= 0) {
            repeat = DEFAULT_REPEAT;
        }

        List<Cost> costs = new ArrayList<>(repeat);
        for (int round = 1; round <= repeat; round++) {
            if (round > 1) {
                // 释放上一轮的 Workbook / 组件树，避免堆占用影响下一轮耗时
                System.gc();
            }
            costs.add(doExport(rows, target));
        }

        printCost(rows, costs, target);
    }

    /**
     * 执行一次导出，返回各阶段耗时
     */
    private static Cost doExport(int rows, File target) throws IOException {
        Cost cost = new Cost();

        // ---------- 1. 生成数据 ----------
        List<LargeRow> dataList = createData(rows);

        DataContext context = new DataContext();
        context.put("title", "大批量带样式导出示例（" + rows + " 行）");
        context.put("rowCount", rows);
        context.put("generateTime", DateUtil.now());
        context.put("dataList", dataList);

        ExcelReport report = new ExcelReport();

        // ---------- 2. 解析模板 ----------
        long start = System.nanoTime();
        ReportTemplate template;
        try (InputStream stream = TestLargeExport.class.getClassLoader().getResourceAsStream(TEMPLATE_FILE)) {
            template = new ReportTemplate(stream);
        }
        cost.template = System.nanoTime() - start;

        // ---------- 3. 渲染模板（把数据填充成组件树） ----------
        start = System.nanoTime();
        Container container = template.render(context);
        cost.render = System.nanoTime() - start;

        // ---------- 4. 导出到 Sheet（测量 + 布局 + 逐单元格写入） ----------
        // 注：report.exportTemplate(template, name, context) = 3 + 4，这里为了统计耗时拆开调用
        start = System.nanoTime();
        report.exportSheet(template.getName(), container, template.getSheetStyle());
        cost.export = System.nanoTime() - start;

        // ---------- 5. 写入磁盘 ----------
        report.save(target);

        return cost;
    }

    /**
     * 生成测试数据
     */
    public static List<LargeRow> createData(int rows) {
        List<LargeRow> dataList = new ArrayList<>(rows);
        long now = System.currentTimeMillis();
        for (int i = 1; i <= rows; i++) {
            dataList.add(new LargeRow(
                    i,
                    "SO" + (1000000 + i),
                    "客户-" + RandomUtil.randomString(6),
                    REGIONS[i % REGIONS.length],
                    PRODUCTS[i % PRODUCTS.length],
                    RandomUtil.randomInt(1, 1000),
                    RandomUtil.randomDouble(100, 99999),
                    RandomUtil.randomDouble(0, 1),
                    new Date(now - i * 3600_000L),
                    STATUS[i % STATUS.length],
                    i % 5 == 0 ? "加急处理" : ""
            ));
        }
        return dataList;
    }

    private static void printCost(int rows, List<Cost> costs, File target) {
        final int repeat = costs.size();
        final long cells = (long) rows * COLUMN_COUNT;
        final double avgExportMs = average(costs, CostField.EXPORT) / 1000000d;
        final String throughput = avgExportMs <= 0 ? "-" : String.format("%,.0f 单元格/秒", cells / (avgExportMs / 1000));

        final String rowFormat = "%-10s %13s %13s %15s %15s%n";
        final String line = "--------------------------------------------------------------------------"
                + "-------------------------------------------------------";

        StringBuilder builder = new StringBuilder();
        builder.append('\n').append("============== 大批量带样式导出耗时（重复 ")
                .append(repeat).append(" 次取平均） ==============").append('\n');
        builder.append(String.format("数据规模        : %,d 行 x %d 列 = %,d 个单元格%n", rows, COLUMN_COUNT, cells));
        builder.append(String.format("最大堆内存      : %,d MB%n", Runtime.getRuntime().maxMemory() / 1024 / 1024));
        builder.append(line).append('\n');
        builder.append(String.format(rowFormat, "轮次", "模板解析", "模板渲染", "导出 Sheet", "合计"));
        builder.append(line).append('\n');

        for (int i = 0; i < repeat; i++) {
            final Cost cost = costs.get(i);
            builder.append(String.format(rowFormat, "第 " + (i + 1) + " 次",
                    formatMs(cost.template), formatMs(cost.render),
                    formatMs(cost.export), formatMs(cost.total())));
        }

        builder.append(line).append('\n');
        builder.append(String.format(rowFormat, "平均",
                formatNanos(average(costs, CostField.TEMPLATE)), formatNanos(average(costs, CostField.RENDER)),
                formatNanos(average(costs, CostField.EXPORT)),
                formatNanos(average(costs, CostField.TOTAL))));
        builder.append(String.format(rowFormat, "最小",
                formatMs(min(costs, CostField.TEMPLATE)), formatMs(min(costs, CostField.RENDER)),
                formatMs(min(costs, CostField.EXPORT)),
                formatMs(min(costs, CostField.TOTAL))));
        builder.append(String.format(rowFormat, "最大",
                formatMs(max(costs, CostField.TEMPLATE)), formatMs(max(costs, CostField.RENDER)),
                formatMs(max(costs, CostField.EXPORT)),
                formatMs(max(costs, CostField.TOTAL))));
        builder.append(line).append('\n');

        builder.append(String.format("平均导出吞吐    : %s%n", throughput));
        builder.append(String.format("平均单行耗时    : %s%n", formatMs(average(costs, CostField.TOTAL) / 1000000d / rows)));
        builder.append(String.format("输出文件        : %s (%,d KB)%n", target.getAbsolutePath(), target.length() / 1024));
        builder.append(line);

        System.out.println(builder);
    }

    private static double average(List<Cost> costs, CostField field) {
        long sum = 0;
        for (Cost cost : costs) {
            sum += field.value(cost);
        }
        return (double) sum / costs.size();
    }

    private static long min(List<Cost> costs, CostField field) {
        long min = Long.MAX_VALUE;
        for (Cost cost : costs) {
            min = Math.min(min, field.value(cost));
        }
        return min;
    }

    private static long max(List<Cost> costs, CostField field) {
        long max = Long.MIN_VALUE;
        for (Cost cost : costs) {
            max = Math.max(max, field.value(cost));
        }
        return max;
    }

    private static int intProperty(String name, int defaultValue) {
        return Integer.getInteger(name, defaultValue);
    }

    private static double toMs(long nanos) {
        return nanos / 1000000d;
    }

    private static String formatMs(long nanos) {
        return formatMs(toMs(nanos));
    }

    /**
     * 纳秒转可读文本（平均值是小数，需要单独的入口避免与 {@link #formatMs(double)} 混淆）
     */
    private static String formatNanos(double nanos) {
        return formatMs(nanos / 1000000d);
    }

    private static String formatMs(double ms) {
        if (ms < 1000) {
            return String.format("%,.2f ms", ms);
        }
        return String.format("%,.0f ms", ms);
    }

    /**
     * 各阶段耗时（单位：纳秒）
     */
    private static class Cost {
        private long template;
        private long render;
        private long export;

        private long total() {
            return template + render + export;
        }
    }

    /**
     * 耗时的取值字段，便于统计平均值 / 最小值 / 最大值
     */
    private enum CostField {
        TEMPLATE {
            @Override
            long value(Cost cost) {
                return cost.template;
            }
        },
        RENDER {
            @Override
            long value(Cost cost) {
                return cost.render;
            }
        },
        EXPORT {
            @Override
            long value(Cost cost) {
                return cost.export;
            }
        },
        TOTAL {
            @Override
            long value(Cost cost) {
                return cost.total();
            }
        };

        abstract long value(Cost cost);
    }

    /**
     * 表格行数据，字段名与模板中 column 的 id 一一对应
     */
    @Data
    @AllArgsConstructor
    public static class LargeRow {
        /**
         * 序号
         */
        private Integer seq;
        /**
         * 订单号
         */
        private String orderNo;
        /**
         * 客户名称
         */
        private String customer;
        /**
         * 区域
         */
        private String region;
        /**
         * 产品名称
         */
        private String product;
        /**
         * 数量
         */
        private Integer quantity;
        /**
         * 金额
         */
        private Double amount;
        /**
         * 完成率
         */
        private Double ratio;
        /**
         * 下单日期
         */
        private Date orderDate;
        /**
         * 状态
         */
        private String status;
        /**
         * 备注
         */
        private String remark;
    }
}
