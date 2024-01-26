package io.github.mengfly.excel.report.component.image;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.usermodel.ClientAnchor;
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

    @Override
    public void onExport(ReportContext context, Point point) {
        final ExcelCellSpan cellSpan = context.getCellSpan(point, size).merge();


        if (image != null) {
            try {
                final XSSFDrawing patriarch = context.createDrawingPatriarch();
                int pictureIndex = context.addPicture(image);

                final ClientAnchor fillAnchor = cellSpan.getFillAnchor(anchorType);
                final XSSFPicture picture = patriarch.createPicture(fillAnchor, pictureIndex);
                final Dimension imageDimension = picture.getImageDimension();
                context.addOnExportFinalizer(() -> scaleType.onAnchor(fillAnchor, cellSpan, imageDimension));

            } catch (Exception e) {
                cellSpan.setValue(e.getMessage());
            }
        }
    }


}
