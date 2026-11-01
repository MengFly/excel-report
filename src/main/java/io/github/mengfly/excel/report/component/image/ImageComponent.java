package io.github.mengfly.excel.report.component.image;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Border;
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
    public void onExport(ReportContext context) {

        final ExcelCellSpan cellSpan = context.getCellSpan(getPosition(), getMeasuredSize()).merge();


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

                    // padding 的单位是像素，四个方向都按 EMU_PER_PIXEL 折算 —— 这是"唯一一把尺子"。
                    // 历史实现纵向用 EMU_PER_PIXEL、横向用 EMU_PER_POINT：纵向恰好正确
                    // （2px × EMU_PER_PIXEL ≡ 2px→1.5pt × EMU_PER_POINT），横向 2px 会被渲染成 2.67px。
                    int top = padding.getTop() * Units.EMU_PER_PIXEL;
                    int bottom = -padding.getBottom() * Units.EMU_PER_PIXEL;

                    int left = padding.getLeft() * Units.EMU_PER_PIXEL;
                    int right = -padding.getRight() * Units.EMU_PER_PIXEL;

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
