package io.github.mengfly.excel.report.bridge;

import cn.hutool.json.JSONUtil;
import io.github.mengfly.excel.report.bridge.text.TextStructExporter;
import io.github.mengfly.excel.report.bridge.web.WebExcel;
import io.github.mengfly.excel.report.bridge.web.WebJsonExporter;
import io.github.mengfly.excel.report.exapmle.TestTemplate;
import io.github.mengfly.excel.report.exapmle.util.TestTemplateUtil;
import io.github.mengfly.excel.report.excel.ExportResult;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class BridgeExportTest {
    static ExportResult export;

    @Before
    public void beforeTest() throws IOException {
        export = TestTemplateUtil.export("TestTemplate.xml", TestTemplate.createTemplateContext());
    }

    @Test
    public void testWebJsonExport() {
        final WebExcel excel = BridgeExporter.export(WebJsonExporter.instance, export);
        final String jsonPrettyStr = JSONUtil.toJsonPrettyStr(excel);
        System.out.println(jsonPrettyStr);
    }

    @Test
    public void testTextExport() {
        final String text = BridgeExporter.export(TextStructExporter.instance, export);
        System.out.println(text);
    }
}
