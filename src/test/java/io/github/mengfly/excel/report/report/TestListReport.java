package io.github.mengfly.excel.report.report;


import io.github.mengfly.excel.report.component.ListComponent;
import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.component.table.TableComponent;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.layout.VLayout;

/**
 * <code><pre>
 * VLayout(
 *      Text(),
 *      Table(),
 *      List(H),
 *      HLayout(Text(), Text(), List(V),
 *      Table()
 * )
 * </pre></code>
 */
public class TestListReport extends VLayout {
    public TestListReport() {
        setAlignPolicy(AlignPolicy.CENTER);

        addItem(new TextComponent(new Size(10, 5), "Test(width=10, height=5)"));

        TableComponent table = new TableComponent(TestDataUtil.getData(10), TestDataUtil.getColumns());
        addItem(table);

        addItem(getHorizontalList());

        final HLayout hLayout = addItem(new HLayout());

        hLayout.addItem(new TextComponent(new Size(3, 10), "Test(width=3, height=10)"));
        hLayout.addItem(new TextComponent(new Size(5, 10), "Test(width=5, height=10)"));
        hLayout.addItem(getVerticalList());

        addItem(table);
    }

    private ListComponent getHorizontalList() {
        ListComponent component = new ListComponent();
        component.setOrientation(Orientation.HORIZONTAL);
        component.setSpan(2);
        component.setTitle("Horizontal List");
        component.setDataList(TestDataUtil.getRandomStringList(9));
        return component;
    }

    private ListComponent getVerticalList() {
        ListComponent component = new ListComponent();
        component.setOrientation(Orientation.VERTICAL);
        component.setTitle("Vertical List");
        component.setSpan(2);
        component.setDataList(TestDataUtil.getRandomStringList(9));
        return component;
    }


}
