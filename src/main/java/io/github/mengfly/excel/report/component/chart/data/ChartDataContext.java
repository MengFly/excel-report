package io.github.mengfly.excel.report.component.chart.data;

import io.github.mengfly.excel.report.component.chart.axis.ChartAxis;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFSheet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataContext {
    private XSSFSheet sheet;
    private ChartAxis axis;
}
