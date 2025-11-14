package io.github.mengfly.excel.report.layout;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.AlignPolicy;
import io.github.mengfly.excel.report.entity.Orientation;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

@Getter
@Setter
public class GridLayout extends AbstractLayout {

    /**
     * 横向或者纵向
     */
    private Orientation orientation;

    private AlignPolicy alignPolicy;
    /**
     * 行数或列数
     * <p>
     * 如果方向为横向，则表示列数
     * 如果方向为纵向，则表示行数
     */
    private int count = 1;

    @Override
    public Size getSize() {
        return getContainerGrop().getSize();
    }


    @Override
    public void onMeasure(Size suggestSize) {
        measuredSize = suggestSize;
        getContainerGrop().onMeasure(suggestSize);
    }

    @Override
    public void onLayout(Point relativePosition) {
        position = relativePosition;
        val containerGrop = getContainerGrop();
        containerGrop.onMeasure(measuredSize);
        containerGrop.onLayout(relativePosition);
    }

    private Layout createRootLayout() {

        if (orientation == Orientation.HORIZONTAL) {
            return new VLayout() {{
                setAlignPolicy(alignPolicy);
            }};
        } else {
            return new HLayout() {{
                setAlignPolicy(alignPolicy);
            }};
        }
    }

    private Layout createGroupLayout() {
        if (orientation == Orientation.HORIZONTAL) {
            return new HLayout() {{
                setAlignPolicy(alignPolicy);
            }};
        } else {
            return new VLayout() {{
                setAlignPolicy(alignPolicy);
            }};
        }
    }

    private Layout getContainerGrop() {
        Layout rootLayout = createRootLayout();

        Layout groupLayout = createGroupLayout();
        for (Container container : containers) {
            groupLayout.addItem(container);
            if (groupLayout.getContainers().size() >= count) {
                rootLayout.addItem(groupLayout);
                groupLayout = createGroupLayout();
            }
        }
        if (!groupLayout.getContainers().isEmpty()) {
            rootLayout.addItem(groupLayout);
        }
        return rootLayout;
    }

}
