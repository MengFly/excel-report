package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.util.WeightSizeHelper;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class HLayout extends AbstractLayout {

    private AlignPolicy alignPolicy = AlignPolicy.START;

    @Override
    public Size getSize() {
        Size size = new Size();
        for (Container container : getContainers()) {
            final int width = container.getSize().width;
            if (width > 0) {
                size.width += width;
            } else if (width < 0) {
                // 最少占一个宽度
                size.width += 1;
            }
            size.height = Math.max(size.height, container.getSize().height);
        }
        return size;
    }

    @Override
    public void onMeasure(Size suggestSize) {
        // 测量自身
        measuredSize = suggestSize;
        final WeightSizeHelper sizeHelper = new WeightSizeHelper(suggestSize, getContainers(), WeightSizeHelper.SIZE_TYPE_WIDTH);

        for (Container container : getContainers()) {
            final int childHeightSize = getChildContainerSuggestSize(suggestSize, container);
            final int childWidthSize = sizeHelper.distributionSize(container);

            // 测量子组件
            container.onMeasure(Size.of(childWidthSize, childHeightSize));
        }
    }

    @Override
    public void onLayout(Point relativePosition) {
        position = relativePosition;
        // 开始布局子组件
        int start = 0;
        final Size measuredSize = getMeasuredSize();
        for (Container container : getContainers()) {
            final Point position = getPosition().add(start,
                    alignPolicy.getPoint(measuredSize.height, measuredSize.height));
            container.onLayout(position);
            start += container.getMeasuredSize().width;
        }
    }

    private int getChildContainerSuggestSize(Size size, Container container) {
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
