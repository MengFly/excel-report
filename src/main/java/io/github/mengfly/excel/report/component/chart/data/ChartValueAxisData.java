package io.github.mengfly.excel.report.component.chart.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;

@Getter
@Setter
@Accessors(chain = true)
public class ChartValueAxisData extends ChartAxisData<XDDFNumericalDataSource<?>> {

    private ChartTypes type;

    public ChartValueAxisData(AxisDataResolver resolver) {
        super(resolver);
    }
}
