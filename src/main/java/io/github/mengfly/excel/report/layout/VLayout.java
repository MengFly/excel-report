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

    public static Size sumContainerSize(List<Container> containers) {
        Size size = new Size();
        for (Container container : containers) {
            size.width = Math.max(size.width, container.getSize().width);

            final int height = container.getSize().height;
            if (height > 0) {
                size.height += height;
            } else if (height < 0) {
                // 至少占一行
                size.height += 1;
            }
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
