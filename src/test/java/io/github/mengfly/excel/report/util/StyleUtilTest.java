package io.github.mengfly.excel.report.util;

import org.junit.Assert;
import org.junit.Test;

public class StyleUtilTest {


    @Test
    public void testAnalysisStyle() {
        String style = "tableDataStyle {dataFormat:0 岁}";
        Assert.assertArrayEquals(StyleUtil.analysisStyle(style).toArray(),
                new String[]{"tableDataStyle", "{dataFormat:0 岁}"});

        style = "tableDataStyle1 tableDataStyle2";
        Assert.assertArrayEquals(StyleUtil.analysisStyle(style).toArray(),
                new String[]{"tableDataStyle1", "tableDataStyle2"});

        style = "{dataFormat:0 岁} tableDataStyle2 {dataFormat:0 岁}";
        Assert.assertArrayEquals(StyleUtil.analysisStyle(style).toArray(),
                new String[]{"{dataFormat:0 岁}", "tableDataStyle2", "{dataFormat:0 岁}"});

        style = "{dataFormat:0 岁} {dataFormat: ${stress.count} 岁}";
        Assert.assertArrayEquals(StyleUtil.analysisStyle(style).toArray(),
                new String[]{"{dataFormat:0 岁}", "{dataFormat: ${stress.count} 岁}"});
    }
}