package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.util.WeightSizeHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class VLayout extends AbstractLayout {

    private AlignPolicy alignPolicy = AlignPolicy.START;

    @Override
    public Size getSize() {
        return sumContainerSize(getContainers());
    }

    @Override
    public void onMeasure(Size suggestSize) {
        measuredSize = suggestSize;
        measure(getContainers(), suggestSize);
        reportOverflow(WeightSizeHelper.SIZE_TYPE_HEIGHT);
    }

    @Override
    public void onLayout(Point relativePosition) {
        position = relativePosition;
        int start = 0;

        for (Container container : containers) {
            Size childSize = container.getMeasuredSize();

            final Point point = relativePosition.add(
                    alignPolicy.getPoint(measuredSize.width, childSize.width), start);

            container.onLayout(point);

            start += childSize.height;
        }
    }

    /**
     * 计算布局的内容尺寸：宽度取子节点最大值，高度为子节点高度之和（弹性子节点至少占 1 行）。
     * <p>
     * 子节点非空且宽度全为负数（弹性）时，宽度返回 -1，表示自身无固有宽度、由父级可用宽度决定。
     *
     * @param containers 子节点
     * @return 内容尺寸
     */
    public static Size sumContainerSize(List<Container> containers) {
        Size size = new Size();
        // 子节点全为弹性宽度时若返回 0，省略 size 的布局会被父级当成"确定 0 宽"而塌陷；
        // 返回 -1 才能走"继承父级可用宽度"的弹性分支（见 getChildContainerSuggestSize）。
        boolean allFlexWidth = !containers.isEmpty();
        for (Container container : containers) {
            final Size childSize = container.getSize();
            if (childSize.width >= 0) {
                allFlexWidth = false;
            }
            size.width = Math.max(size.width, childSize.width);

            final int height = childSize.height;
            if (height > 0) {
                size.height += height;
            } else if (height < 0) {
                // 至少占一行
                size.height += 1;
            }
        }
        if (allFlexWidth) {
            size.width = -1;
        }
        return size;
    }

    public static void measure(List<Container> containers, Size suggestSize) {
        final WeightSizeHelper sizeHelper =
                new WeightSizeHelper(suggestSize, containers, WeightSizeHelper.SIZE_TYPE_HEIGHT);

        for (Container container : containers) {
            final int childWidthSize = getChildContainerSuggestSize(suggestSize, container);
            final int childHeightSize = sizeHelper.distributionSize(container);
            Size childSize = new Size(childWidthSize, childHeightSize);
            // 子组件继续测量
            container.onMeasure(childSize);
        }
    }

    private static int getChildContainerSuggestSize(Size size, Container container) {

        final Size style = container.getStyle(CellStyles.preferredSize, null);

        if (style != null && style.width < 0) {
            return size.width;
        }
        final Size containerSize = container.getSize();

        if (containerSize.width >= 0) {
            return containerSize.width;
        }
        return size.width;
    }
}
