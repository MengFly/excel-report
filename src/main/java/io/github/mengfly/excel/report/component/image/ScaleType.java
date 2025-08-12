package io.github.mengfly.excel.report.component.image;

import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.util.CellUtils;
import org.apache.poi.ss.usermodel.ClientAnchor;
import java.awt.*;
import java.awt.geom.Dimension2D;

/**
 * 图片的缩放类型
 */
public enum ScaleType {

    FIT_START {
        public void onAnchor(ClientAnchor anchor, ExcelCellSpan cellSpan, Dimension imageDimension) {
            final Dimension2D dimension = cellSpan.cellSpanDimension();

            double scaleWidth = dimension.getWidth() / imageDimension.width;
            double scaleHeight = dimension.getHeight() / imageDimension.height;

            double scale = Math.min(scaleWidth, scaleHeight);

            double targetX = imageDimension.width * scale;
            double targetY = imageDimension.height * scale;

            CellUtils.scaleCell(targetX, anchor.getCol1(), anchor.getDx1(),
                    anchor::setCol2, anchor::setDx2, cellSpan::getColumnWidthInPixels, false);

            CellUtils.scaleCell(targetY, anchor.getRow1(), anchor.getDy1(),
                    anchor::setRow2, anchor::setDy2, cellSpan::getRowHeightInPoints, true);
        }
    },
    FIT_END {
        @Override
        public void onAnchor(ClientAnchor anchor, ExcelCellSpan cellSpan, Dimension imageDimension) {
            final Dimension2D dimension = cellSpan.cellSpanDimension();

            double scaleWidth = dimension.getWidth() / imageDimension.width;
            double scaleHeight = dimension.getHeight() / imageDimension.height;

            double scale = Math.min(scaleWidth, scaleHeight);

            double targetX = dimension.getWidth() - imageDimension.width * scale;
            double targetY = dimension.getHeight() - imageDimension.height * scale;

            CellUtils.scaleCell(targetX, anchor.getCol1(), 0,
                    anchor::setCol1, anchor::setDx1, cellSpan::getColumnWidthInPixels, false);

            CellUtils.scaleCell(targetY, anchor.getRow1(), 0,
                    anchor::setRow1, anchor::setDy1, cellSpan::getRowHeightInPoints, true);
        }
    },
    FIT_XY,
    CENTER {
        @Override
        public void onAnchor(ClientAnchor anchor, ExcelCellSpan cellSpan, Dimension imageDimension) {

            final Dimension2D dimension = cellSpan.cellSpanDimension();

            int startRow = anchor.getRow1();
            int startCol = anchor.getCol1();

            double scaleWidth = dimension.getWidth() / imageDimension.width;
            double scaleHeight = dimension.getHeight() / imageDimension.height;

            double scale = Math.min(scaleWidth, scaleHeight);

            final double targetImageWidth = imageDimension.width * scale;
            final double targetImageHeight = imageDimension.height * scale;

            double startTargetX = (dimension.getWidth() - targetImageWidth) / 2;
            double startTargetY = (dimension.getHeight() - targetImageHeight) / 2;

            CellUtils.scaleCell(startTargetX, startCol, 0,
                    anchor::setCol1, anchor::setDx1, cellSpan::getColumnWidthInPixels, false);

            CellUtils.scaleCell(startTargetY, startRow, 0,
                    anchor::setRow1, anchor::setDy1, cellSpan::getRowHeightInPoints, true);

            double endTargetX = startTargetX + targetImageWidth;
            double endTargetY = startTargetY + targetImageHeight;

            CellUtils.scaleCell(endTargetX, startCol, 0,
                    anchor::setCol2, anchor::setDx2, cellSpan::getColumnWidthInPixels, false);

            CellUtils.scaleCell(endTargetY, startRow, 0,
                    anchor::setRow2, anchor::setDy2, cellSpan::getRowHeightInPoints, true);
        }
    };


    public void onAnchor(ClientAnchor anchor, ExcelCellSpan cellSpan, Dimension imageDimension) {

    }
}
