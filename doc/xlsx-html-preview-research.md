# xlsx → 网页回显 方案调研（后端解析 + 前端显示）

> 调研日期：2026-10-09
> 需求口径（来自框架作者）：后端解析 xlsx，前端显示，**样式 / 图表 / 图片都要正常显示**；
> 前端**不使用第三方前端框架**（图表回显允许 ECharts）；技术栈 Java / Spring Boot；
> 目标产物与 `excel-report` 导出的 Excel 尽量一致。
>
> 关联文档：`doc/json-html-starter-guide.md`（自研路径 X 的开发指南）
> 本文档只回答一个问题：**现成的框架能不能替我们干这件事？**

---

## 〇、先说结论（TL;DR）

**没有。** 现成方案全部落在下面三类中的某一类，且**没有一类同时满足"ECharts 回显图表"**：

| 路线 | 产物 | 图表命运 | 结论 |
|---|---|---|---|
| A. 商业库直接渲染 | 静态 HTML（自带 CSS） | **降级为静态图** | 图表变图片 ⇒ 与需求冲突 |
| B. LibreOffice headless | 静态 HTML（`<table>` + inline CSS） | **降级为图片文件** | 同上，且部署重、不可控 |
| C. 开源 POI 系转换器 | 静态 HTML `<table>` | **直接丢弃** | 连图片都没有 |
| D. 前端表格组件（Univer 等） | 浏览器内可交互表格 | 自绘图表（非 ECharts） | 违反"不用前端框架"约束 |
| E. 自研读回 JSON（路径 X） | 结构化 JSON | 原样交给 ECharts | 唯一满足全部约束 |

**决策轴不是"保真度"，而是"你要的是静态快照，还是要可交互的数据"。**
如果只要静态快照 ⇒ 直接买 Aspose.Cells，别自研。
如果要"前端可交互 + 图表 ECharts + 与模板语义联动" ⇒ 只能自研读回层（路径 X）。

---

## 一、逐个方案事实核查

### A. Aspose.Cells for Java（商业）

- 坐标：`com.aspose:aspose-cells`（Maven Central 与 `repository.aspose.com/repo` 均有），最新 25.3；JDK 8+。
- 能力（官方文档核实）：
  - `Workbook.save(path, SaveFormat.HTML)`；`HtmlSaveOptions` 支持
    `setExportImagesAsBase64(true)` / `setAddTooltipText(true)` / `setPresentationPreference(true)`。
  - 支持输出 HTML / MHTML / PDF / XPS / 图片 / JSON；"工作表→图片"、"图表→图片"、"工作表→PDF"。
  - **不含 Excel 安装依赖**，跨 Windows/Linux/macOS。
- ⚠️ **图表在 HTML 里的命运**：`HtmlSaveOptions` 没有"把 chart 转成可交互数据"的开关，
  它走的是"图表→图片"这条渲染路径。**要 ECharts 必须自己解析 `chart XML`**（Aspose 也能读
  `Workbook.getWorksheets().get(i).getCharts()`，但那时你已经在写读回层了）。
- 授权（官网 pricing 页实测）：

  | 档位 | 价格 |
  |---|---|
  | Developer Small Business（1 开发者 / 1 部署地址） | **US$1199** |
  | Developer OEM（1 开发者 / 无限部署地址） | **US$3597** |
  | Developer SDK（1 开发者 / 50 商业部署） | **US$23980** |

- 评估版限制（官方文档明确）：单程序最多打开 **100 个文件**（超出抛异常）；**始终存在一个带评估水印的额外工作表且强制为活动工作表**；
  纯文本导出（CSV/TSV）追加评估声明；PDF/图片顶部贴水印。
  ⇒ **评估版不能用于任何有"多 sheet 报表"的服务**，水印 sheet 永远在最前面，会被用户看到。
  可申请 30 天临时 license（需企业邮箱，个人/免费邮箱不受理）。

### B. Spire.XLS for Java（商业 + 免费版）

- 免费版限制（官方口径，核实过）：
  - 读写 **`.xls`**：最多 5 个 sheet / 200 行；
  - 读写 **`.xlsx`**：**无限制**；
  - 转换到 **PDF / 其他格式：仅前 3 页**。
  ⇒ 免费版对 `.xlsx` 读写确实宽松，但"转 PDF 只 3 页"这条决定了它不能作为报表导出链路的默认引擎。
- 支持 HTML / XML / CSV / Image / PDF / XPS / SVG 转换，宣称覆盖 charts / images / rich text / borders。
- ⚠️ 与 Aspose 同样的根本问题：**HTML 是静态渲染产物，图表不会变成 ECharts 可用的数据**。
- 商业版按开发者订阅计价（官网报价页为 $999 档，以官网实时价为准）。

### C. Apache POI 自带 / 第三方转换器

- 官方示例 `org.apache.poi.ss.examples.html.ToHtml`：
  - 只做 **cell 表格 + style → CSS** 映射；**无图片、无图表**。
- 第三方 `ru.perrymason:e2h:0.1.0`（GitHub `zhukpm/e2h`）：
  - POI → HTML 表格，开源；
  - README 自述局限："complex cell styles; data formatting; colors"；
  - **无 release、维护不活跃** ⇒ 不进生产。
- 结论：这条路只能验证"POI 能读到 cell"，**离"样式/图表/图片完整"差得最远**。

### D. LibreOffice headless（免费，最接近"真 Excel"渲染）

- 做法：`soffice --headless --convert-to 'html:HTML:EmbedImages' --outdir <dir> <file>`。
- 优点：完全免费；样式 / 合并单元格 / 边框 / 数字格式保留为 inline CSS；
  多 sheet 一起导出；公式会先算出值（HTML 里是静态值）。
- ⚠️ 四个硬伤：
  1. **图表变成图片文件**（HTML 里是 `<img>`），不是可交互图表；
  2. **部署重量**：安装包数百 MB，服务器还得装**字体**——字体一缺，列宽/换行全变（这恰恰打在我们最在意的"列宽一致"上）；
  3. **输出不可控**：只能拿到 HTML，拿不到结构化数据 ⇒ 想做"点某个单元格联动"就得去解析 HTML，反向工程；
  4. **进程模型**：单实例并发、超时、profile 锁在服务器上 notoriously 难伺候。
- 它还有一个致命不匹配：**LibreOffice 的 HTML 表格是"电子表格语义"，不是"报表语义"**——
  合并区被拍平成 `colspan`、`px` 宽度来自 LO 自己的排版引擎，与 Excel 的列宽算法不是同一套。

### E. 前端表格组件（Univer / Luckysheet 等）——**违反约束，仅作对照**

- Univer（`dream-num/univer`，Apache-2.0 核心 + 商业 Pro）：
  - Canvas 渲染、500+ 公式、多 sheet、合并、样式、条件格式、超链接、批注、绘图；
  - **图表、透视表、迷你图、协同、导入导出属于 Pro 商业扩展**；
  - 支持 xlsx 导入导出、Node.js 侧无头计算。
- Luckysheet：**已归档**，官方推荐迁移到 Univer。
- 为什么列出：它们的**文档数据模型（多 sheet / 合并 / 样式 / 富文本 / 图表一体化描述）**
  恰好就是"我们 JSON Schema 该长什么样"的现成参考答案 —— 值得抄它的 schema 分层，
  但它本身**不能用在这个项目里**（用户约束：不引入第三方前端框架；且我们要的是 Excel 版式而非可编辑表格）。

### F. Syncfusion（对照，Java 侧未核实完整）

- 官方 stories 页确认 Excel Library 具备：Excel→HTML、Excel→PDF、Worksheet→Image、Chart→Image、Excel→JSON。
- ⚠️ 检索命中的具体文档多为 .NET（`XlsIO`）侧；**Java 版 HTML 导出的细节（图表是否同样降级为图片、图片是否 Base64）本次未逐条核实**。
  若要认真评估需直接查 `syncfusion.com/document/excel-framework/net/...` 对应 Java 文档。
- 与 A/B 同性质：静态渲染产物。

---

## 二、决策矩阵

维度打分：✅ 能做到 / ⚠️ 有条件或降级 / ❌ 做不到

| 方案 | 单元格样式 | 图表 | 图片 | 结构化 JSON | 满足"ECharts" | 前端框架 | 授权费 | 部署重量 | 与 excel-report 链路契合 |
|---|---|---|---|---|---|---|---|---|---|
| **A. Aspose.Cells** | ✅ | ⚠️→静态图 | ✅ | ⚠️（可导 JSON，但需自己解析） | ❌ | 无（自带 HTML） | $1199 起 | 轻（纯 jar） | ⚠️ 拿不到"模板语义" |
| **B. Spire.XLS** | ✅ | ⚠️→静态图 | ✅ | ⚠️ | ❌ | 无 | 免费版受限 / 商业付费 | 轻 | ⚠️ 同上 |
| **C. POI `ToHtml` / e2h** | ⚠️ | ❌ | ❌ | ❌ | ❌ | 无 | 免费 | 轻 | ⚠️ 能力不足 |
| **D. LibreOffice headless** | ✅ | ⚠️→图片文件 | ⚠️ | ❌（只有 HTML） | ❌ | 无 | 免费 | **重（+字体）** | ❌ 排版引擎不同源 |
| **E. Univer 等** | ✅ | ✅（自绘） | ✅ | ✅ | ⚠️ 非 ECharts | **需要** | 开源+Pro | 中（前端包体大） | ❌ 语义不同 |
| **X. 自研读回（现有文档）** | ✅ | ✅ 原样给 ECharts | ✅ | ✅ | ✅ | 无 | 免费 | 轻 | ✅ 同一数据源，几何 100% 对齐 |

### 关于"与 excel-report 链路契合度"这一列

这是最容易被忽略、但对你最关键的一列。

现成方案（A/B/C/D）**都要求"先有一个 xlsx 文件"**。这意味着：

1. 每次预览都要**先完整生成一次 xlsx**（含 POI 建 sheet / 样式池 / 图片流），才能开始解析。
   性能上是"导出 + 解析"两遍，且解析侧还要再解一遍 OOXML（zip + XML + 样式继承解析）。
2. 拿到的是**Excel 的最终态**，模板的**语义信息全部丢失**：
   - 这是哪个组件（`TextComponent` / `ChartComponent` / `ImageComponent`）？——不知道；
   - 这个单元格对应数据模型的哪个字段？——不知道；
   - 这个合并区是"表头横跨"还是"分组容器"？——不知道。
3. 因此**做不了任何"报表态"的交互**：点某个表头联动过滤、点图表下钻明细、
   区域高亮、单元格 hover 显示 SpEL 表达式 —— 这些在 Excel 语义里根本不存在。

自研读回（X）则相反：**几何层和 Excel 完全同源**（同一份 `XSSFWorkbook` 读回，
列宽 px / 行高 pt / EMU anchor 数值一致），语义层是自己定义的，可以自由加交互字段。

---

## 三、明确建议

### 建议 1：不引入任何现成框架做主链路（维持路径 X）

理由（按权重排序）：
1. **ECharts 这条硬需求直接排除了 A/B/C/D** —— 它们都给不出可交互图表数据；
2. **语义信息是自研的独占收益** —— 组件级交互只有自己读回才有；
3. **"导出 + 再解析"双遍开销** 可通过"一次导出、双产物"消掉（JSON 在 export 之后直接从
   workbook 读，不落 xlsx 字节流，也不需要拿通用解析器）；
4. **零授权成本、零额外部署**（不装 LibreOffice、不加字体、不引 40MB+ jar）。

### 建议 2：如果临时要"给客户看个大概"，Aspose.Cells 是唯一值得考虑的外挂

适用场景：**内部 demo / 客户临时预览 / 排查"Excel 里到底长什么样"**。
一行 `workbook.save(htmlPath)` 就能出一个可发给客户的静态页面，这是自研两天的事。
但：
- **绝不能进生产** —— 评估版强制插一个水印 sheet 在最前面，客户一眼看到；
- 拿它做"保真度基准"很合适：用它渲染你的 xlsx，和自研 JSON 的 HTML 截图对比，
  就是一个**现成的回归基线**（比手写期望值靠谱）。这一条其实很实用，建议采纳为验证手段。

### 建议 3：把 E（Univer）当"schema 参考"，不当依赖

Univer 的文档数据模型分层（`sheet → cell → style/value/richText` 分离、多 sheet、
合并区独立表达）值得在设计 `json-html-starter-guide.md` 的 JSON Schema v1 时对照借鉴。
但**不要引它的包**：违反"不用第三方前端框架"的约束，且它的目标是"可编辑电子表格"，
与我们"只读报表回显"目标不一致。

### 建议 4：如果一定要"零自研"，最诚实的说法是

> 只有"静态快照"能做到零自研，且必须付费 + 图表降级为图片。
> 一旦要求"图表可交互 / 样式由前端精确控制 / 与报表语义联动"，就必然要自研读回层。

---

## 四、事实边界（哪些我没验证到）

诚实标注，避免被当成已核实结论：

- **Aspose 在 HTML 模式下图表的具体产物形态**（`<img>` data URI？独立文件？多图拼图？）
  未跑实测，只从官方 `HtmlSaveOptions` 无对应开关推断为"图片"。落地前建议花 1 小时跑一个真例验证。
- **Spire.XLS 转 HTML 的保真细节**（合并区、图表、RichText 的具体呈现）未跑实测。
- **Syncfusion Java 版**能力本次只核实到"官方有 Excel→HTML / Chart→Image"，细节未查。
- **LibreOffice 对 xlsx 的 HTML 导出会把合并区拍成 `colspan`** 是基于通用行为的推断，
  未在你的实际报表上跑过。
- 各家 HTML 输出**在 Chrome 下的像素级表现**未实测（字体引擎差异必然存在，
  与本项目"行内文本 ≈95%"的结论一致）。

---

## 五、下一步建议动作

1. **不改现有文档结论** —— 调研结果支持 `doc/json-html-starter-guide.md` 的路径 X，无需修订主方案。
2. 在文档 §8「已知差异与验收清单」里**补一条**：把 Aspose.Cells 列为"保真度对照基准"（仅测试期使用，不进生产）。
3. 调研结论已记入 `.workbuddy/memory/2026-10-09.md`，避免后续重复调研。