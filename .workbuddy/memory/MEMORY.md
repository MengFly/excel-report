# excel-report 项目长期记忆

## 构建与验证

- **Maven 必须用 JDK 8**：`JAVA_HOME=D:\Java\jdk1.8.0_231`。默认 `JAVA_HOME=D:\Java\jdk-21.0.1` 会因 lombok 1.18.20 不兼容 JDK 21 报 `NoSuchFieldError: JCTree$JCImport ... 'qualid'`。
- 常用命令：`mvn -o -q -DskipTests compile`（交付标准 = 编译通过）、`mvn -o -q test-compile`。
- 需要临时验证时，导出依赖 classpath 后写单文件探针跑，不要污染 `src/test`：
  `mvn -o -q dependency:build-classpath -Dmdep.outputFile=<file> -Dmdep.includeScope=test`。
- 用户自行做效果验证/冒烟测试；AI 交付到"编译通过 + 产物就绪"为止。

## 兄弟仓库 excel-report-plugin（`../excel-report-plugin`）

- 该插件复用本框架的渲染链路（`ReportTemplate#render`、`ExpressionHelper` 子类等），**改本框架的公开 API（尤其 `template.*` 包名、类名、方法签名）前必须先 grep 插件源码评估影响**（框架 `AGENTS.md` 的硬要求）。
- 插件依赖 **Maven Central 上发布的固定版本 jar**（`io.github.mengfly:excel-report:1.5.1`，见其 `build.gradle.kts`），
  因此本地源码改名不会立刻打断插件构建，**影响会推迟到插件升级依赖版本时**——改 API 时要在 doc 里写清这条时间差。
- 插件仓库 `AGENTS.md` 明确："**不要编译构建，由我来验证！**" —— 不要为插件执行 gradle 命令。
- 已知待同步项：框架 `exepression` → `expression` 包改名（`6873074`）后，插件 `ExcelReportPreviewEngine.kt:10-12` 三行 import 需在升级依赖时同步。

## 框架代码易踩点

- **`ExcelCellSpan` 的 auto 宽高计算依赖三要素**：cell 的**当前内容 + 当前样式 + 是否已 `merge()`**，且结果靠 `Map.compute(max)` 累积
  ⇒ **调用顺序本身就是语义的一部分**。框架固定顺序是 `getCellSpan`（构造 + `setStyle`，cell 空白、未 merge）→ `merge()` → `setValue()`（终态）。
  已实测：空白未 merge ⇒ `getColumnWidth` 返回 -1；有值已 merge ⇒ 返回"整块文本 ÷ 列数"（5.39）；有值未 merge ⇒ 10.78。
- **不能只对"带 auto 样式的 cell"之外的行做批量计算**：POI 的 `getColumnWidth(sheet, col, true, firstRow, lastRow)` 会算区间内所有 cell，
  而框架语义是"只算带 `width=auto` 的 cell"。实测反例：只给表头设 auto 时，整列算会把列宽从 2.71 撑到 28.54。
- 2026-09-28 已完成"延迟到终态算一次"（`ExcelCellSpan.autoSizeCalculated` + `ReportContext.cellSpans` 末尾补算），
  三重验证（模板端到端字节比对 + 只 merge 场景 A/B + 有值场景 A/B）均等价，收益 -50%。

## 文档维护习惯

- `doc/优化建议.md` 是本项目的优化台账：条目完成后改写为 `✅ 已解决`，保留原建议 + 实现说明 + **实测证据**，并同步顶部【〇、实现状态总览】表与【五、优先建议】表。
- 该文档重视**实证**：结论必须带可复现的探针/对照数据；若实测推翻原假设，直接写"原建议错误，已实测否决"，不要含糊。
