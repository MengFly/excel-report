package io.github.mengfly.excel.report.excel;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.component.table.TableColumn;
import io.github.mengfly.excel.report.component.table.TableComponentNew;
import io.github.mengfly.excel.report.component.table.TableObjFieldColumn;
import io.github.mengfly.excel.report.component.text.TextComponent;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.style.StyleMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Container.finalStyle 的回归用例：导出期必须把"祖先链 + 自身"的合并样式记录到每个节点上。
 * 钉住三件事：
 *   1. 子节点的 finalStyle 要含父级继承来的样式；
 *   2. 同名 key 子覆盖父；
 *   3. TableComponentNew 直连 onExport 的路径也要回填（该路径曾绕过 Container.export）。
 * 背景与取舍见 doc/container-final-style.md。
 */
public class FinalStyleTest {

    private final ExcelReport report = new ExcelReport();

    private void export(Container container) {
        report.exportSheet("s", container, new StyleMap());
    }

    /** 子节点的 finalStyle = 父级继承 + 自身。 */
    @Test
    public void childShouldInheritParentStyle() {
        HLayout layout = new HLayout();
        layout.addStyle(CellStyles.fontBold, true);

        TextComponent text = new TextComponent(Size.of(1, 1), "v");
        text.addStyle(CellStyles.fontHeight, 20.);
        layout.addItem(text);

        export(layout);

        Assert.assertNotNull("布局节点应记录 finalStyle", layout.getFinalStyle());
        Assert.assertEquals("布局节点应含自身 fontBold", Boolean.TRUE,
                layout.getFinalStyle().getStyle(CellStyles.fontBold, null));

        Assert.assertNotNull("子节点应记录 finalStyle", text.getFinalStyle());
        Assert.assertEquals("子节点应继承父级 fontBold", Boolean.TRUE,
                text.getFinalStyle().getStyle(CellStyles.fontBold, null));
        Assert.assertEquals("子节点自身 fontHeight 应保留", Double.valueOf(20.),
                text.getFinalStyle().getStyle(CellStyles.fontHeight, null));
    }

    /** 同名 key 子覆盖父。 */
    @Test
    public void childShouldOverrideParentStyle() {
        HLayout layout = new HLayout();
        layout.addStyle(CellStyles.fontHeight, 10.);

        TextComponent text = new TextComponent(Size.of(1, 1), "v");
        text.addStyle(CellStyles.fontHeight, 20.);
        layout.addItem(text);

        export(layout);

        Assert.assertEquals("子节点应覆盖父级 fontHeight", Double.valueOf(20.),
                text.getFinalStyle().getStyle(CellStyles.fontHeight, null));
    }

    /** 未导出时 finalStyle 为空。 */
    @Test
    public void finalStyleShouldBeNullBeforeExport() {
        Assert.assertNull("未导出前不应有 finalStyle",
                new TextComponent(Size.of(1, 1), "v").getFinalStyle());
    }

    /** Table 的直连 onExport 路径也要把列样式记到子 Container 上（header 用 column 样式，数据用 dataStyle）。 */
    @Test
    public void tableColumnStyleShouldReachChildContainer() {
        TableObjFieldColumn column = TableObjFieldColumn.of("name", "名称");
        column.addDataStyle(CellStyles.fontHeight, 9.);

        Map<String, Object> row = new HashMap<>();
        TableComponentNew table = new TableComponentNew(
                Collections.singletonList(row),
                Collections.<TableColumn>singletonList(column));

        export(table);

        List<Container> children = table.getContainers();
        // header 子节点用 column.getStyle()（TableColumn 构造器内置 fontBold=true）
        Container header = children.get(0);
        Assert.assertNotNull("header 子节点应记录 finalStyle", header.getFinalStyle());
        Assert.assertEquals("header 应带上列样式 fontBold", Boolean.TRUE,
                header.getFinalStyle().getStyle(CellStyles.fontBold, null));

        // 数据子节点用 column.getDataStyle()
        Container data = children.get(1);
        Assert.assertNotNull("数据子节点应记录 finalStyle", data.getFinalStyle());
        Assert.assertEquals("数据子节点应带上 dataStyle", Double.valueOf(9.),
                data.getFinalStyle().getStyle(CellStyles.fontHeight, null));
    }

    @After
    public void tearDown() throws Exception {
        report.getWorkbook().close();
    }
}
