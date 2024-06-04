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
    public void onExport(ReportContext context, Point point, Size suggestSize) {
        int start = 0;

        final WeightSizeHelper sizeHelper = new WeightSizeHelper(suggestSize, getContainers(), WeightSizeHelper.SIZE_TYPE_WIDTH);

        for (Container container : getContainers()) {
            final int childHeightSize = getChildContainerSuggestSize(suggestSize, container);
            final int childWidthSize = sizeHelper.distributionSize(container);

            Size childSize = new Size(childWidthSize, childHeightSize);

            container.export(context, point.add(start, alignPolicy.getPoint(suggestSize.height, childSize.height)), childSize);

            start += childSize.width;
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
