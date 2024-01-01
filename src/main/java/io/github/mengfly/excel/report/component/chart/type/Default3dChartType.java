package io.github.mengfly.excel.report.component.chart.type;

import cn.hutool.core.collection.CollectionUtil;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;

import java.util.Set;

/**
 * 默认的3D图表类型
 * 默认的图表类型，支持左右两个轴 ，左右两个轴的数据可以是同一个，也可以是不同的
 * <p>
 * 支持的数据图表类型： 折线图，柱状图，饼图，散点图，柱状图，填充折线图
 *
 * @see ChartTypes#AREA3D
 * @see ChartTypes#BAR3D
 * @see ChartTypes#LINE3D
 */
public class Default3dChartType extends DefaultChartDataType {

    @Override
    public Set<ChartTypes> supportChartTypes() {
        return CollectionUtil.newHashSet(ChartTypes.AREA3D, ChartTypes.BAR3D, ChartTypes.LINE3D);
    }
}
