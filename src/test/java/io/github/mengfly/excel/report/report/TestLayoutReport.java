package io.github.mengfly.excel.report.report;


import io.github.mengfly.excel.report.component.TextComponent;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.layout.HLayout;
import io.github.mengfly.excel.report.layout.VLayout;
import io.github.mengfly.excel.report.style.CellStyles;

/**
 * <code><pre>
 * VLayout(
 *      Text(),
 *      HLayout(Text(), Text())
 * )
 * </pre></code>
 */
public class TestLayoutReport extends VLayout {

    public TestLayoutReport() {
        setAlignPolicy(AlignPolicy.CENTER);
        addItem(new TextComponent(new Size(10, 5), "Test(width=10, height=5)"));

        final HLayout hLayout = addItem(new HLayout());

        final TextComponent item = new TextComponent(new Size(3, 1), "Test(width=3)");
        item.addStyle(CellStyles.fontColor, CellStyles.createColor(0xff0000));
        item.addStyle(CellStyles.fontBold, true);
        item.addStyle(CellStyles.fontName, "楷体");
        hLayout.addItem(item);
        hLayout.addItem(new TextComponent(new Size(5, 1), "Test(width=5)"));
    }
}
