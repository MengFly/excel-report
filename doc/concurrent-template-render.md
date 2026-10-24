---
date: 2026-10-24
description: 修复同一模板被并发导出时，共享 DOM 惰性缓存竞态导致静默缺块/错位的问题
complete: true
---

# 同一模板并发导出静默渲染错误 - 任务规划

## 1. 需求概述

`TemplateManager` 是单例，`ReportTemplate` 按模板 id 缓存后被所有线程共享。`ReportTemplate` 内部持有的 DOM（`ContainerTreeNode.element`）在 `render()` 期间被并发遍历，而 JDK 8 自带 Xerces 的 DOM 节点上带有**非线程安全的惰性缓存**：

- `ParentNode.fNodeListCache`：`getChildNodes().item()` / `getLength()` 共用；
- `ParentNode.fBufferStr`：`getTextContent()` 共用；
- 延迟展开节点（`DeferredElementImpl`）的子节点列表。

并发遍历会抛 `NPE @ ParentNode.nodeListItem` / `IllegalArgumentException`，而异常被 `ProcessControl.fetchList` 的 catch 吞掉，最终导出文件仍然生成、但内容缺块或错位，调用方无感知。

目标：同一模板并发导出结果正确，且不改变公开 API、不改变现有宽松异常策略（用户已确认）。

## 2. 需求澄清记录

### 2.1 关键实测（决定了方案选择）

用 JDK `1.8.0_231` 对 `src/test/resources/Example1Template.xml` 做一次性探针（8 线程 × 2000 次递归遍历，探针放在 `target/probe/`，不入库）：

| 场景 | 结果 |
|---|---|
| 默认（`defer-node-expansion=true`）+ `childNodes` 遍历 | `errors=7`（`IllegalArgumentException: type: -1` / NPE） |
| **关闭延迟展开**（`defer-node-expansion=false`）+ `childNodes` 遍历 | **`errors=7`（仍为 `NPE @ ParentNode.nodeListItem`）** |
| 默认 + 对遍历加锁 | `errors=0` |
| 关闭延迟展开 + 改用 `getFirstChild()/getNextSibling()` 遍历 | `errors=0` |

**结论：需求里建议的"仅关闭 XML 延迟节点展开"不足以解决问题**——`fNodeListCache` 与延迟展开无关，关闭后 childNodes 遍历依旧竞态。因此采用"渲染入口加锁"方案。

### 2.2 与用户的确认结果

| 问题 | 用户选择 | 说明 |
|---|---|---|
| 并发安全实现方式 | **渲染入口加锁** | 在 `ReportTemplate.render` 内按模板实例串行化 DOM 遍历；不引入"关闭延迟展开 + 改造遍历"的额外改动 |
| 渲染期异常策略 | **保持现状** | 只修并发；`ProcessControl` / `IfExpression` / `ForExpression` 的现有 `log.error` + 跳过行为不变，避免破坏存量"部分渲染"用法 |
| 回归用例 | **不写** | 不新增 `src/test` 用例；验证依赖现有测试套件 + 一次性探针（`target/` 下，不入库） |

### 2.3 锁的边界确认

- **必须在锁内**：`render()` → `ContainerTreeNode.render()` → `ParserFactory` → 各 Parser 对 `element` 的读取（`getChildNodes` / `getElementsByTagName` / `getAttribute` / `getStyle` / `listChild` 等）。
- **不需要在锁内**：`ExcelReport.exportSheet` 的 `onMeasure/onLayout/export`。其中仅有的后续 DOM 读取是 `AbstractLayout.nodePath()` 的 `getAttribute/getTagName` 与 `WebJsonExporter.resolveType()` 的 `getTagName`，均为字段/只读集合读取，不触发上述缓存。
- **不同模板**：各自持有锁，互不阻塞；`IncludeParser` 递归渲染的是另一个 `ReportTemplate` 实例，正常模板不会形成环（自包含/互包含本身即无限递归，属既有问题）。
- **死锁风险**：同一线程内 `synchronized` 可重入；跨模板的包含环在单线程也会栈溢出，不因加锁新增可用性。

## 3. 任务列表

### 任务 1：在 `ReportTemplate.render` 增加实例级渲染锁

| 属性 | 值 |
|------|-----|
| **描述** | 在 `src/main/java/io/github/mengfly/excel/report/template/ReportTemplate.java` 中新增一个私有实例锁字段，并把 `logIssuesOnce()` + `rootNode.render(context)` 整体放进锁内。锁字段不能被 Lombok 类级 `@Getter` 暴露，需用 `@Getter(AccessLevel.NONE)` 排除。用 `//` 注释写明：JDK 8 Xerces 的 `fNodeListCache`/`fBufferStr` 不是线程安全的、仅关 `defer-node-expansion` 实测无效、POI 导出阶段不在锁内；`/** */` 保持"给调用方看的契约"，只补充 `render` 可并发调用的说明。 |
| **依赖关系** | 无 |
| **验收标准** | ① `render()` 内所有 DOM 读取均位于同一实例锁内；② 未新增/修改任何 public 方法签名，插件调用方式不变；③ 临时探针（`target/` 下，不入库）8 线程并发 `render` 同一模板 0 异常，且各线程返回的容器结构摘要一致；④ 现有测试全绿。 |

### 任务 2：更新优化台账与"易踩点"

| 属性 | 值 |
|------|-----|
| **描述** | 更新 `doc/优化建议.md`：① 一、状态总览中"二.1"行补充"已用实例锁串行化同模板并发 render"的结论；② 三、备查"二.1"把"多线程请各自持有模板实例"改为"已修复，可共享"；③ 四、易踩点中"同一个 `ReportTemplate` 实例不能并发 `render`"条目改为已修复，并记录"仅关 `defer-node-expansion` 仍竞态"的实测证据与锁的覆盖边界（render 内 DOM / 导出阶段无 DOM 读）。不新增终态条目，不改动已有否决/取消项。 |
| **依赖关系** | 依赖任务 1（需按最终实现与实测结果落笔） |
| **验收标准** | 台账中不再出现"多线程需各自持有模板实例"的结论；新增说明与代码实现、实测数据一致；条目仍保持"不重开"口径，避免被巡检重复提出。 |

### 任务 3：构建与并发验证（不新增测试代码）

| 属性 | 值 |
|------|-----|
| **描述** | 用 JDK 1.8 执行全量测试：`JAVA_HOME=D:\Java\jdk-1.8 mvn -o clean test`（当前默认 `JAVA_HOME` 是 21，需显式指定）。再用 `target/` 下一次性探针（不入库）通过 `TemplateManager#getTemplate` 取同一模板，8 线程各并发 `render` 若干轮，统计异常数与容器结构摘要。 |
| **依赖关系** | 依赖任务 1 |
| **验收标准** | ① `mvn -o clean test` 全部通过；② 探针异常数为 0，各线程结构摘要一致；③ 探针文件不进入 `git status`。 |

## 4. 执行顺序

1. **任务 1**（代码修复）——唯一的阻塞项。
2. **任务 2**（文档台账）与 **任务 3**（构建验证）可在任务 1 完成后并行；任务 2 落笔前以任务 3 的实测结果为准。

## 5. 影响面与约束

- **公开 API**：无变化（`render` 签名、`TemplateManager`、`ReportTemplate` 构造器均不动）。
- **下游插件**：`../excel-report-plugin` 仅调用渲染与预览，不涉及锁字段，零影响；按仓库约定不为其执行编译构建。
- **性能**：只串行化同一模板的 `render`（解析 + 组件树构建）阶段；`ExcelReport.exportSheet` 的 POI 写入（占导出耗时约 97.8%）不在锁内，不同模板之间互不阻塞。
- **异常语义**：按用户确认保持现状，`ProcessControl.fetchList` 等既有 catch 不改。
- **注释规范**：变更原因、实测数据写在代码旁的 `//` 注释中，不写入 `/** */` 契约注释。

## Complements

### 1. 在 `ReportTemplate.render` 增加实例级渲染锁

- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/template/ReportTemplate.java` — 新增私有 `renderLock` 字段（用 `@Getter/@Setter(AccessLevel.NONE)` 排除 Lombok 暴露），`render()` 内的 `logIssuesOnce()` 与 `rootNode.render(context)` 整体放入 `synchronized (renderLock)`；补充"同一实例可并发调用"的契约说明与竞态原因注释。
- **审查结果**：审查通过（锁字段 private/final 未被 Lombok 暴露；`render` 内 DOM 读取全部在锁内；public API 未变；`javap` 确认无 `getRenderLock`）
- **完成时间**：2026-10-24

### 2. 更新 `doc/优化建议.md` 台账与易踩点

- **状态**：✅ 已完成
- **修改文件**：
  - `doc/优化建议.md` — ① 状态总览"二.1"改为"已用实例级渲染锁串行化"并补依据；② 三、备查"二.1"更新为已修复，补充"仅关 `defer-node-expansion` 仍失败（修复前 errors=7，加锁后 0 错误）"的实测；③ 四、易踩点"不能并发 render"条目改为"可并发，但 DOM 不可在锁外遍历"；④ 四.4 历史段落标注"二.1 已修复"并修正"模板实例需各自持有"的表述。
- **审查结果**：审查通过（三处均已落笔、无旧结论残留、终态条目未变、与 `ReportTemplate.java:61-63/160-165` 实现一致）
- **完成时间**：2026-10-24

### 3. 构建与并发验证（不新增测试代码）

- **状态**：✅ 已完成（验收①受存量用例限制：1 个与本次无关的失败，其余 59/59 通过）
- **修改文件**：无（一次性探针位于 `target/probe/`，`target/` 已被 `.gitignore` 忽略，不入库）
- **验证结果**：
  - `JAVA_HOME=D:\Java\jdk-1.8 mvn -o clean test`：60 个用例中 **1 个存量失败** `SimpleTemplateTest#testLayoutReport`——HEAD 中该用例请求 `TestLayoutReport.xlsx.xml`，而资源只有 `TestLayoutReport.xml`，与本次改动无关（`git show HEAD` 已确认测试与资源均未变）。
  - 排除该存量用例后：**59/59 通过**（`mvn -o test -Dtest='!SimpleTemplateTest#testLayoutReport'`）。
  - 真实类并发探针（JDK 1.8.0_231，`TestTemplate` 含 include / 表格 / List / 图表）：8 线程 × 200 次共享 `TemplateManager` 缓存的同一 `ReportTemplate` 并发 `render`，**errors=0，结构签名唯一（1 种）**，耗时 6013 ms。
  - 探针复现命令（`mvn clean` 会删除 `target/probe/`，需先重建探针与 classpath；探针源码见本次会话临时文件，不入库）：
    - `mvn -o -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt`
    - `javac -encoding UTF-8 -cp "target/classes;target/test-classes;$(cat target/cp.txt)" -d target/probe target/probe/TemplateConcurrencyProbe.java`
    - `java -Xmx1g -cp "target/probe;target/classes;target/test-classes;$(cat target/cp.txt)" TemplateConcurrencyProbe`
  - 摘要口径：签名由"容器类名 + 模板标签 + Layout 子节点递归"构成，用于判断 DOM 遍历是否丢子树；**不包含 Table 行内容**，不覆盖单元格级内容比对。
- **审查结果**：审查通过（独立复现 errors=0/签名唯一；复核存量失败与本改动无关；探针已被 `.gitignore` 忽略。文档已按要求补状态标注、复现命令与摘要口径）
- **完成时间**：2026-10-24

### 完成总结

- 3 条任务全部完成并通过审查；公开 API 未变，渲染异常策略未变（按用户确认）。
- 核心改动：`ReportTemplate.render` 按实例加锁，串行化共享模板的 DOM 遍历；`doc/优化建议.md` 的台账与易踩点已同步。
- 验证：JDK 1.8 下 59/59（排除 1 个与本次无关的存量失败）；真实类 8 线程 × 200 次共享模板并发 `render`，errors=0、结构签名唯一。
- 遗留（非本次范围）：`SimpleTemplateTest#testLayoutReport` 请求 `TestLayoutReport.xlsx.xml`，资源实际为 `TestLayoutReport.xml`，属存量失败。




