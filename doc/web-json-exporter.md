---
date: 2026-10-10
complete: true
description: 新增 bridge 包下的 WebJsonExporter，把 ExportResult 转成可前端传输的结构化 JSON 对象（扁平单元格网格），用于前端回显匹配样式的 Excel；优先使用 Container 属性，不依赖 Sheet 的 POI 属性
---

# WebJsonExporter - 任务规划

## 1. 需求概述

背景：框架导出 Excel 时把结构"写进" POI 的 `XSSFSheet`，前端拿不到可传输的结构。上一步已给每个 `Container` 回填了合并后的完整样式 `finalStyle`（见 [container-final-style.md](file:///d:/job/excel-report/excel-report/doc/container-final-style.md)），为"从 Container 树反推带样式结构"打好了基础。

目标：新增 `io.github.mengfly.excel.report.bridge.web.WebJsonExporter`，输入 `ExportResult`，输出一个**结构化对象**（bridge 包内强类型 POJO），描述一张"扁平单元格网格"，供前端回显匹配样式的 Excel。

约束（来自 `AGENTS.md`）：
- 编译/构建使用 JDK 1.8。
- **优先兼容与使用 Container 属性**（`getPosition()` / `getMeasuredSize()` / `getFinalStyle()` / `templateNode()`）；**如无必要不读取 Sheet 里的 POI 属性**（不读 `XSSFCell` / `CellStyle`；本方案完全不访问 `ReportContext.getSheet()`）。
- 方法/字段上方的 `/** */` 只写对调用方的契约；变更说明、边界背景用 `//` 或 `/* */` 写在改动旁。
- 纯新增类，对 `../excel-report-plugin` 影响为零（该插件只做 IDEA 内渲染，不消费本包）。

## 2. 需求澄清记录

| 澄清点 | 结论 |
|---|---|
| JSON 结构形态 | **扁平单元格网格**：遍历 Container 树，把每个可视组件转成一个单元格 `{row,col,rowSpan,colSpan,type,value,style,...}`，前端直接按网格回显 |
| 对外 API / 返回类型 | **返回结构化对象**，且载体为 **bridge 包内强类型 POJO**（`WebExcel` / `WebCell`），不用裸 Map/JSONObject |
| 图片 / 图表处理 | **图片内嵌 base64**（`ImageComponent.getImage().openStream()` → dataURL）；**图表占位描述** |
| 样式输出形式 | **仅值归一化，保留 key 名**：颜色→`#RRGGBB`、枚举→小写、布尔/数字→原生类型；key 沿用框架样式 key（`fillForegroundColor` / `fontBold` / `alignHorizontal` …） |
| 组件覆盖范围 | **全组件覆盖，含 List 特判**：Text/Link/Span/Image/Chart/Layout(H/V/Grid)/Table 走树遍历；`ListComponent` 单独特判展开表头/数据单元格 |
| 包名 | `io.github.mengfly.excel.report.bridge` |

## 3. 现状与根因（代码勘察结论）

- 入口数据：`ExcelReport.exportSheet(...)` / `exportTemplate(...)` 返回 `ExportResult`，其 `getContainer()` 是渲染后（已 measure + layout + export）的组件树根，`getContext()` 是 `ReportContext`（[ExportResult.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/excel/ExportResult.java)）。
- 几何信息在 Container 上：网格坐标 `position`（x=列, y=行）与 `measuredSize`（w=列跨度, h=行跨度）。→ 单元格 `row/col/rowSpan/colSpan` 可**完全由 Container 得到**，无需读 POI。
- 样式信息在 Container 上：`getFinalStyle()` 返回合并后完整样式（`StyleMap`，内部是 `keyId -> 字符串值`），导出后有效（[Container.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/Container.java#L56-L64) / [StyleHolder.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/style/StyleHolder.java#L12-L18)）。
- 类型/模板标签：`Container.templateNode().getTagName()` 可得模板标签名（如 `Text`/`Image`/`Chart`）；手工构造的组件无模板节点，回退 `getTypeName()`（类简名）。
- 组件分层：
  - **容器类（递归）**：`Layout`（`HLayout`/`VLayout`/`GridLayout`/`TableComponentNew`）。`TableComponentNew` 的子节点就是单元格 `TextComponent`（[TableComponentNew.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/component/table/TableComponentNew.java#L46-L65)）。
  - **叶子类（产单元格）**：`TextComponent`、`LinkComponent`(继承 Text)、`SpanComponent`(仅合并区域)、`ImageComponent`、`ChartComponent`。
  - **无尺寸/跳过的**：`PageRowSplitComponent`/`PageColSplitComponent`，`getSize()` 为 `(0,0)`。
  - **特判**：`ListComponent` 不是 `Layout`、**无子 Container**，其表头/数据单元格在 `onExport` 内联生成（[ListComponent.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/component/list/ListComponent.java#L40-L91)），纯树遍历拿不到，需按同一套坐标规则手工展开。
- 样式归一化的可行路径：`StyleMap` 存的是字符串，`CellStyles.getStyleKey(id)` 可反查到 `StyleKey`，`StyleKey.getStyle(raw)` 得到强类型值（枚举/`XSSFColor`/`Boolean`/`Number`/`Size`…），再按类型归一化（颜色→`#RRGGBB`、枚举→小写名、其余原生类型）。未注册的 key 回退原字符串。
- Sheet 级样式来自 `ReportContext.getSheetStyle()`（`StyleMap`，非 POI），可安全归一化输出；**sheet 名不主动从 POI 读取**，改由调用方可选传入。
- 已知边界（沿用上一步结论）：`ListComponent` 表头附加样式来自 `ListHeader.getStyle()`，不在 `finalStyle` 内，需在特判时单独合并。

## 4. 任务列表

### 任务 1：bridge 模型类（WebExcel / WebCell）

| 属性 | 值 |
|------|-----|
| **描述** | 新增 `io.github.mengfly.excel.report.bridge.web.WebExcel` 与 `WebCell` 强类型 POJO。`WebExcel`：`sheetName`(可空，由调用方传入)、`rowCount`、`colCount`、`sheetStyle`(`Map<String,Object>`，归一化后的 sheet 级样式)、`cells`(`List<WebCell>`)。`WebCell`：`row`、`col`、`rowSpan`、`colSpan`、`type`(组件类型：模板标签名或类简名)、`value`(文本值，可空)、`image`(base64 dataURL，可空)、`link`(超链接地址，可空)、`style`(`Map<String,Object>`，归一化样式)。使用 Lombok `@Getter/@Setter`，保持可再序列化。 |
| **依赖关系** | 无 |
| **验收标准** | 编译通过（JDK 1.8）；用 hutool `JSONUtil.toJsonStr` 能把实例序列化为合法 JSON。 |

### 任务 2：样式归一化（WebStyleNormalizer）

| 属性 | 值 |
|------|-----|
| **描述** | 新增包内归一化工具（如 `WebStyleNormalizer`）。输入 `StyleMap`，输出 `Map<String,Object>`：逐条 `id -> raw`，用 `CellStyles.getStyleKey(id)` 反查 `StyleKey` 并 `getStyle(raw)` 得强类型值，再归一化——`XSSFColor`→`#RRGGBB`（`getRGB()` 为空时回退原值）、`Enum`→`name().toLowerCase()`、`Boolean`/`Number`→原生、`Size`/其他→字符串形式；查不到 key 或转换异常则保留原字符串。null 入参返回空 Map。 |
| **依赖关系** | 无（可与任务 1 并行） |
| **验收标准** | 单测覆盖：颜色 `#2570d4`、枚举 `alignHorizontal=CENTER→"center"`、布尔、数字、未注册 key 回退；异常不抛出。 |

### 任务 3：WebJsonExporter 主遍历（几何/文本/类型）

| 属性 | 值 |
|------|-----|
| **描述** | 新增 `WebJsonExporter`，提供 `public static WebExcel export(ExportResult result)`（可另加 `export(ExportResult, String sheetName)` 重载）。从 `result.getContainer()` 递归遍历：`is Layout`（HLayout/VLayout/GridLayout/TableComponentNew）→ 递归 `getContainers()`；叶子组件 → 以 `position`/`measuredSize` 生成 `WebCell`（`row=position.y, col=position.x, rowSpan=size.height, colSpan=size.width`），`style` 取 `getFinalStyle()` 归一化，`type` 取 `templateNode()` 标签名（无则 `getTypeName()`），`value` 取组件文本（`TextComponent.getText()`）。`size<=0` 或 `position==null` 的节点跳过。产出后按 `(row,col)` 稳定排序。`WebExcel.sheetStyle` 来自 `result.getContext().getSheetStyle()` 归一化；`rowCount/colCount` 由 cells 的覆盖范围计算。可选轻量去重：同一左上角 `(row,col)` 合并，`value` 优先取非空，span 取较大值。 |
| **依赖关系** | 依赖任务 1、任务 2 |
| **验收标准** | 用模板导出（嵌套 H/V/Grid + Text/Span）后，cells 的行列/跨度与 `position`/`measuredSize` 一致；`style` 为归一化后的 key->值；`type` 为模板标签名。 |

### 任务 4：图片 base64 内嵌 & 图表占位

| 属性 | 值 |
|------|-----|
| **描述** | 在遍历中特判：`ImageComponent` → 若 `getImage()!=null` 用 `openStream()` 读取字节并 `Base64` 编码，`image` 置为 `data:{imageType};base64,...`（`imageType` 取自 `Image.getImageType()`，缺省 `png`）；读取失败不抛出，`image` 置空并保留占位文本。`ChartComponent` → 占位单元格，`type` 取模板标签名（如 `Chart`）、`value` 填图表类型描述（`getType()` 相关标识）。二者几何与样式仍按任务 3 规则输出。 |
| **依赖关系** | 依赖任务 3 |
| **验收标准** | 含 `Image` 的模板导出后 `image` 是以 `data:image/` 开头的合法 dataURL 且能被 base64 解码；含 `Chart` 的模板导出后存在 `type=Chart` 的占位单元格，未抛异常。 |

### 任务 5：ListComponent 单元格展开特判

| 属性 | 值 |
|------|-----|
| **描述** | 对 `ListComponent` 特判展开（与 `onExport` 坐标规则一致，[ListComponent.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/component/list/ListComponent.java#L49-L91)）：`VERTICAL` 时表头 `(x, y)` 跨度 `(span, header.span)`、数据逐行 `(x, y+startRow)` 跨度 `(span, 1)`；`HORIZONTAL` 时表头 `(x, y)` 跨度 `(header.span, span)`、数据逐列 `(x+startCol, y)` 跨度 `(1, span)`。表头样式对齐框架实际行为：**仅 `HORIZONTAL` 分支叠加 `header.getStyle()`**（`exportHorizontalList` 才叠加，`exportVerticalList` 未叠加）；数据单元格样式一律用 `getFinalStyle()`。`value` 取 `header.getTitle()` / 各数据元素。单元格 `type` 同样遵循"模板标签名优先、否则类简名"规则。 |
| **依赖关系** | 依赖任务 3 |
| **验收标准** | 含 `List`（横/纵）的模板导出后，表头与各数据单元格均出现在正确行列、跨度正确；横向表头样式含 `Header` 附加样式，纵向表头不含（与框架一致）。 |

### 任务 6：测试与回归

| 属性 | 值 |
|------|-----|
| **描述** | 新增单测（JUnit4，风格对齐 [FinalStyleTest.java](file:///d:/job/excel-report/excel-report/src/test/java/io/github/mengfly/excel/report/excel/FinalStyleTest.java)）：(a) 嵌套布局 + Text 的几何/跨度正确；(b) 样式归一化（颜色/枚举/布尔/数字/未注册回退）；(c) 继承样式经 `finalStyle` 进入 cell.style；(d) 图片 base64 前缀与可解码；(e) 图表与 List 特判；(f) 导出前调用 `export` 得到空/安全结果不抛异常。并回归既有 `FinalStyleTest`、`ExcelCellSpanTest`、`ExcelReportSheetNameTest`。 |
| **依赖关系** | 依赖任务 1–5 |
| **验收标准** | `mvn test` 全绿（既有 43 项 + 新增用例）。 |

## 5. 执行顺序

```
任务 1（模型）     任务 2（样式归一化）
        \             /
         └── 任务 3（主遍历：几何/文本/类型）
                    ├── 任务 4（图片/图表特判）
                    └── 任务 5（List 特判）
                              └── 任务 6（测试与回归）
```

任务 1 与任务 2 无依赖，可并行；任务 4 与任务 5 无相互依赖，可并行；任务 6 在全部完成后统一回归。

## 6. 边界与风险

- **不碰 POI**：全程不调用 `ReportContext.getSheet()`，几何来自 `position`/`measuredSize`、样式来自 `finalStyle`/`sheetStyle`，满足"如无必要不取 Sheet 的 POI 属性"。
- **sheet 名**：不从 POI 读取；`WebExcel.sheetName` 由调用方通过重载可选传入，缺省为空。
- **`finalStyle` 有效期**：仅在导出完成后有效；对未经 `export` 的 Container 调用，`finalStyle` 为 `null`，归一化需按空 Map 处理、不抛异常。
- **List 边界**：`ListComponent` 无子 Container，只能靠特判重建；其表头附加样式必须显式合并 `ListHeader.getStyle()`（上一步文档已记录该边界）。
- **图片 IO**：`ImageComponent` 内嵌 base64 会在导出期产生 IO 与体积膨胀（大图/多图）；读取失败按占位降级，不中断导出。
- **重叠单元格**：`SpanComponent` 只定义合并区域、无值；与 Text 同左上角时按任务 3 的去重规则合并，避免前端网格错位。
- **零破坏**：全部为 bridge 包内新增类，不改动框架既有 API；`../excel-report-plugin` 不消费该包 ⇒ 预期零影响。
- **注释规范**：契约写 `/** */`；"为何不读 POI""为何 List 需特判"等说明用 `//` 写在改动旁。

## 7. Complements

### 1. bridge 模型类（WebExcel / WebCell）
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/bridge/WebExcel.java` — 新增：sheetName/rowCount/colCount/sheetStyle/cells
  - `src/main/java/io/github/mengfly/excel/report/bridge/WebCell.java` — 新增：row/col/rowSpan/colSpan/type/value/image/link/style
- **审查结果**：审查通过（字段与需求一致；Lombok @Getter/@Setter，hutool 可序列化）
- **完成时间**：2026-10-10 10:25

### 2. 样式归一化（WebStyleNormalizer）
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/bridge/WebStyleNormalizer.java` — 新增：`CellStyles.getStyleKey` 反查强类型值后归一化（颜色 `#RRGGBB`、枚举小写、布尔/数字原生、其余字符串），未注册/异常回退原串，null 返回空 Map
- **审查结果**：审查通过（单测覆盖颜色/枚举/布尔/数字/未注册/null）
- **完成时间**：2026-10-10 10:25

### 3. WebJsonExporter 主遍历
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/bridge/WebJsonExporter.java` — 新增：`export(ExportResult)` / `export(ExportResult, String)`；Layout 递归、叶子产单元格；几何取 position/measuredSize、样式取 finalStyle、类型取模板标签名（回退类简名）；(row,col) 排序与轻量去重；rowCount/colCount 计算
- **审查结果**：审查通过
- **完成时间**：2026-10-10 10:25

### 4. 图片 base64 内嵌 & 图表占位
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/bridge/WebJsonExporter.java` — 新增 `toDataUrl`（Image → `data:{type};base64,`，失败降级占位文本）与 `chartLabel`（图表占位描述）
- **审查结果**：审查通过（单测验证 dataURL 可解码、图表占位单元格存在）
- **完成时间**：2026-10-10 10:25

### 5. ListComponent 单元格展开特判
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/bridge/WebJsonExporter.java` — 新增 `expandList`：横/纵表头与数据单元格坐标/跨度对齐 `ListComponent#onExport`；表头样式仅横向叠加 `header.getStyle()`
- **审查结果**：审查通过，并按审查意见修正"纵向表头误叠加 header 样式"（改为仅横向叠加，与框架 `exportVerticalList` 一致）
- **完成时间**：2026-10-10 10:25

### 6. 测试与回归
- **状态**：✅ 已完成
- **修改文件**：
  - `src/test/java/io/github/mengfly/excel/report/bridge/WebJsonExporterTest.java` — 新增 10 个用例（几何/归一化/未注册/null/继承样式/图片/图表/横纵 List/sheetName/空入参）
- **审查结果**：`mvn test`（JDK 1.8）全绿 —— `Tests run: 54, Failures: 0, Errors: 0`，BUILD SUCCESS
- **完成时间**：2026-10-10 10:30

### 完成总结
- 6 条任务全部实现并通过审查；纯新增 `bridge` 包（4 个主类 + 1 个测试类），未改动框架既有 API，对 `../excel-report-plugin` 零影响。
- 全程不读取 Sheet 的 POI 属性（几何取自 Container 的 position/measuredSize，样式取自 finalStyle/sheetStyle），满足"优先使用 Container 属性"的约束。

## 8. 补充：真实复现增强（第二轮）

用户验收反馈：**图表只有占位描述、无法复现；列宽/行高缺失、无法还原真实尺寸**，并要求排查其它阻碍复现的问题。

### 8.1 排查结论

| 问题 | 根因 | 处理 |
|---|---|---|
| 图表无法复现 | 仅输出占位文本，未带类型/坐标轴/数据 | 新增 `WebChart`/`WebChartAxis`/`WebChartSeries`/`WebCellRange`，从 `ChartDataType` 提取支持类型、标题、图例、数据标签、分类轴与数值轴的系列数据 |
| 列宽/行高缺失 | 已算出的实测宽高只存在于 `ReportContext` 的私有 `autoWidthColumn`/`autoHeightRow`，未导出 | `ReportContext` 新增 `getColumnWidths()`/`getRowHeights()`（只读、纯新增，零破坏）；`WebExcel` 新增 `columnWidths`(字符)/`rowHeights`(磅) |
| Sheet 级样式未归一化 | `WebStyleNormalizer` 只查 `CellStyles` 注册表，`SheetStyles` 独立注册表未查 ⇒ `defaultRowHeight`/`tabColor`/`margin` 等落回原始字符串 | 归一化时依次查 `CellStyles` 与 `SheetStyles` |
| 富文本被降级 | `TextComponent.getText()` 可为 `RichText` 或 `<html>` 串，原先只 `toString` | `WebCell` 新增 `richText`（`WebTextRun` 分片），拆分 html/`RichText`，`value` 存纯文本拼接 |
| 图片渲染参数缺失 | 只给 dataURL，缺缩放/内边距/锚点 | `WebCell.image` 由 String 改为 `WebImage`（dataUrl/scaleType/scaleHeight/padding/anchorType） |

### 8.2 任务列表

| 任务 | 内容 | 状态 |
|---|---|---|
| A | `ReportContext` 暴露已算列宽/行高；`WebExcel` 增加 `columnWidths`/`rowHeights` | ✅ |
| B | 图表数据模型与提取（`WebChart*` + `WebCellRange`） | ✅ |
| C | `WebStyleNormalizer` 兼容 `SheetStyles` | ✅ |
| D | 富文本分片（`WebTextRun`） | ✅ |
| E | 图片元信息（`WebImage`） | ✅ |
| F | 测试与回归、文档更新 | ✅ |

### 8.3 修改文件

- `src/main/java/io/github/mengfly/excel/report/excel/ReportContext.java` — 新增 `getColumnWidths()`/`getRowHeights()`（只读视图，纯新增 API，对插件零影响）
- `src/main/java/io/github/mengfly/excel/report/bridge/WebExcel.java` — 新增 `columnWidths`/`rowHeights`
- `src/main/java/io/github/mengfly/excel/report/bridge/WebCell.java` — `image` 改为 `WebImage`；新增 `chart`/`richText`
- `src/main/java/io/github/mengfly/excel/report/bridge/WebStyleNormalizer.java` — 增加 `SheetStyles` 注册表回退
- `src/main/java/io/github/mengfly/excel/report/bridge/WebJsonExporter.java` — 填充宽高、富文本、图片参数、图表数据
- 新增：`WebImage`/`WebTextRun`/`WebCellRange`/`WebChart`/`WebChartAxis`/`WebChartSeries`
- `src/test/java/io/github/mengfly/excel/report/bridge/WebJsonExporterTest.java` — 13 个用例（新增宽高/Sheet 级样式/富文本/图片参数/图表数据与区域引用）

### 8.4 验收与验证

- **单测**：`mvn test -Dtest='!SimpleTemplateTest'`（JDK 1.8）——`Tests run: 56, Failures: 0, Errors: 0`，BUILD SUCCESS。
  （`SimpleTemplateTest#testLayoutReport` 失败与本轮无关：其引用的 `TestLayoutReport.xlsx` 模板资源不存在，属用户并行在写的用例。）
- **真实模板核验**：`WebJsonExportTest`（`TestTemplate.xml`）输出中确认——
  - `columnWidths` 为实测值（如第 0 列 `20.75`）；
  - `chartTypes` 为实际类型（`line/scatter/bar/area`、`bar3d/area3d/line3d`、`pie3d/pie`），系列含 `title/chartType/color/smooth/values` 与真实数据点；
  - 富文本单元格输出 `richText` 分片（对应模板内 `<html>…<font>RichText</font>…</html>`）。

### 8.5 边界

- `columnWidths`/`rowHeights` 仅含**显式设置过** `width`/`height` 样式的列/行；其余列/行需用 `sheetStyle` 的 `defaultColumnWidth`/`defaultRowHeight` 兜底。
- 图表数据若为单元格区域引用（`valueType="address"`），输出 `reference`（`CellRangeAddress` 的四至），由前端从 `cells` 网格取值——框架侧不读 POI 单元格。
- 富文本的 `value` 为纯文本拼接；分行/分片渲染以前端按 `richText` 处理为准。
- 本轮新增的 `ReportContext#getColumnWidths()`/`getRowHeights()` 为纯新增只读方法，不改任何既有行为。

## 9. 内容模型：内容 ≠ 单元格

### 9.1 背景

第二轮里 `WebCell` 直接挂了 `value/image/link/chart/richText` 一组可选字段，形成"可选字段袋子"，被质疑冗余，并讨论过让 `WebImage`/`WebChart` 继承 `WebCell`。

### 9.2 结论：继承方向不成立，改用"抽出内容基类"

`WebCell` 描述的是**网格位置 + 样式**；`WebImage`/`WebChart` 描述的是**该位置装的内容**。内容没有行/列/跨度/样式，让内容继承单元格会让它凭空获得几何语义（IS-A 不成立），冗余反而更严重。真正的冗余来自"内容与几何混在同一类"，故**把内容抽出去**：

```
WebCell { row, col, rowSpan, colSpan, type, style, content }
WebCellContent(abstract) { kind }        // kind: text / image / chart，供前端稳定分发
  ├── WebTextContent { value, link, richText }
  ├── WebImage       { dataUrl, scaleType, scaleHeight, padding, anchorType }
  └── WebChart       { chartTypes, title, titleOverlay, legend, marker, labelAxis, valueAxes }
```

### 9.3 修改文件

- 新增：`WebCellContent`、`WebTextContent`
- `WebCell` — 移除 `value/image/link/chart/richText`，改为单一 `content`
- `WebImage`/`WebChart` — 继承 `WebCellContent`，构造器置 `kind`
- `WebJsonExporter` — `toContent(...)` 按组件产出内容；`textContent(...)` 承载文本/富文本；`mergeCell` 简化为"内容取非空 + 跨度取大"
- 测试 — 断言改为经由 `content`（新增 Span 无内容、kind 判别用例），共 14 项

### 9.4 验证

`mvn test -Dtest='!SimpleTemplateTest'`（JDK 1.8）：BUILD SUCCESS，`WebJsonExporterTest` 14/14 通过。