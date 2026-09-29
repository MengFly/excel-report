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
- **样式 key 的"实例判定"与"类别判定"方向相反，不能互相委托**（2026-09-29 实测，三轮返工）：实例判定 =
  `keyTarget.isInstance(target)`（"实例 ⊆ key 目标类型"）；类别判定（`CellStyles.filterStyle` 用）= `category.isAssignableFrom(keyTarget)`
  （"key 目标类型 ⊆ 类别"）。写反任一处都会静默失效：`filterStyle(map, Font.class)` 方向写反 ⇒ 8 个 font key 全丢（`getFont` 返回 null）；
  `Object` 重载委托给类别判定 ⇒ 13 个 target=`CellStyle.class` 的 cell key 全丢（`XSSFCellStyle.isAssignableFrom(CellStyle)=false`）。
  正确写法可用对称谓词 `a.isAssignableFrom(b) || b.isAssignableFrom(a)` 覆盖两种用途，但**类别判定会过宽**：
  `SizeStyleKey`（targetType `Object.class`）会混进 `filterStyle` 的所有结果，导致 `fontPool` 因 preferredSize 差异多生成 Font。
- **用户会与 AI 并行编辑同一批文件**：动手前先看 `ls -l --time-style=+%H:%M:%S`，不要假设工作区是自己的最后状态（本轮被覆盖过一次）。
- 探针建议放 `target/probe/`（`mvn clean` 即清，不进 git）；`javac` 需 `-encoding UTF-8`、`java` 需 `-Dfile.encoding=UTF-8`，否则中文注释按 GBK 报错。
- **`CellStyles` / `SheetStyles` 刻意不并表**（2026-09-29 评估后决定）：两者 key 集分别镜像 XSD 的 `Style`(32) 与 `SheetStyle`(27)，子元素交集为空；
  共用的登记/查找逻辑抽在 package-private 的 `style/StyleRegistry.java`，两个门面**各持一份实例**，各自保留 `DEFAULT_STYLE`
  （cell 默认 20 项 / sheet 默认 `defaultRowHeight=20f`，由 `ExcelReport:60`/`:63` 分别消费，方向不能互串）。
  并表会让"key 写错上下文"从"查不到 ⇒ 丢弃"变成"进 `StyleMap` ⇒ 事后被类型过滤静默丢弃"，改变 `StyleMap.equals` ⇒ 影响 `cellStylePool`/`fontPool` 的池命中。
- `SizeStyleKey` 的 targetType 是 `CellStyle.class`（POI 接口）⇒ 在对称谓词下它归入 `CellStyle` 类别：`filterStyle(map, CellStyle.class)`
  会带上 `preferredSize`（apply 是空 lambda，无副作用）；`filterStyle(map, Font.class)` 不受影响（`Font` 与 `CellStyle` 无父子关系）。
  若将来新增 `filterStyle(..., CellStyle.class)` 的调用点，需先决定 `preferredSize` 该不该出现在结果里。
- 样式**应用循环已统一**为 `StyleRegistry.initStyle(T target, StyleMap)`（先按"实例属于 key 目标类型"过滤再 apply）；
  `CellStyles.initStyle` / `SheetStyles.initStyle` 各自委托到自己的 `REGISTRY`，目标由调用方建（`workbook.createCellStyle()` / `createFont()`）。
  `CellStyles.createCellStyle`/`createFont`/`SheetStyles.initSheetStyle` 已删除（下一个版本为 breaking）。
- XSD `Style` 原本缺 `fillBackgroundColor`（框架侧一直注册着该 key），2026-09-29 已补；插件 `StyleKeyInfo.kt` 的镜像清单仍缺该 id，待插件侧同步。
- POI 5.2.5 读回样式时的 API 细节：`XSSFColor` 无 `getRGBHexString()`（用 `getRGB()` + `HexUtilencodeHexStr`）；`CellStyle.getXxxBorderColor()`
  返回 short 索引，颜色要 `XSSFCellStyle.getXxxBorderXSSFColor()`；`XSSFFont.getFontHeight()` 返回 **twips**（20pt → 400）；
  `XSSFFont.getUnderline()` 返回 short（`FontUnderline.DOUBLE.valueOf()` = 2）；`Sheet` 无 `getZoom()`；`CTWorksheet` 无 `getSheetViewArray(int)`。
- `SheetStyles` 的 `margin`（全边）与 `marginLeft` 等同时写入时，结果取决于 `HashMap` 迭代顺序（`initSheetStyle` 遍历 `styleMap.values()`），
  现有顺序下 `margin` 先应用、`marginLeft` 后覆盖 ⇒ 单边生效（旧实现同序，非回归）。

## 文档维护习惯

- `doc/优化建议.md` 是**台账式**文档（2026-09-28 精简自 628 行 → 129 行），固定五节：
  **一、状态总览**（表格：编号｜条目｜状态+提交号｜关键结论）→ **二、待办**（按 P1/P2/P3 分组）→
  **三、已完成项的关键结论（备查）** → **四、易踩点** → **五、性能验证建议**。
- 编号体系沿用旧版章节（二 = 易用性、三 = 性能、四 = 扩展性、六 = 已知隐患），文件顶部有说明行。
- 维护原则：**只留结论 + 关键实测数字 + 方案与前提**，不堆论证过程与长代码块；条目完成时更新状态列并附提交号；
  被实测推翻的假设标注"原建议被否决"；不再做的项标注"❌ 已取消"、实测依据移入"三、备查"；完整论证交给 git 历史。
  新增实测结论要带可复现数字（探针可只写"实测"）。

## 项目定位（对外口径）

- **框架专注复杂报表**（合并区 / 多列布局 / 图表 / 图片 anchor / 精确宽高）；**大批量报表不在此框架范围内**，
  一律建议走其他方案（POI SXSSF / EasyExcel(FastExcel) / 流式 CSV）。
- 由此决定：**三.6 SXSSF 流式导出已取消（2026-09-29）**，不要再提"改 `ExcelReport` 换 SXSSFWorkbook"。
  实测依据（类型不兼容、行序必须单调、刷盘行 `getRow()`=null / `getColumnWidth`=-1、图表缓存丢失）归档在 `doc/优化建议.md` 的"三、备查 三.6"。
- **扩展面是 XSD，不是注册表**（用户口径，2026-09-29）：`Parser` 标签集与 style key 集都由
  `src/main/resources/schema/excel-report-1.0.0.xsd` 封闭定义（`ContainerGroupContainer` 是封闭 `xs:choice`、`Style` 是封闭 `xs:all`，
  全 XSD 仅 `include` 类型有一处 `xs:anyAttribute`）。⇒ **四.1（Parser SPI）/ 四.6（registerStyleKey）已取消**，不要再提；
  若真要扩展，属"改 schema 协议 + 同步插件校验"。框架运行期**不校验 XSD**（`src/main` 无 schema 引用），
  模板头的 `xsi:schemaLocation` 只被编辑器/插件使用，所以会出现"能跑但不合规"。
