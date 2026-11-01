package io.github.mengfly.excel.report.util;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.poi.util.Units.EMU_PER_PIXEL;

public class CellUtils {

    /**
     * 把"距起点单元格的偏移量"换算成 (单元格索引, 单元格内偏移 EMU)，用于设置 ClientAnchor 的 col/dx 或 row/dy。
     * <p>
     * {@code targetSize} 必须与 {@code nextSize} 返回的值<b>同量纲</b>；本方法统一按像素折算 EMU
     * （{@code EMU_PER_PIXEL}），所以调用方要么两侧都给像素，要么两侧都先换算到像素。
     *
     * @param targetSize 目标尺寸（与 nextSize 同量纲）
     * @param startCell  起始单元格索引
     * @param startD     起始单元格内的已有偏移（EMU）
     * @param endCell    结果：结束单元格索引
     * @param endD       结果：结束单元格内偏移（EMU）
     * @param nextSize   按单元格索引取该格尺寸（与 targetSize 同量纲）
     * @param isRow      是否为行方向。<b>仅为签名兼容保留，不再参与单位选择</b>
     */
    public static void scaleCell(final double targetSize,
                                 final int startCell,
                                 final int startD,
                                 Consumer<Integer> endCell,
                                 Consumer<Integer> endD,
                                 Function<Integer, Number> nextSize,
                                 boolean isRow) {
        if (targetSize < 0) {
            throw new IllegalArgumentException("target size < 0");
        }
        if (Double.isInfinite(targetSize) || Double.isNaN(targetSize)) {
            throw new IllegalArgumentException("target size " + targetSize + " is not supported");
        }

        int cellIdx = startCell;
        double dim, delta;
        for (double totalDim = 0, remDim; ; cellIdx++, totalDim += remDim) {
            dim = nextSize.apply(cellIdx).doubleValue();
            remDim = dim;
            if (cellIdx == startCell) {
                remDim -= startD;
            }
            delta = targetSize - totalDim;
            if (delta < remDim) {
                break;
            }
        }

        // 列与行统一按像素折算 EMU。历史实现行方向用 EMU_PER_POINT（磅），而 ScaleType 喂进来的
        // 纵向目标值是「图片像素 × scale」，同一串数字在横向当 px、纵向当 pt ⇒ 纵向被放大 4/3。
        // isRow 因此失去意义，保留只是为了避免破坏公开签名。
        final double emu = EMU_PER_PIXEL;

        double endDval = delta * emu;

        if (cellIdx == startCell) {
            endDval += startD;
        }

        endCell.accept(cellIdx);
        endD.accept((int) Math.rint(endDval));
    }
}
