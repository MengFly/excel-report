package io.github.mengfly.excel.report.component.chart.axis;

import io.github.mengfly.excel.report.component.chart.AxisType;
import io.github.mengfly.excel.report.component.chart.data.AxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.ChartValueAxisData;
import io.github.mengfly.excel.report.component.chart.data.RelationAxisDataResolver;
import io.github.mengfly.excel.report.component.chart.data.ValueAxisDataResolver;
import lombok.Getter;
import lombok.Setter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChartValueAxis extends ChartAxis {

    private boolean lineSmooth;
    private List<ChartValueAxisData> dataList = new ArrayList<>();

    @Override
    public AxisType getType() {
        return AxisType.VALUE;
    }

    public ChartValueAxisData addData(ChartValueAxisData data) {
        this.dataList.add(data);
        return data;
    }

    public ChartValueAxisData addData(AxisDataResolver data) {
        return addData(new ChartValueAxisData(data));
    }

    public ChartValueAxisData addData(List<? extends Number> data) {
        return this.addData(new ChartValueAxisData(new ValueAxisDataResolver(data)));
    }

    public ChartValueAxisData addData(CellRangeAddress addresses) {
        return this.addData(new ChartValueAxisData(new RelationAxisDataResolver(addresses)));
    }

    @Override
    public void initAxisStyle(XDDFChartAxis axis) {
        super.initAxisStyle(axis);
    }
}
