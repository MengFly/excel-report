package io.github.mengfly.excel.report.report;


import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.component.image.FileSystemImage;
import io.github.mengfly.excel.report.component.image.ImageComponent;
import io.github.mengfly.excel.report.component.list.ListComponent;
import io.github.mengfly.excel.report.component.list.ListHeader;
import io.github.mengfly.excel.report.component.table.TableColumn;
import io.github.mengfly.excel.report.component.table.TableComponent;
import io.github.mengfly.excel.report.component.table.TableObjFieldColumn;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.layout.VLayout;

import java.util.Arrays;
import java.util.List;

/**
 * <code><pre>
 *     TestImage[10,39] {
 *     TextComponent[10,5]
 *     HLayout[10,11] {
 *         TableComponent[3,11]
 *         ImageComponent[7,11]
 *     }
 *     ListComponent[10,2]
 *     HLayout[10,10] {
 *         TextComponent[3,10]
 *         TextComponent[5,10]
 *         ListComponent[2,10]
 *     }
 *     HLayout[10,11] {
 *         TableComponent[3,11]
 *         ImageComponent[7,11]
 *     }
 * }
 * </pre>
 * </code>
 */
public class TestImage extends VLayout {

    public TestImage() {
        setAlignPolicy(AlignPolicy.CENTER);

        addItem(new TextComponent(new Size(10, 5), "Test(width=10, height=5)"));

        final HLayout layout = getTableImageLayout();

        addItem(layout);

        addItem(getHorizontalList());


        final HLayout hLayout = addItem(new HLayout());

        hLayout.addItem(new TextComponent(new Size(3, 10), "Test(width=3, height=10)"));
        hLayout.addItem(new TextComponent(new Size(5, 10), "Test(width=5, height=10)"));
        hLayout.addItem(getVerticalList());

        addItem(layout);
    }

    private static HLayout getTableImageLayout() {
        TableComponent table = new TableComponent(TestDataUtil.getData(10), getColumns());
        final ImageComponent image = new ImageComponent();
        image.setImage(new FileSystemImage(TestDataUtil.getTestImageFile()));
        image.setSize(Size.of(7, 11));
        HLayout layout = new HLayout();
        layout.addItem(table);
        layout.addItem(image);
        return layout;
    }

    private ListComponent getHorizontalList() {
        ListComponent component = new ListComponent();
        component.setOrientation(Orientation.HORIZONTAL);
        component.setSpan(2);
        component.setHeader(new ListHeader("Horizontal List", 1));
        component.setDataList(TestDataUtil.getRandomStringList(9));
        return component;
    }

    private ListComponent getVerticalList() {
        ListComponent component = new ListComponent();
        component.setOrientation(Orientation.VERTICAL);
        component.setHeader(new ListHeader("Vertical List", 1));
        component.setSpan(2);
        component.setDataList(TestDataUtil.getRandomStringList(9));
        return component;
    }

    public static List<TableColumn> getColumns() {
        return Arrays.asList(
                TableObjFieldColumn.of("seq", "序号"),
                TableObjFieldColumn.of("name", "姓名"),
                TableObjFieldColumn.of("age", "年龄")
        );
    }

}
