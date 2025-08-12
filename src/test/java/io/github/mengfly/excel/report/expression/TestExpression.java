package io.github.mengfly.excel.report.expression;

import io.github.mengfly.excel.report.template.exepression.ExpressionHelper;
import org.junit.Test;

public class TestExpression {

    @Test
    public void testExpression() {
        createExpression("${areaVo.getEx > 1 ${areaVo.getEx}");
        createExpression("${areaVo.");
        createExpression("${areaVo.getEx}");
        createExpression("${areaVo.getEx > 1 ? areaVo.getEx : areaVo.getEx2}");
        createExpression("${areaVo.getE} dsefe ${area} ${edds}");
        createExpression("${areaVo.getE} dsefe $area} ${edds}");
        createExpression("${areaVo.getE} dsefe ${area ${edds}");
    }

    private void createExpression(String expression) {
        System.out.println(expression);
        System.out.println(ExpressionHelper.createExpression(expression));
        System.out.println();
    }
}
