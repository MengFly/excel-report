package io.github.mengfly.excel.report.component.image;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Border;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;

import java.awt.*;

@Getter
@Setter
public class ImageComponent extends AbstractComponent {

    private Size size = new Size(4, 10);
    private Image image;
    private ScaleType scaleType = ScaleType.FIT_XY;
    private ClientAnchor.AnchorType anchorType;
    private String padding = "2,2,2,2";
    /**
     * 高度缩放比例（再原基础上进行高度的适配）
     */
    private Double scaleHeight = 1.0;

    @Override
    public void onExport(ReportContext context, Point point, Size suggestSize) {

        final ExcelCellSpan cellSpan = context.getCellSpan(point, suggestSize).merge();


        if (image != null) {
            try {
                final XSSFDrawing patriarch = context.createDrawingPatriarch();
                int pictureIndex = context.addPicture(image);

                final ClientAnchor fillAnchor = cellSpan.getFillAnchor(anchorType);
                final XSSFPicture picture = patriarch.createPicture(fillAnchor, pictureIndex);
                final Dimension imageDimension = picture.getImageDimension();
                imageDimension.height = (int) (imageDimension.height * scaleHeight);
                context.addOnExportFinalizer(() -> {
                    scaleType.onAnchor(fillAnchor, cellSpan, imageDimension);
                    Border padding = Border.of(getPadding());

                    int top = padding.getTop() * Units.EMU_PER_PIXEL;
                    int bottom = -padding.getBottom() * Units.EMU_PER_PIXEL;

                    int left = padding.getLeft() * Units.EMU_PER_POINT;
                    int right = -padding.getRight() * Units.EMU_PER_POINT;

                    fillAnchor.setDx1(fillAnchor.getDx1() + left);
                    fillAnchor.setDx2(fillAnchor.getDx2() + right);

                    fillAnchor.setDy1(fillAnchor.getDy1() + top);
                    fillAnchor.setDy2(fillAnchor.getDy2() + bottom);
                });

            } catch (Exception e) {
                cellSpan.setValue(e.getMessage());
            }
        }
    }


}
