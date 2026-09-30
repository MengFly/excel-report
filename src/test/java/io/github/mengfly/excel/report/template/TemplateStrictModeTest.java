package io.github.mengfly.excel.report.template;

import io.github.mengfly.excel.report.Container;
import io.github.mengfly.excel.report.exception.TemplateValidationException;
import io.github.mengfly.excel.report.template.validate.TemplateValidator;
import io.github.mengfly.excel.report.template.validate.ValidationIssue;
import io.github.mengfly.excel.report.template.validate.ValidationIssue.Level;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * 严格 / 宽松两种模式的行为。
 * <p>
 * 默认必须<b>宽松</b>：静默失效的行为与历史版本一致，只是多一条汇总 WARN —— 否则存量模板
 * （尤其是没声明命名空间的模板）会在升级依赖后直接跑不起来。严格模式是显式选择。
 * <p>
 * 夹具 {@code BrokenTemplate.xml} 只含一个 ERROR（引用未定义的样式 id），但结构合法、能正常渲染。
 */
public class TemplateStrictModeTest {

    private static final String BROKEN = "BrokenTemplate";

    @Test
    public void defaultModeShouldBeLenient() {
        TemplateManager manager = TemplateManager.getInstance();
        boolean origin = manager.isStrict();
        try {
            manager.setStrict(false);
            manager.clearCache(BROKEN);

            ReportTemplate template = manager.getTemplate(BROKEN);
            Assert.assertFalse("模板问题应当被记录下来", template.validate().isEmpty());
            Assert.assertTrue("夹具的问题里应当有 ERROR", template.hasErrors());

            // 宽松模式下渲染必须照常成功（这是"不破坏存量行为"的关键）
            Container container = template.render(new DataContext());
            Assert.assertNotNull("宽松模式下必须仍能渲染出组件", container);
        } finally {
            manager.setStrict(origin);
            manager.clearCache(BROKEN);
        }
    }

    @Test
    public void strictModeShouldRejectTemplateWithIssues() {
        TemplateManager manager = TemplateManager.getInstance();
        boolean origin = manager.isStrict();
        try {
            manager.setStrict(true);
            manager.clearCache(BROKEN);
            try {
                manager.getTemplate(BROKEN);
                Assert.fail("严格模式下应当抛 TemplateValidationException");
            } catch (TemplateValidationException e) {
                Assert.assertFalse("异常里应带上问题清单", e.getIssues().isEmpty());
                Assert.assertTrue("应包含未定义样式 id 的规则",
                        hasRule(e.getIssues(), TemplateValidator.RULE_STYLE_UNDEFINED_ID));
                Assert.assertTrue("异常消息应含模板 id", e.getMessage().contains(BROKEN));
            }
        } finally {
            manager.setStrict(origin);
            manager.clearCache(BROKEN);
        }
    }

    /**
     * 严格模式在模板<b>已被宽松地缓存之后</b>打开，同样要拦住 —— 否则行为取决于调用顺序。
     */
    @Test
    public void strictModeShouldAlsoApplyToCachedTemplate() {
        TemplateManager manager = TemplateManager.getInstance();
        boolean origin = manager.isStrict();
        try {
            manager.setStrict(false);
            manager.clearCache(BROKEN);
            manager.getTemplate(BROKEN); // 先宽松地缓存

            manager.setStrict(true);
            try {
                manager.getTemplate(BROKEN);
                Assert.fail("已缓存的模板在严格模式下同样应被拦住");
            } catch (TemplateValidationException expected) {
                Assert.assertTrue(expected.getIssues().size() > 0);
            }
        } finally {
            manager.setStrict(origin);
            manager.clearCache(BROKEN);
        }
    }

    /** 合法模板在严格模式下也必须正常通过。 */
    @Test
    public void strictModeShouldAcceptValidTemplate() {
        TemplateManager manager = TemplateManager.getInstance();
        boolean origin = manager.isStrict();
        try {
            manager.setStrict(true);
            manager.clearCache("TestTemplate");
            ReportTemplate template = manager.getTemplate("TestTemplate");
            Assert.assertFalse("自带模板不应有问题: " + template.validate(), template.hasErrors());
            Assert.assertEquals(0, countErrors(template.validate()));
        } finally {
            manager.setStrict(origin);
            manager.clearCache("TestTemplate");
        }
    }

    private static boolean hasRule(List<ValidationIssue> issues, String ruleId) {
        for (ValidationIssue issue : issues) {
            if (ruleId.equals(issue.getRuleId())) {
                return true;
            }
        }
        return false;
    }

    private static int countErrors(List<ValidationIssue> issues) {
        int count = 0;
        for (ValidationIssue issue : issues) {
            if (issue.getLevel() == Level.ERROR) {
                count++;
            }
        }
        return count;
    }
}
