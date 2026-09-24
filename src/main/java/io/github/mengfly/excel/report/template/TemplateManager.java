package io.github.mengfly.excel.report.template;

import io.github.mengfly.excel.report.exception.TemplateNotFoundException;
import io.github.mengfly.excel.report.template.factory.ClasspathTemplateFactory;
import io.github.mengfly.excel.report.template.factory.TemplateFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Getter
@Setter
public class TemplateManager {

    @Getter
    private static final TemplateManager instance = new TemplateManager();

    /**
     * 模板工厂
     */
    private volatile TemplateFactory templateFactory = new ClasspathTemplateFactory();

    /**
     * 解析结果缓存：同一个模板只解析一次
     * <p>
     * key 为模板 id（不含工厂补全的 .xml 后缀，与原样传入的 id 一致）。
     */
    private final Map<String, ReportTemplate> templateCache = new ConcurrentHashMap<>();

    /**
     * 获取模板（带缓存）
     *
     * @param id 模板Id
     * @return 模板信息
     * @throws TemplateNotFoundException 如果找不到模板，抛出异常
     */
    public ReportTemplate getTemplate(String id) throws TemplateNotFoundException {
        if (id == null) {
            throw new TemplateNotFoundException(null);
        }
        // computeIfAbsent 对同一 key 加锁，不同 key 可并发解析；映射函数抛出的异常会原样传播
        return templateCache.computeIfAbsent(id, key -> {
            final ReportTemplate template = templateFactory.getTemplate(key);
            template.setTemplateManager(this);
            return template;
        });
    }

    /**
     * 清除全部模板缓存
     * <p>
     * 模板文件在运行期发生变化时（例如开发期热更新）可调用此方法强制重新解析。
     */
    public void clearCache() {
        templateCache.clear();
    }

    /**
     * 清除指定模板的缓存
     *
     * @param id 模板Id
     */
    public void clearCache(String id) {
        if (id != null) {
            templateCache.remove(id);
        }
    }


}
