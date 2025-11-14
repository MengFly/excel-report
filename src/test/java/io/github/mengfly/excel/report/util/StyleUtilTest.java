package io.github.mengfly.excel.report.util;

import org.junit.Assert;
import org.junit.Test;

public class StyleUtilTest {


    @Test
    public void testAnalysisStyle() {
        String style = "tableDataStyle {dataFormat:0 岁}";
        Assert.assertArrayEquals(new String[]{"tableDataStyle", "{dataFormat:0 岁}"},
                StyleUtil.analysisStyle(style).toArray());

        style = "tableDataStyle1 tableDataStyle2";
        Assert.assertArrayEquals(new String[]{"tableDataStyle1", "tableDataStyle2"},
                StyleUtil.analysisStyle(style).toArray());

        style = "{dataFormat:0 岁} tableDataStyle2 {dataFormat:0 岁}";
        Assert.assertArrayEquals(new String[]{"{dataFormat:0 岁}", "tableDataStyle2", "{dataFormat:0 岁}"},
                StyleUtil.analysisStyle(style).toArray());

        style = "{dataFormat:0 岁} {dataFormat: ${stress.count} 岁}";
        Assert.assertArrayEquals(new String[]{"{dataFormat:0 岁}", "{dataFormat: ${stress.count} 岁}"},
                StyleUtil.analysisStyle(style).toArray());
    }
}