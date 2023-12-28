package io.github.mengfly.excel.report.layout;

import lombok.Getter;
import lombok.Setter;
import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;


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
    public void onExport(ReportContext context, Point point) {
        final Size size = getSize();

        int start = 0;

        for (Container container : getContainers()) {
            final Size childSize = container.getSize();

            container.export(context, point.add(alignPolicy.getPoint(size.width, childSize.width), start));

            start += childSize.height;
        }
    }
}
