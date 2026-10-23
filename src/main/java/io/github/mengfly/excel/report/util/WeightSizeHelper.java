package io.github.mengfly.excel.report.util;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.style.CellStyles;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class WeightSizeHelper {
    public static final int SIZE_TYPE_WIDTH = 1;
    public static final int SIZE_TYPE_HEIGHT = 2;

    private final int sizeType;

    /*
     * 分配结果：构造期一次算完，distributionSize 只查表。
     *
     * 2026-10-23 变更：原实现是"顺序调用、每次扣减剩余空间"的有状态计算，且弹性子节点只按 weight 均分剩余，
     * 完全不看子节点自身内容尺寸 —— 内容多的子节点分不够就静默溢出（导出阶段才因合并区重叠抛异常）。
     * 现改为 flex-basis: content 语义：先保底内容尺寸，剩余空间再按 weight 分配；剩余不足时按内容比例压缩。
     * 内容尺寸打平时新旧结果完全等价（b + (avail - n·b)/n == avail/n），行为差异只落在旧算法规必分错的场景。
     */
    private final Map<Container, Integer> allocation = new IdentityHashMap<>();

    /*
     * 弹性子节点的内容尺寸与权重：getSize() 对布局类子节点是递归累加（Table 还要重建整棵合成布局），
     * 构造期各算一次并缓存，避免同一子节点在分配与告警里反复求值。
     */
    private final Map<Container, Integer> basis = new IdentityHashMap<>();
    private final Map<Container, Double> weight = new IdentityHashMap<>();

    public WeightSizeHelper(Size totalSize, List<Container> children, int sizeType) {
        this.sizeType = sizeType;
        final int available = sizeType == SIZE_TYPE_WIDTH ? totalSize.width : totalSize.height;

        int fixedSizeSum = 0;
        int basisSum = 0;
        double weightSum = 0;
        final List<Container> flexChildren = new ArrayList<>();
        for (Container child : children) {
            final Integer size = getChildSize(child);
            if (size >= 0) {
                allocation.put(child, size);
                fixedSizeSum += size;
                continue;
            }
            basis.put(child, contentSizeOf(child, sizeType));
            weight.put(child, getContainerWeight(child));
            flexChildren.add(child);
            basisSum += basis.get(child);
            weightSum += weight.get(child);
        }

        final int remaining = available - fixedSizeSum;
        if (remaining >= basisSum) {
            distributeSurplus(flexChildren, remaining - basisSum, weightSum);
        } else {
            distributeDeficit(flexChildren, Math.max(remaining, 0), basisSum);
        }
    }

    /*
     * 空间够放：内容尺寸保底，剩余按 weight 分。
     * 顺序扣减 surplus/weightSum，让取整余数自然落到最后一个弹性子节点（与原实现的取整行为一致）。
     */
    private void distributeSurplus(List<Container> flexChildren, int surplus, double weightSum) {
        for (Container child : flexChildren) {
            final double childWeight = weight.get(child);
            final int extra = (int) (surplus * childWeight / weightSum);
            allocation.put(child, basis.get(child) + extra);
            surplus -= extra;
            weightSum -= childWeight;
        }
    }

    /*
     * 空间不够：按各自内容尺寸等比压缩，最小 1。物理上放不下，上层据"分配 < 内容"给出告警。
     */
    private void distributeDeficit(List<Container> flexChildren, int available, int basisSum) {
        for (Container child : flexChildren) {
            final int childBasis = basis.get(child);
            int size = (int) ((long) available * childBasis / basisSum);
            if (size < 1) {
                size = 1;
            }
            allocation.put(child, size);
            available -= size;
            basisSum -= childBasis;
        }
    }

    /**
     * 子节点在该轴上的内容尺寸
     * <p>
     * 布局类子节点的 {@code getSize()} 本身就是子元素累加值；叶子组件是声明值；
     * 声明为负（弹性叶子，内容不可知）时按最小 1 计。
     *
     * @param child    子节点
     * @param sizeType {@link #SIZE_TYPE_WIDTH} / {@link #SIZE_TYPE_HEIGHT}
     * @return 内容尺寸，至少 1
     */
    public static int contentSizeOf(Container child, int sizeType) {
        final int value = axisOf(child.getSize(), sizeType);
        return value < 1 ? 1 : value;
    }

    /**
     * 子节点在该轴上的尺寸是否由权重弹性分配
     * <p>
     * 判定口径与分配完全一致：声明的 {@code size} 该轴为负，或未声明但 {@code getSize()} 该轴为负。
     *
     * @param child    子节点
     * @param sizeType {@link #SIZE_TYPE_WIDTH} / {@link #SIZE_TYPE_HEIGHT}
     * @return 弹性分配返回 true
     */
    public static boolean isFlexSize(Container child, int sizeType) {
        final Size preferredSize = child.getStyle(CellStyles.preferredSize, null);
        if (preferredSize != null && axisOf(preferredSize, sizeType) < 0) {
            return true;
        }
        return axisOf(child.getSize(), sizeType) < 0;
    }

    private static int axisOf(Size size, int sizeType) {
        return sizeType == SIZE_TYPE_WIDTH ? size.width : size.height;
    }

    private Integer getChildSize(Container child) {
        if (isFlexSize(child, sizeType)) {
            return -1;
        }
        return axisOf(child.getSize(), sizeType);
    }

    /**
     * 为组件分配尺寸
     * <p>
     * 分配在构造期一次算完，重复调用返回同一结果（不会像旧实现那样每次调用继续扣减剩余空间）。
     *
     * @param container 组件（必须是构造时传入的 children 之一）
     * @return 尺寸；未知组件返回 0
     */
    public int distributionSize(Container container) {
        final Integer size = allocation.get(container);
        return size == null ? 0 : size;
    }

    private double getContainerWeight(Container child) {
        final Double weight = child.getStyle(CellStyles.weight, 1.0);
        if (weight <= 0) {
            return 1.0;
        }
        return weight;
    }
}
