package io.github.mengfly.excel.report.exapmle;

import cn.hutool.core.util.RandomUtil;
import io.github.mengfly.excel.report.exapmle.util.TestTemplateUtil;
import io.github.mengfly.excel.report.excel.ExcelReport;
import io.github.mengfly.excel.report.template.DataContext;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Example1 {


    private static List<List<DataStat>> getData() {
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
        TestTemplateUtil.exportTemplate(report, context, "Example1Template.xml");

        TestTemplateUtil.saveReport(report, "example1");
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
