package io.github.mengfly.excel.report.exapmle;

import cn.hutool.core.util.RandomUtil;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.template.DataContext;
import io.github.mengfly.excel.report.template.ReportTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Example1 {


    public static List<List<DataStat>> getData() {
        List<List<DataStat>> province = new ArrayList<>();
        province.add(Collections.singletonList(DataStat.createRandom("all")));
        for (int i = 0; i < 5; i++) {
            List<DataStat> stats = new ArrayList<>();
            for (int i1 = 0; i1 < RandomUtil.randomInt(3, 8); i1++) {
                stats.add(DataStat.createRandom("XXX"));
            }
            province.add(stats);
        }
        return province;
    }

    public static void main(String[] args) throws IOException {


        DataContext context = new DataContext();
        context.put("data", Example1.getData());

        ExcelReport report = new ExcelReport();

        try (final InputStream resourceAsStream = Example1.class.getClassLoader().getResourceAsStream("Example1Template.xml")) {
            ReportTemplate template = new ReportTemplate(resourceAsStream);

            report.exportTemplate(template, null, context);
        }
        report.save(new File("example1.xlsx"));
    }

    @Data
    @AllArgsConstructor
    private static class DataStat {
        private String name;
        private DataItem all;
        private DataItem local;
        private DataItem localOther;
        private DataItem other;

        public static DataStat createRandom(String name) {
            return new DataStat(name,
                    DataItem.createRandom(),
                    DataItem.createRandom(),
                    DataItem.createRandom(),
                    DataItem.createRandom()
            );
        }

    }


    @Data
    @AllArgsConstructor
    public static class DataItem {
        private Long sum;
        private Long man;
        private Long women;

        public static DataItem createRandom() {
            return new DataItem(RandomUtil.randomLong(1000000),
                    RandomUtil.randomLong(1000000),
                    RandomUtil.randomLong(1000000)
            );
        }
    }
}
