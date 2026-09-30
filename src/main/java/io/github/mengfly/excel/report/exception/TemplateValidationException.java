package io.github.mengfly.excel.report.exception;

import io.github.mengfly.excel.report.template.validate.ValidationIssue;

import java.util.Collections;
import java.util.List;

/**
 * 严格模式下模板校验不通过时抛出。
 * <p>
 * 默认（宽松）模式不会抛这个异常，问题只以日志汇总的形式出现；
 * 见 {@code TemplateManager#setStrict(boolean)}。
 *
 * @see ValidationIssue
 */
public class TemplateValidationException extends ExcelReportException {

    private static final long serialVersionUID = 1L;

    /**
     * 异常消息里最多列出多少条问题，避免超长堆栈。
     */
    private static final int MAX_PRINTED = 10;

    private final List<ValidationIssue> issues;

    public TemplateValidationException(String templateId, List<ValidationIssue> issues) {
        super(buildMessage(templateId, issues));
        this.issues = issues == null ? Collections.<ValidationIssue>emptyList() : issues;
    }

    /**
     * @return 校验发现的问题（至少一条）
     */
    public List<ValidationIssue> getIssues() {
        return issues;
    }

    private static String buildMessage(String templateId, List<ValidationIssue> issues) {
        List<ValidationIssue> list = issues == null ? Collections.<ValidationIssue>emptyList() : issues;
        long errors = 0;
        for (ValidationIssue issue : list) {
            if (issue.getLevel() == ValidationIssue.Level.ERROR) {
                errors++;
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("模板校验未通过：").append(templateId)
                .append("（").append(errors).append(" 个错误 / ").append(list.size()).append(" 条问题）");
        int printed = 0;
        for (ValidationIssue issue : list) {
            if (printed++ >= MAX_PRINTED) {
                builder.append("\n  ... 还有 ").append(list.size() - MAX_PRINTED).append(" 条");
                break;
            }
            builder.append("\n  ").append(issue);
        }
        return builder.toString();
    }
}
