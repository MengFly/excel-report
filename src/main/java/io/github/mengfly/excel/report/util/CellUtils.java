package io.github.mengfly.excel.report.util;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.poi.util.Units.EMU_PER_PIXEL;
import static org.apache.poi.util.Units.EMU_PER_POINT;

public class CellUtils {

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

        double emu;
        if (isRow) {
            emu = EMU_PER_POINT;
        } else {
            emu = EMU_PER_PIXEL;
        }

        double endDval = delta * emu;

        if (cellIdx == startCell) {
            endDval += startD;
        }

        endCell.accept(cellIdx);
        endD.accept((int) Math.rint(endDval));
    }
}
