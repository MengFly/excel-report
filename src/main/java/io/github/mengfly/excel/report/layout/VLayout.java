package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
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
            size.height += container.getSize().height;
        }
        return size;
    }

    @Override
    public void onExport(ReportContext context, Point point, Size suggestSize) {

        int start = 0;

        for (Container container : getContainers()) {
            final Size childSize = getChildContainerSujjestSize(suggestSize, container);

            container.export(
                    context,
                    point.add(alignPolicy.getPoint(childSize.width, childSize.width), start),
                    childSize
            );

            start += childSize.height;
        }
    }

    private Size getChildContainerSujjestSize(Size size, Container container) {
        final Size containerSize = container.getSize();

        if (containerSize.width >= 0 && containerSize.height >= 0) {
            return containerSize;
        }

        if (containerSize.width == -1) {
            return Size.of(size.width, containerSize.height);
        } else {
            return containerSize;
        }
    }
}
