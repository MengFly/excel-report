package io.github.mengfly.excel.report.component.chart.data;

import cn.hutool.core.convert.Convert;
import io.github.mengfly.excel.report.component.chart.AxisType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ValueAxisDataResolver implements AxisDataResolver {

    private List<?> dataList = new ArrayList<>();


    @Override
    public int dataCount() {
        return dataList.size();
    }

    @Override
    public XDDFDataSource<?> createDataSource(ChartDataContext context) {

        if (context.getAxis().getType() == AxisType.VALUE) {
            return XDDFDataSourcesFactory.fromArray(convertNumber(dataList));
        } else {
            return XDDFDataSourcesFactory.fromArray(convertString(dataList));
        }
    }

    private String[] convertString(List<?> dataList) {
        return dataList.stream().map(Object::toString).toArray(String[]::new);
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    private Number[] convertNumber(List<?> dataList) {
        return dataList.stream().map(obj -> {
            if (obj instanceof Number) {
                return obj;
            }
            try {
                final Number number = Convert.toNumber(obj);
                if (number == null) {
                    return 0;
                }
                return number;
            } catch (Exception e) {
                return 0.;
            }
        }).toArray(Number[]::new);
    }
}
