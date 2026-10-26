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


@Setter
@Getter
public class HLayout extends AbstractLayout {

    private AlignPolicy alignPolicy = AlignPolicy.START;

    @Override
    public Size getSize() {
        return sumContainerSize(getContainers());
    }

    @Override
    public void onMeasure(Size suggestSize) {
        // 测量自身
        measuredSize = suggestSize;
        measure(getContainers(), suggestSize);
        reportOverflow(WeightSizeHelper.SIZE_TYPE_WIDTH);
    }

    @Override
    public void onLayout(Point relativePosition) {
        position = relativePosition;
        // 开始布局子组件
        int start = 0;
        for (Container container : containers) {
            Size childSize = container.getMeasuredSize();
            final Point position = relativePosition.add(start,
                    alignPolicy.getPoint(measuredSize.height, childSize.height));
            container.onLayout(position);
            start += container.getMeasuredSize().width;
        }
    }

    public static void measure(List<Container> containers, Size suggestSize) {
        final WeightSizeHelper sizeHelper = new WeightSizeHelper(suggestSize, containers, WeightSizeHelper.SIZE_TYPE_WIDTH);

        for (Container container : containers) {
            final int childHeightSize = getChildContainerSuggestSize(suggestSize, container);
            final int childWidthSize = sizeHelper.distributionSize(container);

            // 测量子组件
            container.onMeasure(Size.of(childWidthSize, childHeightSize));
        }
    }

    /**
     * 计算布局的内容尺寸：高度取子节点最大值，宽度为子节点宽度之和（弹性子节点至少占 1 列）。
     * <p>
     * 子节点非空且高度全为负数（弹性）时，高度返回 -1，表示自身无固有高度、由父级可用高度决定。
     *
     * @param containers 子节点
     * @return 内容尺寸
     */
    public static Size sumContainerSize(List<Container> containers) {
        Size size = new Size();
        // 与 VLayout 对称：子节点全为弹性高度时返回 -1（由父级决定），而不是塌成 0。
        boolean allFlexHeight = !containers.isEmpty();
        for (Container container : containers) {
            final Size childSize = container.getSize();
            final int width = childSize.width;
            if (width > 0) {
                size.width += width;
            } else if (width < 0) {
                // 最少占一个宽度
                size.width += 1;
            }
            if (childSize.height >= 0) {
                allFlexHeight = false;
            }
            size.height = Math.max(size.height, childSize.height);
        }
        if (allFlexHeight) {
            size.height = -1;
        }
        return size;
    }


    private static int getChildContainerSuggestSize(Size size, Container container) {
        final Size style = container.getStyle(CellStyles.preferredSize, null);

        if (style != null && style.height < 0) {
            return size.height;
        }
        final Size containerSize = container.getSize();

        if (containerSize.height >= 0) {
            return containerSize.height;
        }
        return size.height;
    }
}
