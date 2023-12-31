package io.github.mengfly.excel.report.component.image;

import io.github.mengfly.excel.report.component.AbstractComponent;
import io.github.mengfly.excel.report.entity.Point;
import io.github.mengfly.excel.report.entity.Size;
import io.github.mengfly.excel.report.excel.ExcelCellSpan;
import io.github.mengfly.excel.report.excel.ReportContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.xssf.usermodel.XSSFDrawing;

@Getter
@Setter
public class ImageComponent extends AbstractComponent {

    private Size size = new Size(4, 10);
    private Image image;

    @Override
    public void onExport(ReportContext context, Point point) {
        final ExcelCellSpan cellSpan = context.getCellSpan(point, size).merge();

        if (image != null) {
            try {
                final XSSFDrawing patriarch = context.createDrawingPatriarch();

                int pictureIndex = context.addPicture(image);

                patriarch.createPicture(cellSpan.getFillAnchor(), pictureIndex);
            } catch (Exception e) {
                cellSpan.setValue(e.getMessage());
            }
        }
    }
}
