package io.github.mengfly.excel.report.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.StyleHolder;
import io.github.mengfly.excel.report.template.ContainerTreeNode;
import io.github.mengfly.excel.report.util.WeightSizeHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class AbstractLayout extends StyleHolder implements Layout {


    private ContainerTreeNode templateNode;
    @Getter
    protected final List<Container> containers = new ArrayList<>();
    @Getter
    protected Size measuredSize;
    @Getter
    protected Point position;

    @Override
    public ContainerTreeNode templateNode() {
        return templateNode;
    }

    @Override
    public void templateNode(ContainerTreeNode templateNode) {
        this.templateNode = templateNode;
    }

    @Override
    public final void export(ReportContext context) {
        Layout.super.export(context);
    }

    @Override
    public void onExport(ReportContext context) {
        for (Container container : getContainers()) {
            container.export(context);
        }
    }

    @Override
    public <T extends Container> T addItem(T item, Object constraint) {
        this.containers.add(item);
        return item;
    }

    protected Size containerWrapSize(Size size) {
        return Size.of(
                size.width > 0 ? size.width : 1,
                size.height > 0 ? size.height : 1
        );
    }

    /*
     * 已告警过的子节点：GridLayout 在 onLayout 里会再 onMeasure 一次，不去重会重复打印。
     */
    private final Set<Container> overflowWarned = Collections.newSetFromMap(new IdentityHashMap<Container, Boolean>());

    /*
     * 2026-10-23 新增：分配后校验"弹性子节点分到的尺寸 < 其内容尺寸"。
     *
     * 旧实现既不校验也不裁剪：子元素照旧顺序排下去，越过本节点范围、压到下一个兄弟节点的行/列区间，
     * 两张合并区相交后要到导出阶段才由 POI addMergedRegion 抛 IllegalStateException，而报错里只有单元格坐标、
     * 与模板位置无关（详见 doc/flex-layout-allocation.md）。
     *
     * 只看弹性子节点：非弹性子节点的分配值就是它自己声明的尺寸（布局类的 getSize() 也已覆盖成内容累加），
     * 不会溢出；而 size="31,0" 这类占位节点（如 PageRowSplit）若按"内容至少 1"比较会天天误报。
     *
     * 合成布局（GridLayout#getContainerGroup 每次临时 new 的 VLayout/HLayout）没有 templateNode，
     * 会随 getSize()/onMeasure/onLayout 反复创建，真因在它的真实父级，这里直接跳过。
     */
    protected void reportOverflow(int sizeType) {
        if (templateNode() == null) {
            return;
        }
        final boolean vertical = sizeType == WeightSizeHelper.SIZE_TYPE_HEIGHT;
        final String unit = vertical ? "行" : "列";
        final int available = vertical ? measuredSize.height : measuredSize.width;

        final int[] contents = new int[containers.size()];
        int contentSum = 0;
        for (int i = 0; i < contents.length; i++) {
            contents[i] = WeightSizeHelper.contentSizeOf(containers.get(i), sizeType);
            contentSum += contents[i];
        }

        for (int i = 0; i < contents.length; i++) {
            final Container container = containers.get(i);
            if (!WeightSizeHelper.isFlexSize(container, sizeType)) {
                continue;
            }
            final int allocated = vertical ? container.getMeasuredSize().height : container.getMeasuredSize().width;
            final int content = contents[i];
            if (allocated >= content || !overflowWarned.add(container)) {
                continue;
            }
            log.warn("布局内容溢出：{}（父容器第 {} 个子节点）分到 {} {}，内容需要 {} {}（缺 {} {}）；父容器可用 {} {}，子节点内容合计 {} {}。"
                            + "继续导出写合并区时会抛 IllegalStateException，请检查该节点 size 的{}是否偏小（-1 表示弹性）。",
                    nodePath(container), i + 1, allocated, unit, content, unit, content - allocated, unit,
                    available, unit, contentSum, unit, vertical ? "高度" : "宽度");
        }
    }

    // 模板节点自底向上的标签链（带上该节点的 size 属性），用于在日志里点名病灶而不必再写探针。
    private static String nodePath(Container container) {
        final ContainerTreeNode node = container.templateNode();
        if (node == null) {
            return container.getClass().getSimpleName();
        }
        final StringBuilder path = new StringBuilder();
        for (ContainerTreeNode current = node; current != null; current = current.getParent()) {
            if (path.length() > 0) {
                path.insert(0, " > ");
            }
            final String size = current.getAttribute("size");
            path.insert(0, size.isEmpty() ? current.getTagName() : current.getTagName() + "[size=" + size + "]");
        }
        return path.toString();
    }
}
