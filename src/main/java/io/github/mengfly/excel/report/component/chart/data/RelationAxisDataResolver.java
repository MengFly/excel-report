package io.github.mengfly.excel.report.component.chart.data;

import io.github.mengfly.excel.report.component.chart.AxisType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelationAxisDataResolver implements AxisDataResolver {

    private CellRangeAddress address;

    @Override
    public int dataCount() {
        return (address.getLastColumn() - address.getFirstColumn() + 1) * (address.getLastRow() - address.getFirstRow() + 1);
    }

    @Override
    public XDDFDataSource<?> createDataSource(ChartDataContext context) {

        if (context.getAxis().getType() == AxisType.VALUE) {
            return XDDFDataSourcesFactory.fromNumericCellRange(context.getSheet(), address);
        } else {
            return XDDFDataSourcesFactory.fromStringCellRange(context.getSheet(), address);
        }
    }
}
