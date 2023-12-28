package io.github.mengfly.excel.report.report;

import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.component.table.TableComponent;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.layout.VLayout;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * <code><pre>
 * VLayout(
 *      Text(),
 *      Table(),
 *      HLayout(Text(), Text()),
 *      Table()
 * )
 * </pre></code>
 */
public class TestTableReport extends VLayout {
    public TestTableReport() {
        setAlignPolicy(AlignPolicy.START);

        addItem(new TextComponent(new Size(10, 5), "Test(width=10, height=5)"));

        TableComponent table = new TableComponent(TestDataUtil.getData(10), TestDataUtil.getColumns());
        addItem(table);

        final HLayout hLayout = addItem(new HLayout());

        hLayout.addItem(new TextComponent(new Size(3, 1), "Test(width=3)"));
        hLayout.addItem(new TextComponent(new Size(5, 1), "Test(width=5)"));

        addItem(table);
    }


    @Data
    @AllArgsConstructor
    public static class TestData {
        private Integer seq;
        private String name;
        private Integer age;
    }
}
