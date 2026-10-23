---
date: 2026-10-23
complete: true
description: 补上前端 JSON 导出的用户可见说明（README + javadoc + VERSION.md/V1.6.1.md），并把注释口径与实现对齐；不新增序列化 API
---

# 前端 JSON 导出说明与 V1.6.1 补记 - 任务规划

## 1. 需求概述

### 1.1 背景

巡检（2026-10-22）指出：`bridge` 包已有把导出结果转成"前端可回显扁平网格"的能力，但**三个用户入口都没有说明**——

- `README.md` 检索 `WebJsonExporter` / `JSON` 零命中；
- `VERSION.md` 最新条目仍是 V1.6.0，而 `pom.xml` 已是 1.6.1（且 `1.6.1` 标签已打在 `affe2f0`）；
- 使用者只能翻 `BridgeExportTest` 才知道"拿到 POJO 后还要自己再调一次 JSON 工具"。

### 1.2 目标

让用上 1.6.1 的人能在 README 里直接看到"怎么把报表变成前端可渲染的 JSON"，照抄一段示例即可跑通；同时把 1.6.1 的真正变更（含**公共类型包迁移**）补进版本记录。

### 1.3 范围界定（经澄清后收窄）

原建议中的"bridge 包补一个直接返回 JSON 文本的导出方法"**不采纳**。理由（用户口径）：框架有意在后续优化中逐步移除对 `hutool` 的依赖，JSON 序列化属于调用方选择，不应进框架 API。

⇒ 本轮**不新增任何序列化方法**，交付物是：README 小节 + javadoc 用法说明 + 版本记录；代码侧只改注释文本，不改行为。

## 2. 需求澄清记录

| 澄清点 | 用户结论 | 对任务的影响 |
|---|---|---|
| 序列化出口形态 | **不加进框架**；只在 README 与 API 文档里写清怎么做 | 删除原建议的"补 toJson 方法"；任务从"写代码"变为"写文档"。示例里用哪个库由文档说明，不作为框架约定 |
| README 深度 | **只给最小 Java 示例** | 不做字段语义表、不写前端渲染代码；但保留一句"必须在导出之后调用"的时机提示（否则读者会拿到空样式） |
| V1.6.1 条目范围 | **全量 + 建 `version/V1.6.1.md`** | 任务扩到 1.6.1 的全部公共变更，含 `TextComponent`/`TableComponent`/图表包的迁移与 `Container` 方法移除 |
| 注释与实现不一致 | **修正注释对齐实现** | 删掉 `WebExcel` javadoc 里对不存在的 `export(ExportResult, String)` 的引用；`WebJsonExporter` 类注释改为如实描述"仅 sheetName 读 POI" |

派生推论（本轮按此执行，不再追问）：
- 框架当前 `hutool-all` 是 compile 依赖，README 示例用它的 `JSONUtil` 是最短路径；但文档必须同时声明"返回的是普通 POJO，任意 JSON 库均可"，否则等于用文档把 hutool 又绑死了。
- javadoc 里**不写**具体库的调用代码（同上理由），只写"用项目已有的 JSON 库序列化"。

## 3. 现状勘察（证据）

| 事实 | 证据 |
|---|---|
| 导出能力已实现，返回 POJO | `WebJsonExporter.java:58`（`implements BridgeExporter<WebExcel>`）、`:71`（`export(ExportResult)` 返回 `WebExcel`）；类内无任何 `toJson` |
| 序列化由调用方完成 | `BridgeExportTest.java:25-26`：`BridgeExporter.export(...)` 之后自己调 `JSONUtil.toJsonPrettyStr` |
| `ExportResult` 可从导出链路直接拿到 | `ExcelReport.java:97` `exportTemplate(...)` 返回 `ExportResult`；`ExportResult` 仅有 `container` / `context` 两个 getter |
| javadoc 引用了不存在的重载 | `WebExcel.java:23` 写"需由调用方通过 `WebJsonExporter#export(ExportResult, String)` 传入"，但该类只有单参 `export`（`WebJsonExporter.java:71`） |
| 注释与实现矛盾 | `WebJsonExporter.java:45` 称"全程不读取 Sheet 的 POI 属性"，但 `:76` 实际调了 `result.getContext().getSheet().getSheetName()` |
| README 缺该能力 | 全文无 `WebJsonExporter` / `JSON` 命中；依赖片段仍是 `1.6.0`（`README.md:27`） |
| VERSION.md 落后一格 | `VERSION.md:1` 首条为 V1.6.0；`version/` 下只有 V1.2.0 / V1.3.0 / V1.6.0，无 V1.6.1 |

### 3.1 V1.6.1 实际变更集（`git diff v1.6.0..v1.6.1`，src/main 51 个文件）

**新增能力**

- `bridge` 包：`BridgeExporter`（导出器抽象）、`web.WebJsonExporter` + 9 个 `Web*` 模型、`text.TextStructExporter`（调试用文本结构）
- `excel.ExportResult`：`exportTemplate` / `exportSheet` 的返回值，承载渲染后的 `Container` 树与 `ReportContext`
- `StyleAble#getFinalStyle()` / `setFinalStyle(...)`：`Container#export` 在 `StyleChain` 回调内记录"祖先链 + 自身"合并后的完整样式
- `ReportContext`：新增已算出的列宽/行高与 sheet 级样式访问

**破坏性变更（编译期可发现）**

| 类别 | 变更前 | 变更后 |
|---|---|---|
| 文本组件包 | `component.TextComponent` / `LinkComponent` / `SpanComponent` | 迁至 `component.text.*` |
| 表格组件语义 | `TableComponent`（`dataList` + `columns` 数据驱动表，自带导出逻辑） | **删除**；`TableComponentNew`（布局式，`GridLayout` 承载 `Text` 子组件）改名为 `TableComponent`。**两个构造签名不变 ⇒ 老调用仍能编译**；但无参构造与 `setDataList`/`setColumns`/`setHeaderHeight`/`setHeaderVisible` 已不存在（字段改 final，Lombok 不再生成 setter） |
| 图表包 | `chart.AxisType`、`chart.data.{ChartMarker,ChartTitle,Legend}`、`chart.data.{AxisDataResolver,ValueAxisDataResolver,RelationAxisDataResolver}` | 分别迁至 `chart.axis`、`chart.component`、`chart.data.resolver` |
| 方法移除 | `Container#print()`、`Container#getTypeName()`、`Layout#print()` | 已删除（`TextStructExporter` 取代打印职责） |

> 已核 `../excel-report-plugin`：其 `src` 下无 `getTypeName` / `.print()` 调用，方法移除对该插件零影响。

## 4. 任务列表

### 任务 1：注释口径对齐（`WebExcel` / `WebJsonExporter`）

| 属性 | 值 |
|------|-----|
| **描述** | 只改注释文本，不改行为：(a) `WebExcel#sheetName` 的 javadoc 删除对不存在的 `export(ExportResult, String)` 的引用，改为"自动取自导出 Sheet 的名称"；(b) `WebJsonExporter` 类注释里"全程不读取 Sheet 的 POI 属性"改为准确表述——**只有 sheetName 取自 `Sheet#getSheetName()`，几何/样式/宽高均来自 `Container` 与 `ReportContext`**。遵循 `AGENTS.md`：`/** */` 只写对调用方的契约，变更缘由用 `//` 附在改动旁。 |
| **依赖关系** | 无 |
| **验收标准** | `mvn -o -q -DskipTests compile`（JDK 1.8）通过；全文搜索 `export(ExportResult, String)` 在 `src/main` 下 0 命中；类注释不再出现与 `:76` 矛盾的表述。 |

### 任务 2：`WebJsonExporter` 类 javadoc 补"如何得到 JSON 文本"

| 属性 | 值 |
|------|-----|
| **描述** | 在类 javadoc 的取值约定之后追加一段用法说明：`export(...)` 返回的是**普通 POJO**（标准 getter），框架**不内置** JSON 序列化，调用方用项目已有的 JSON 库转成文本即可；给出一行调用链（`report.exportTemplate(...)` → `instance.export(result)` → 序列化），并注明"必须在该 Sheet 导出完成后调用，否则 `finalStyle` 与宽高为空"。**不写具体库的调用代码**（避免文档反向绑定将要移除的 hutool）。 |
| **依赖关系** | 依赖任务 1（同一文件的注释改动，顺序执行避免冲突） |
| **验收标准** | 只看 javadoc 能独立回答"我要 JSON 文本该怎么做"；新增内容不含具体 JSON 库的类名/方法名。 |

### 任务 3：README 新增「导出为前端 JSON（可选）」小节

| 属性 | 值 |
|------|-----|
| **描述** | 位置定在 `## 最终效果` 之后、`## 模板校验（可选）` 之前（读完"怎么导出 Excel"紧接着"也能导出 JSON 给前端"，不打断主链路）。内容：一句能力说明（1.6.1 起）+ **最小 Java 示例**（从 `exportTemplate` 拿 `ExportResult`，`WebJsonExporter.instance.export(...)` 转 `WebExcel`，用 JSON 库输出字符串）+ 两句约束（"必须在导出之后调用"、"框架不绑定序列化库，任意 JSON 库均可"）。同时在 `## 特点` 末尾加一条，让能力可被发现。 |
| **依赖关系** | 依赖任务 2（示例口径与 javadoc 一致） |
| **验收标准** | 示例中每个类型/方法签名与源码逐一对应（`ExcelReport#exportTemplate`、`WebJsonExporter#export`、`WebExcel` getter）；不出现前端渲染代码；小节与「模板校验」一样标注"（可选）"。 |

### 任务 4：README 版本号与升级提示同步

| 属性 | 值 |
|------|-----|
| **描述** | 依赖片段 `<version>1.6.0</version>` → `1.6.1`；顶部第 7 行"V1.6.0 包含破坏性变更…"的提示更新为同时覆盖 1.6.1（1.6.1 也有包迁移与类删除），链接到新的 `version/V1.6.1.md`。 |
| **依赖关系** | 可与任务 3 并行（同一文件不同位置，需串行落笔） |
| **验收标准** | README 内不再把 1.6.0 当作当前版本；升级提示同时指向 V1.6.0 与 V1.6.1 详情页。 |

### 任务 5：`VERSION.md` 补 V1.6.1 条目

| 属性 | 值 |
|------|-----|
| **描述** | 在 V1.6.0 之前插入 V1.6.1 条目，结构沿用现有风格：`[更新详情](version/V1.6.1.md)` → **新增**（bridge 前端 JSON / 文本结构导出、`ExportResult`、`finalStyle`、`ReportContext` 列宽行高与 sheet 样式）→ **重构**（图表轴数据解析拆分、文本组件拆分到 `text` 包）→ **⚠️ 破坏性变更**（包迁移表、`TableComponent` 语义替换、`Container#print`/`getTypeName` 移除）。内容按 §3.1 的已核实清单写，每条都能对应到提交或源码位置。 |
| **依赖关系** | 无（与任务 1-4 并行） |
| **验收标准** | 条目中每条断言均可回溯（提交号或源码）；不把 1.6.0 的修复项误抄进 1.6.1；条目不重复 V1.6.0 已列的 `fillBackgroundColor`。 |

### 任务 6：新建 `version/V1.6.1.md` 详情页

| 属性 | 值 |
|------|-----|
| **描述** | 结构对齐 `version/V1.6.0.md`：`## V1.6.1` + 分节。至少包含：(1) 新增「报表转前端 JSON」——**含完整可跑示例**（导出 → 转 `WebExcel` → 序列化，并给出一段裁剪后的 JSON 产物示意，字段只取 `sheetName`/`rowCount`/`colCount`/`cells[].content.kind`）；(2) `ExportResult` 与 `finalStyle` 的作用；（3) ⚠️ 破坏性变更，逐条给**迁移前后代码**（`TextComponent` import、图表包路径、`TableComponent` 的"哪些会编译失败 / 哪些静默换实现"）；(4) 插件/下游影响说明。 |
| **依赖关系** | 依赖任务 5（VERSION.md 条目链接指向本文件） |
| **验收标准** | 迁移示例的旧/新全限定名与 `git diff` 的 rename 结果逐字一致（`component.text.*`、`component.TableComponent`、`chart.axis.AxisType`、`chart.component.{ChartMarker,ChartTitle,Legend}`、`chart.data.resolver.*`）；不出现框架内部实现位置的描述性路径（遵循"文档面向使用者"的口径）。 |

### 任务 7：编译与一致性自检

| 属性 | 值 |
|------|-----|
| **描述** | `JAVA_HOME=D:\Java\jdk1.8.0_231` 下执行 `mvn -o -q -DskipTests compile`；再逐条把文档里出现的类型名/方法名与源码比对一次（重点：README 示例、V1.6.1.md 迁移表）。按用户口径**不写单元测试**，运行期效果由用户自行验证。 |
| **依赖关系** | 依赖任务 1-6 |
| **验收标准** | 编译通过；文档中不残留任何不存在的方法签名（本轮已知风险点即 `export(ExportResult, String)` 这一处）。 |

## 5. 执行顺序

```
任务 1（注释对齐） → 任务 2（javadoc 用法）
任务 5（VERSION.md 条目）
        └── 任务 6（V1.6.1.md 详情页）
任务 3（README 小节） → 任务 4（README 版本号）
                                    └── 任务 7（编译 + 一致性自检）
```

- 任务 5/6 与任务 1-4 读取的文件互不重叠，可并行；任务 3 与任务 2 口径必须一致，故 2 先于 3。
- 任务 3、4 同改 `README.md`，串行落笔，避免互相覆盖。

## 6. 边界与风险

- **零行为变更**：本轮不改任何导出逻辑。`WebJsonExporter` 的注释修正只是把"声称"改成"实际"，若日后想让"不读 POI"真正成立，属另一项改动（需引入调用方传 sheetName 的入口），不在本轮。
- **文档不得反向绑定 hutool**：示例代码用 `JSONUtil` 是因为它当前在 compile 依赖里（写出来能直接跑），但必须显式声明"任意 JSON 库均可"，否则与"后期移除 hutool"的方向冲突。
- **1.6.1 已发版**：`1.6.1` 标签已在 `affe2f0`，本轮是**补记**而非发版说明的预告。文档用陈述语气写"1.6.1 起…"，不写"本次将…"。
- **`TableComponent` 是隐蔽点**：名字没变、语义全换（数据驱动表 → 布局式表），且**两个构造签名一致**，老调用照样编译通过——
  升级者从"能编译"推不出"实现已换"。V1.6.1.md 必须分别写清"哪些写法会编译失败"（无参构造、`setDataList` 等）与"哪些会静默换实现"（`new TableComponent(dataList, columns)`）。
- **`Container#print()` 移除的下游影响**：已核 `../excel-report-plugin` 未使用，但这是公开方法移除，需在破坏性变更里列出（该插件依赖 Central 上的 1.5.1，影响推迟到其升级依赖时）。
- **不新增第二份真相**：VERSION.md 条目写"是什么"，`version/V1.6.1.md` 写"怎么改"，两者不复制大段内容；README 只给最小示例，字段语义不展开。

## 7. Complements

### 1. 注释口径对齐（WebExcel / WebJsonExporter）

- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/bridge/web/WebExcel.java` — `sheetName` javadoc 删除对不存在的 `export(ExportResult, String)` 的引用；类注释删除"不依赖 Sheet 的 POI 属性"（该说法与实际读 sheet 名矛盾）
  - `src/main/java/io/github/mengfly/excel/report/bridge/web/WebJsonExporter.java` — 类注释"全程不读取 Sheet 的 POI 属性"改为"除工作表名称外"，并补一条 sheetName 来源；`export` 方法 javadoc 同步
  - `doc/web-json-exporter.md` — 更正一条历史记录里把 `export(ExportResult, String)` 记为已实现的表述
- **审查结果**：首轮 ❌（漏了 `WebExcel` 类注释这第三处同类矛盾）→ 修正后复审 ✅
- **完成时间**：2026-10-23 09:25

### 2. `WebJsonExporter` javadoc 补"如何得到 JSON 文本"

- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/bridge/web/WebJsonExporter.java` — 类 javadoc 追加"只产出结构化对象、不做序列化"的说明与调用链
- **审查结果**：首轮 ❌（javadoc 里点了 hutool / Jackson / Gson 三个库名，与"不绑定 hutool"的方向相左）→ 改为"项目已有的任意 JSON 库"后复审 ✅
- **完成时间**：2026-10-23 09:25

### 3. README 新增「导出为前端 JSON（可选）」

- **状态**：✅ 已完成
- **修改文件**：
  - `README.md` — 在「最终效果」之后、`## 模板校验（可选）` 之前新增小节：能力说明 + 最小 Java 示例 + 两条约束；`## 特点` 增第 7 条
- **审查结果**：✅ 示例签名、字段名与源码逐一对应；无前端渲染代码
- **完成时间**：2026-10-23 09:22

### 4. README 版本号与升级提示同步

- **状态**：✅ 已完成
- **修改文件**：
  - `README.md` — 依赖片段 `1.6.0`→`1.6.1`；顶部升级提示改为同时覆盖 1.6.0 与 1.6.1 并链到两个详情页
- **审查结果**：✅ README 内不再把 1.6.0 当当前版本
- **完成时间**：2026-10-23 09:22

### 5. `VERSION.md` 补 V1.6.1 条目

- **状态**：✅ 已完成
- **修改文件**：
  - `VERSION.md` — 新增 V1.6.1 段：新增 / 重构 / ⚠️ 破坏性变更（包迁移、`TableComponent` 语义替换、方法移除）
- **审查结果**：首轮 ❌（`Legend` 旧路径误写为 `chart.data.Legend`；"两个构造签名不变"表述不准）→ 修正后复审 ✅
- **完成时间**：2026-10-23 09:30

### 6. 新建 `version/V1.6.1.md`

- **状态**：✅ 已完成
- **修改文件**：
  - `version/V1.6.1.md` — 新建：前端 JSON 导出（含示例与产物示意）、`ExportResult`/`finalStyle`、破坏性变更（含"编译期暴露"与"静默换实现"的区分）、下游影响
- **审查结果**：首轮 ❌（同上的 `Legend` 路径与构造措辞；第 4 节标题不符要求）→ 修正后复审 ✅
- **完成时间**：2026-10-23 09:30
- **附注**：JSON 产物示意按真实输出核对过（跑 `BridgeExportTest#testWebJsonExport`），修正了"`link: null` 会出现在 JSON 里"的错误——hutool 默认省略 null 字段

### 7. 编译与一致性自检

- **状态**：✅ 已完成
- **执行**：
  - `JAVA_HOME=D:\Java\jdk1.8.0_231 mvn -o -q -DskipTests compile` → 通过
  - `mvn -o javadoc:javadoc` → BUILD SUCCESS（确认改过的 javadoc 不破坏打包）
  - `git diff --name-status -M v1.6.0..1.6.1` 逐条比对了 10 个 rename，迁移表全覆盖、无遗漏
- **完成时间**：2026-10-23 09:28

### 完成总结

- 7 条任务全部通过审查。**零逻辑改动**：`src/main` 下只有两个文件的 javadoc 变更（`git diff` 可验），其余全为文档。
- 原建议的"补 toJson 方法"按用户口径**不实施**——序列化留在调用方，框架只给结构。
- 审查共拦下 4 类事实错误：遗漏的第三处注释矛盾、javadoc 反向绑定 hutool、`Legend` 包路径写错、JSON 样例含不会出现的 `link: null`。
- 另有一处**计划外的发现**：新旧 `TableComponent` 双参构造签名相同 ⇒ 升级者编译器不会报警。已在 VERSION.md 与 V1.6.1.md 中区分"会编译失败的写法"与"会静默换实现的写法"。
