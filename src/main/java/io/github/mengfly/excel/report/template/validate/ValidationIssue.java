package io.github.mengfly.excel.report.template.validate;

import lombok.Getter;

/**
 * 模板校验发现的一条问题。
 * <p>
 * 框架长期是"静默"的：标签 / 属性 / 样式 key 写错不报错，只被丢弃或忽略。
 * 本类把这些静默失效变成可汇集的条目，供 {@link TemplateValidator} 返回、
 * 供 {@code ReportTemplate#validate()} 汇总，也供严格模式决定是否中断。
 *
 * @see TemplateValidator
 */
@Getter
public class ValidationIssue {

    public enum Level {
        /**
         * 会导致模板行为与预期不符：内容被静默丢弃、静默回退，或只渲染了一部分。
         */
        ERROR,
        /**
         * 多半不是本意，但运行期有确定的兜底行为（例如颜色名不认识就回退成黑色）。
         */
        WARN
    }

    /**
     * 规则标识，用于过滤、忽略清单以及与离线脚本对拍。
     */
    private final String ruleId;

    private final Level level;

    /**
     * 出问题的节点路径（形如 {@code template/container/VLayout/Text[2]@style}）。
     */
    private final String path;

    private final String message;

    public ValidationIssue(Level level, String ruleId, String path, String message) {
        this.level = level;
        this.ruleId = ruleId;
        this.path = path;
        this.message = message;
    }

    @Override
    public String toString() {
        return "[" + level + "] " + ruleId + " @ " + path + ": " + message;
    }
}
