package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Grid Layout
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class GridLayout extends AbstractLayout {

    /**
     * 网格排布方向
     */
    private Orientation orientation = Orientation.VERTICAL;
    /**
     * 垂直于排布方向上的组件数量
     */
    private final int count;

    @Override
    public Size getSize() {
        int count = Math.max(1, getCount());


        if (orientation == Orientation.VERTICAL) {
            return getVerticalSize(count);
        } else {
            return getHorizontalSize(count);
        }
    }

    private Size getHorizontalSize(int count) {
        int width = 0;
        int height = 0;

        for (int i = 0; i < getContainers().size(); i++) {
            int horizontalSize = 0;
            int verticalSize = 0;
            for (int index = 0; index < count; index++) {
                if (i < getContainers().size()) {
                    final Size containerSize = getContainers().get(i).getSize();
                    horizontalSize += containerSize.width;
                    verticalSize = Math.max(verticalSize, containerSize.height);
                }
                i++;
            }
            width = Math.max(width, horizontalSize);
            height += verticalSize;
        }

        return new Size(width, height);
    }

    private Size getVerticalSize(int count) {
        int width = 0;
        int height = 0;

        for (int i = 0; i < getContainers().size(); i++) {
            int horizontalSize = 0;
            int verticalSize = 0;
            for (int index = 0; index < count; index++) {
                if (i < getContainers().size()) {
                    final Size containerSize = getContainers().get(i).getSize();
                    horizontalSize = Math.max(horizontalSize, containerSize.width);
                    verticalSize += containerSize.height;
                }
            }

            height = Math.max(height, verticalSize);
            width += horizontalSize;
        }

        return new Size(width, height);
    }

    @Override
    public void onExport(ReportContext context, Point point) {

    }
}
