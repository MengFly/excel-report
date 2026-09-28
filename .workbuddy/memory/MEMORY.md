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

## 文档维护习惯

- `doc/优化建议.md` 是本项目的优化台账：条目完成后改写为 `✅ 已解决`，保留原建议 + 实现说明 + **实测证据**，并同步顶部【〇、实现状态总览】表与【五、优先建议】表。
- 该文档重视**实证**：结论必须带可复现的探针/对照数据；若实测推翻原假设，直接写"原建议错误，已实测否决"，不要含糊。
