---
date: 2026-10-09
complete: true
description: 在 Container 上记录导出期合并后的完整样式（finalStyle），供后续从 Container 树反推完整带样式的 Excel 结构
---

# Container finalStyle 记录 - 任务规划

## 1. 需求概述

背景：框架通过 `StyleChain`（运行时样式栈）实现样式沿组件树的层级继承，但合并后的完整样式只存在于导出调用栈期间，导出结束即 `pop` 丢弃。每个 `Container` 自身只持有局部 `StyleMap`（`StyleHolder.style`），反推结构时拿不到父级继承来的样式。

目标：不新增"捕获器/收集器/监听器"，**在导出过程中把每个 `Container` 合并后的完整样式（finalStyle）记录到该 Container 自身**，导出结束后即可通过遍历 Container 树取得完整样式，作为后续"从 Container 反推带样式 Excel 结构"的数据基础。

约束（来自 `AGENTS.md`）：
- 编译/构建使用 JDK 1.8。
- 方法/字段上方的 `/** */` 只写对调用方的契约；变更说明、回归背景用 `//` 或 `/* */` 写在改动旁。
- 对 `../excel-report-plugin` 的影响需评估，目标为零影响。

## 2. 需求澄清记录

| 澄清点 | 结论 |
|---|---|
| 交付范围 | **不需要捕获/收集器**，只在 `Container` 上记录 `finalStyle` 即可（不实现 JSON/HTML 输出、不做结构化模型） |
| 暴露方式 | 挂在 `Container` 上、通过 getter 读取（未额外指定，按此默认） |
| 采集粒度 | 最终合并 `StyleMap`（几何信息 `position/measuredSize` 已在 Container 上，无需重复） |
| 字段策略 | **采用独立 `finalStyle` 字段**。评估过"直接把 merged 写回 `StyleHolder.style`"的替代方案，单次导出可行，但因无删除能力导致二次导出/换父链时旧继承样式泄漏（不可复用、非幂等），且使 `getStyle()` 语义二义（导出前=本地、导出后=完整），故否决。 |

## 3. 现状与根因（代码勘察结论）

- 样式继承是"运行时栈"语义：`Container.export()` 默认方法用 `context.getStyleChain().onStyle(getStyle(), () -> onExport(context))` 压栈（[Container.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/Container.java#L56-L58)），合并结果在 [StyleChain.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/style/StyleChain.java#L24-L37) 的 `finally` 中 `pop`，从未落到节点。
- 节点本地样式只存局部：`StyleHolder.style`（[StyleHolder.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/style/StyleHolder.java#L9-L10)）；`Container` 无 parent 指针。
- 所有 Container 实现都继承 `StyleHolder`（`AbstractComponent` / `AbstractLayout`），因此承载字段放 `StyleHolder` 可覆盖全部实现。
- `Component.super.export` / `Layout.super.export` 最终都落到 `Container.export` 默认方法 ⇒ 回填逻辑只需改一处。
- 统一落点：全部单元格都经由 `ReportContext.getCellSpan(...)`（[ReportContext.java](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/excel/ReportContext.java#L49-L59)）。
- **唯一绕过 `export()` 的路径**：`TableComponentNew` 在 `onExport` 内直接调子节点 `container.onExport(context)`（[TableComponentNew.java#L108](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/component/table/TableComponentNew.java#L108-L119)），不经过 `Container.export` ⇒ 该路径的子 Container 不会记录 `finalStyle`。
- **边界**：`ListComponent` 的表头/数据单元格样式由 `context.getCurrentChildStyle(header.getStyle())` 额外叠加（[ListComponent.java#L54-L55](file:///d:/job/excel-report/excel-report/src/main/java/io/github/mengfly/excel/report/component/list/ListComponent.java#L54-L55)），`ListHeader` 不是 `Container`，无节点可挂。

## 4. 任务列表

### 任务 1：新增 finalStyle 承载字段与契约

| 属性 | 值 |
|------|-----|
| **描述** | 在 `Container` 接口新增 `default StyleMap getFinalStyle()`（默认返回 `null`）与 `default void setFinalStyle(StyleMap)`（默认空实现），保证对外部既有实现零破坏；在 `StyleHolder` 增加 `finalStyle` 字段并实现上述两个方法（类方法天然覆盖接口 default）。字段命名 `finalStyle`，语义为"本次导出时该节点的合并后完整样式"，仅在导出后有效。 |
| **依赖关系** | 无 |
| **验收标准** | 编译通过（JDK 1.8）；未导出时 `getFinalStyle()` 返回 `null`；既有 `getStyle()` 语义与行为不变；`Container` 接口为纯新增 default 方法、不破坏任何实现。 |

### 任务 2：导出期回填 finalStyle

| 属性 | 值 |
|------|-----|
| **描述** | 在 `Container.export()` 默认方法的 `onStyle` 回调内，先 `setFinalStyle(context.getStyleChain().getStyle())`，再 `onExport(context)`。该处取到的即 `DEFAULT_STYLE + 祖先链 + 自身` 的合并结果，且是 `StyleChain` 每层新建的独立 `StyleMap` 实例，直接持有引用即可（无需拷贝）。 |
| **依赖关系** | 依赖任务 1 |
| **验收标准** | 嵌套 `H/V/Grid` + `Text` 导出后，每个 Container 的 `finalStyle` 含祖先样式与自身样式；出现同名 key 时子覆盖父；叶子组件（Text/Span/Link/Image/Chart）的 `finalStyle` 与其实际写入单元格的样式一致。 |

### 任务 3：补齐 TableComponentNew 直连 onExport 的路径

| 属性 | 值 |
|------|-----|
| **描述** | 将 `TableComponentNew` 中两处 `container.onExport(context)` 改为 `container.export(context)`。行为等价性依据：这些子节点是构造期创建的 `TextComponent`，其自身 `getStyle()` 为空 `StyleMap`，`export()` 压入空样式再弹出的合并结果与直接 `onExport` 完全一致；改动后子 Container 即可记录 `finalStyle`（= 父链 + 列样式/数据列样式）。 |
| **依赖关系** | 依赖任务 2 |
| **验收标准** | Table 模板导出后，表头子 `TextComponent.finalStyle` 含 `column.getStyle()`，数据子 `TextComponent.finalStyle` 含 `column.getDataStyle().getStyle()`；Table 导出结果（单元格值/样式）与改动前逐字节一致。 |

### 任务 4：边界确认（List 内部样式）

| 属性 | 值 |
|------|-----|
| **描述** | 明确 `ListComponent` 内部表头/数据单元格的附加样式无对应 Container，无法随本机制记录。本轮不引入新的承载结构；在文档中写明该边界，并给出反推侧取值方式：`ListComponent.getFinalStyle()` 仅代表组件自身（含祖先），表头附加样式需另行读取 `ListHeader.getStyle()` 后合并。 |
| **依赖关系** | 依赖任务 2 |
| **验收标准** | 边界写入文档；反推侧取值方式明确；不因此任务改动 `ListComponent` / `ListHeader` 行为。 |

### 任务 5：测试与回归

| 属性 | 值 |
|------|-----|
| **描述** | 新增单测（JUnit4，风格对齐 [ExcelCellSpanTest.java](file:///d:/job/excel-report/excel-report/src/test/java/io/github/mengfly/excel/report/excel/ExcelCellSpanTest.java)），覆盖：(a) 嵌套继承；(b) 子覆盖父；(c) Table 列样式记录；(d) 未导出时 `getFinalStyle()` 为 `null`。并回归既有 `ExcelCellSpanTest`、`ExcelReportSheetNameTest` 及模板类测试。 |
| **依赖关系** | 依赖任务 1–4 |
| **验收标准** | `mvn test` 全绿；新增用例能证明父级样式已被正确带入子节点 `finalStyle`。 |

## 5. 执行顺序

```
任务 1（承载字段/契约）
   └─ 任务 2（导出期回填）
        ├─ 任务 3（Table 直连路径补齐）
        └─ 任务 4（List 边界确认）
             └─ 任务 5（测试与回归）
```

任务 3 与任务 4 无相互依赖，可并行；任务 5 在全部完成后统一回归。

## 6. 边界与风险

- `finalStyle` 仅在**导出（measure → layout → export）之后**有效，反推须在导出完成后进行。
- `GridLayout` 在 `onLayout/onExport` 期间动态构造临时包装布局（不进入 `getContainers()`）；反推走 `getContainers()` 返回的用户节点，其 `finalStyle` 已由真实导出路径记录，正确。
- `TableComponentNew` 的子 `TextComponent` 自身样式为空是任务 3 等价性的前提；若后续给其注入非空样式，`onExport → export` 的等价性需重新评估。
- **已知边界（List 内部样式）**：`ListComponent` 的表头/数据单元格通过 `context.getCurrentChildStyle(header.getStyle())` 在叶子内部额外叠加样式，`ListHeader` 不是 `Container`，无节点可挂 ⇒ 该附加样式**不进入** `finalStyle`。反推侧取值方式：`ListComponent.getFinalStyle()` 仅代表组件自身（含祖先），表头附加样式需另行读取 `ListHeader.getStyle()` 后与之一并合并。
- 插件影响：任务 1 为接口纯新增 default 方法 + 基类字段；`../excel-report-plugin` 仅走模板渲染、不进入导出链路 ⇒ 预期零影响，实现时确认插件未实现 `Container`/未覆写相关方法。
- 注释规范：契约写 `/** */`；"为何改 onExport→export""finalStyle 的有效期"等变更说明用 `//` 写在改动旁。

## 7. Complements

### 1. 新增 finalStyle 承载字段与契约
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/Container.java` — 新增 `getFinalStyle()/setFinalStyle()` 两个 default 方法（零破坏）
  - `src/main/java/io/github/mengfly/excel/report/style/StyleHolder.java` — 新增 `finalStyle` 字段并实现上述方法
- **审查结果**：编译通过（JDK 1.8）
- **完成时间**：2026-10-09

### 2. 导出期回填 finalStyle
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/Container.java` — `export()` 默认方法内取 `styleChain.getStyle()` 回填
- **审查结果**：`FinalStyleTest` 继承/覆盖用例通过
- **完成时间**：2026-10-09

### 3. 补齐 TableComponentNew 直连 onExport 路径
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/component/table/TableComponentNew.java` — 108/119 行 `container.onExport(context)` → `container.export(context)`
- **审查结果**：`FinalStyleTest.tableColumnStyleShouldReachChildContainer` 通过（列样式/数据列样式均被记录）
- **完成时间**：2026-10-09

### 4. List 内部样式边界确认
- **状态**：✅ 已完成
- **修改文件**：
  - `doc/container-final-style.md` — 在「边界与风险」写明 List 边界及反推侧取值方式（不改 ListComponent/ListHeader 行为）
- **审查结果**：文档记录完整
- **完成时间**：2026-10-09

### 5. 测试与回归
- **状态**：✅ 已完成
- **修改文件**：
  - `src/test/java/io/github/mengfly/excel/report/excel/FinalStyleTest.java` — 新增 4 个用例（继承/覆盖/导出前为 null/Table 列样式）
- **审查结果**：全量 `mvn test` 43 项全绿，BUILD SUCCESS
- **完成时间**：2026-10-09
