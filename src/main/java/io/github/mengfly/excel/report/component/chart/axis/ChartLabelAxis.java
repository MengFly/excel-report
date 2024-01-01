package io.github.mengfly.excel.report.component.chart.axis;

import io.github.mengfly.excel.report.component.chart.AxisType;
import io.github.mengfly.excel.report.component.chart.data.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.List;

@Getter
@Setter
public class ChartLabelAxis extends ChartAxis {
    private AxisType type;
    private ChartLabelAxisData data;
    //private AxisLabelAlignment labelAlignment;

    @Override
    public void initAxisStyle(XDDFChartAxis axis) {
        super.initAxisStyle(axis);
        //if (labelAlignment != null && axis instanceof XDDFCategoryAxis) {
        //    ((XDDFCategoryAxis) axis).setLabelAlignment(labelAlignment);
        //}
    }

    public XDDFDataSource<?> createDataSource(XSSFSheet sheet) {
        return getData().createDataSource(new ChartDataContext(sheet, this));
    }


    public ChartLabelAxisData setData(AxisDataResolver resolver) {
        this.data = new ChartLabelAxisData(resolver);
        return this.data;
    }

    public ChartLabelAxisData setData(List<?> data) {
        return setData(new ValueAxisDataResolver(data));
    }

    public ChartLabelAxisData setData(CellRangeAddress addresses) {
        return setData(new RelationAxisDataResolver(addresses));
    }
}
