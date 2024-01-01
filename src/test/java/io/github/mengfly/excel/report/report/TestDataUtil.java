package io.github.mengfly.excel.report.report;

import cn.hutool.core.util.RandomUtil;
import io.github.mengfly.excel.report.component.table.TableColumn;
import io.github.mengfly.excel.report.component.table.TableObjFieldColumn;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestDataUtil {

    public static File getTestImageFile() {
        return new File("img/test.jpg");

    }

    public static List<String> getRandomStringList(int size) {
        List<String> randomData = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            randomData.add(RandomUtil.randomString(5));
        }
        return randomData;
    }

    public static List<Object> getData(int size) {
        List<Object> testData = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            testData.add(new TestData(i + 1, RandomUtil.randomString(5), 10 + i, RandomUtil.randomDouble(60, 100)));
        }
        return testData;
    }

    public static List<TableColumn> getColumns() {
        return Arrays.asList(
                TableObjFieldColumn.of("seq", "序号", 2),
                TableObjFieldColumn.of("name", "姓名", 3),
                TableObjFieldColumn.of("age", "年龄", 2)
        );
    }

    public static List<? extends Number> getRandomValueData() {
        List<Double> doubles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            doubles.add(RandomUtil.randomDouble(0, 10));
        }
        return doubles;
    }

    public static List<Integer> getRandomIntegerList(int i) {
        List<Integer> integers = new ArrayList<>();
        for (int j = 0; j < i; j++) {
            integers.add(RandomUtil.randomInt(100));
        }
        return integers;
    }


    @Data
    @AllArgsConstructor
    public static class TestData {
        private Integer seq;
        private String name;
        private Integer age;
        private Double score;
    }
}
