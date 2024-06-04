package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import io.github.mengfly.excel.report.style.CellStyles;
import io.github.mengfly.excel.report.util.WeightSizeHelper;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class VLayout extends AbstractLayout {

    private AlignPolicy alignPolicy = AlignPolicy.START;

    @Override
    public Size getSize() {
        Size size = new Size();
        for (Container container : getContainers()) {
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

    @Override
    public void onExport(ReportContext context, Point point, Size suggestSize) {

        int start = 0;

        final WeightSizeHelper sizeHelper = new WeightSizeHelper(suggestSize, getContainers(), WeightSizeHelper.SIZE_TYPE_HEIGHT);

        for (Container container : getContainers()) {
            final int childWidthSize = getChildContainerSuggestSize(suggestSize, container);
            final int childHeightSize = sizeHelper.distributionSize(container);
            Size childSize = new Size(childWidthSize, childHeightSize);

            container.export(
                    context,
                    point.add(alignPolicy.getPoint(childSize.width, childSize.width), start),
                    childSize
            );

            start += childSize.height;
        }
    }

    private int getChildContainerSuggestSize(Size size, Container container) {

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
