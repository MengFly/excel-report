package io.github.mengfly.excel.report.util;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;

import java.util.List;

public class WeightSizeHelper {
    public static final int SIZE_TYPE_WIDTH = 1;
    public static final int SIZE_TYPE_HEIGHT = 2;

    /**
     * 等待要被分配的尺寸
     */
    private int waitForDistributionSize;
    /**
     * 等待要被分配的权重
     */
    private double waitForDistributionWeight = 0;

    private final int sizeType;


    public WeightSizeHelper(Size totalSize, List<Container> children, int sizeType) {
        this.sizeType = sizeType;

        waitForDistributionSize = sizeType == SIZE_TYPE_WIDTH ? totalSize.width : totalSize.height;

        for (Container child : children) {
            final Integer size = getChildSize(child);
            // 固定大小尺寸的组件
            if (size > 0) {
                waitForDistributionSize -= size;
            } else if (size < 0) {
                // 非固定大小的组件
                double weight = getContainerWeight(child);
                waitForDistributionWeight += weight;
            }
        }
    }

    private Integer getChildSize(Container child) {
        final Size size = child.getStyle(CellStyles.preferredSize, null);
        if (sizeType == SIZE_TYPE_WIDTH) {
            if (size != null && size.width < 0) {
                return -1;
            }
            return child.getSize().width;
        } else {
            if (size != null && size.height < 0) {
                return -1;
            }
            return child.getSize().height;
        }

    }

    /**
     * 为组件分配尺寸
     *
     * @param container 组件
     * @return 尺寸
     */
    public int distributionSize(Container container) {
        final Integer size = getChildSize(container);
        if (size >= 0) {
            return size;
        }
        // 获取权重
        final double containerWeight = getContainerWeight(container);

        // 获取组件应该分配的尺寸(最小分配尺寸为1)
        int needDistributionSize = (int) (waitForDistributionSize * containerWeight / waitForDistributionWeight);
        if (needDistributionSize < 1) {
            needDistributionSize = 1;
        }

        waitForDistributionWeight -= containerWeight;
        waitForDistributionSize -= needDistributionSize;

        return needDistributionSize;
    }

    private double getContainerWeight(Container child) {
        final Double weight = child.getStyle(CellStyles.weight, 1.0);
        if (weight <= 0) {
            return 1.0;
        }
        return weight;
    }
}
