---
date: 2026-10-23
description: 修正 VLayout / HLayout 弹性子节点的尺寸分配（内容保底 + 剩余按权重），并在内容放不下时给出点明模板节点的告警
---

# 弹性布局分配修正（弹性子节点溢出无告警） - 任务规划

> 需求来源：AI 巡检 2026-10-23（任务 `r_mvkjzt9xu8rbk` / #2）
> 关联文档：`doc/code-review.md` 002（框架侧，本需求的正身）、003（宽度塌陷，本次不做）

## 1. 需求概述

**现状问题**：`size` 中高度写 `-1` 的语义是"弹性"——父级 `VLayout` 按 `weight`（默认 1）**均分剩余高度**，
与子节点自己的**内容高度无关**。当多个弹性子节点内容高度不等时，内容多的那个分到的行数不足，
`VLayout` 既不裁剪也不告警，子元素继续顺序排布、溢出到下一个兄弟节点的行区间，直到导出阶段 POI
`addMergedRegion` 才抛 `IllegalStateException: Cannot merge ... overlaps ...`，报错信息只有单元格坐标。

**目标**：让弹性分配在"内容放得下"的前提下**一定放得下**（而不是靠用户猜权重），
放不下时在**分配阶段**就给出带模板节点路径与尺寸的 WARN。

**本次不追求**：把"内容放不下"变成不失败——行数真不够时物理上无法容纳，只能提前暴露。

## 2. 需求澄清记录

| 决策点 | 结论 | 理由 |
|---|---|---|
| 弹性高度的默认分配语义 | **内容保底 + 剩余按权重**（改默认行为） | 内容高度打平时与现状"等分"完全等价（见 §4.2），行为变化只落在"现在就必崩"的输入上 |
| 内容合计 > 可用高度时 | **按各自内容比例压缩 + WARN** | 谁内容多谁分得多，避免"内容小的白占、内容大的独自爆掉"；不改成抛异常（真实数据一变就从"有瑕疵"变"完全导不出"） |
| 告警形式 | **逐节点 WARN**，带 tag 链与尺寸 | 与项目既有 `log.warn` 风格一致，定位最直接；不攒批、不接 `setStrict` |
| 是否同步改 HLayout 宽度 | **一起改，共用同一算法** | 横向走同一套 `WeightSizeHelper`，属同源静默溢出（`code-review` 003 旁注"本次未实测"）；逻辑收在一处，横纵语义一致 |

## 3. 现状与根因（关键证据）

`target/probe/out.txt` 中该模板的真实测量结果：

| 节点 | `getSize()`（内容） | `measured`（分配） | 结果 |
|---|---|---|---|
| 根 `VLayout` | 31,159 | 31,159 | 159 = 47（固定）+ 112（3 个弹性包裹层内容之和） |
| 包裹层 1（2 条） | 0,32 | 31,37 | 底部留白 5 行 |
| 包裹层 2（3 条） | 0,48 | 31,37 | **溢出 11 行 ⇒ 崩** |
| 包裹层 3（2 条） | 0,32 | 31,38 | 底部留白 6 行 |

关键点：**框架在分配那一刻就已经知道正确答案**。

- `VLayout.getSize()` = `sumContainerSize(...)`，对固定高度子节点累加其声明高度，对弹性子节点（`height<0`）记 1
  ⇒ 对"内容由固定高度子元素组成"的包裹层，`getSize().height` **就是内容高度**（32 / 48 / 32）。
- 根节点 `onMeasure()` 无参调用时用 `onMeasure(getSize())` ⇒ 可用高度 159 **本身就等于内容之和**。
- 而 `WeightSizeHelper` 的分配公式里只有 `weight`，`getSize()` 只被用来判断"是否弹性"（`-1` 即弹性），
  **内容高度从未参与计算** ⇒ 112 ÷ 3 = 37/37/38。

结论：本案例不是"空间不够"，而是"分配算法无视已知的内容高度，把恰好够用的空间分错了"。

## 4. 方案设计

### 4.1 算法：`flex-basis = 内容高度`，等价于 CSS 的 `flex-basis: content + flex-grow: weight`

统一在 `WeightSizeHelper` 内实现（`SIZE_TYPE_HEIGHT` / `SIZE_TYPE_WIDTH` 共用），
**构造期一次性算出分配表**，`distributionSize(container)` 改为查表 ⇒ 变为幂等、无副作用（旧实现每次调用扣减剩余，重复调用结果不同）。

```
available   = total(轴)
fixedSum    = Σ 固定子节点尺寸（getChildSize >= 0）
E           = 弹性子节点（getChildSize < 0），w_i = weight（<=0 视为 1）
basis_i     = max(1, child.getSize()[轴])      // 内容尺寸；弹性叶子（-1）仍按 1
remaining   = available - fixedSum
sumBasis    = Σ basis_i

if (remaining >= sumBasis) {                   // 放得下：保底 + 剩余按权重（保持旧实现的顺序扣减，让末尾节点吸收取整余数）
    surplus = remaining - sumBasis
    for (c in E 按容器顺序) {
        extra = (int)(surplus * w_c / Σw_剩余)
        alloc_c = basis_c + extra
        surplus -= extra;  Σw_剩余 -= w_c
    }
} else {                                       // 放不下：按内容比例压缩 + 登记溢出（同时仍保证每个 >= 1）
    pool = max(remaining, 0)
    for (c in E 按容器顺序) {
        alloc_c = max(1, (int)((long) pool * basis_c / Σbasis_剩余))
        pool -= alloc_c;  Σbasis_剩余 -= basis_c
    }
}
```

对**非弹性子节点**的返回值保持不变（仍是声明尺寸），因此固定尺寸的排版结果一字不变。

### 4.2 为什么"内容打平时零差异"

设 n 个弹性子节点、`basis_i` 全为 b、`Σw = W`：

- 旧：`alloc_i = available × w_i / W`
- 新：`alloc_i = b + (available − n·b) × w_i / W = available/n`（当 `w_i` 全等时）

代入 `b = available/n` 即得两者相同 ⇒ 三组条数相同的 2/2/2 场景走新算法**逐字节应与旧版一致**（这是本次的回归基线）。
差异只出现在 `basis` 不等的场景，也就是旧算法必然分错的场景。

### 4.3 告警

分配完成后，对每个"弹性且 `alloc < basis`"的子节点打一条 WARN（`log.warn`，slf4j 已就位）：

```
布局内容溢出：VLayout > VLayout[size=-1,-1]（父容器第 13 个子节点）分到 22 行，内容需要 48 行（缺 26 行）；
父容器可用 100 行，子节点内容合计 159 行。继续导出写合并区时会抛 IllegalStateException，请检查该节点 size 的高度是否偏小（-1 表示弹性）。
```

实现要点：

- 节点路径由 `container.templateNode()` 向上取 `parent` 链拼 tag（根在左），附带该节点自身的 `size` 属性值；
  再补一个"父容器内第 N 个子节点"用来区分内容完全同构的几个包裹层。
- **去重**：布局实例内用 `Set<Container>` 记住已告警的子节点（`GridLayout` 在 `onLayout`
  里会重新 `onMeasure` 一次，不去重会重复打印）。
- **合成布局不告警**：`GridLayout#getContainerGroup` 每次临时 new 出来的 `VLayout`/`HLayout` 没有 `templateNode`，
  真因在它的真实父级，跳过（否则每次 `getSize()`/`onMeasure`/`onLayout` 都可能重复）。

### 4.4 已知局限（写进 doc，不在本次解决）

1. **弹性叶子的内容不可知**：`Text`/`Image` 自身 `height=-1` 时没有内容尺寸，`basis` 仍按 1 计（内容高度的下界）。
2. **根级的"可用高度"恒等于内容之和**：`ExcelReport#exportSheet` 走 `container.onMeasure()` → `onMeasure(getSize())`，
   而根 `VLayout.getSize().height` = 固定高度之和 + 各弹性子节点内容之和 ⇒ 顶层**不会**出现"空间不够"，
   本次算法在顶层必然刚好放下。因此溢出告警实际只会在两种情况下触发：调用方直接 `onMeasure(Size)` 给了更小的可用尺寸、
   或父级自身被上层压小（`HLayout` 给子节点的高度永远是父级自高度）。
3. **`size` 声明的高度对 layout 类子节点不生效**：`VLayout`/`HLayout`/`GridLayout`/`TableComponent` 的 `getSize()`
   都覆盖成"主轴内容累加、交叉轴取最大"（2026-10-26 起交叉轴在子项全为弹性负数时返回 -1，表示继承父级），
   父级 `WeightSizeHelper` 拿到的是内容值而非声明值 ⇒ "把高度写死写小"只会对叶子组件成立。
   这也是 `code-review.md` 001「备选方案（写死高度）不推荐」的另一个原因：连"写死"都不一定按写的值生效。
4. **重复 render 会重复告警**：告警按布局实例去重，同一模板批量导出 N 次仍是 N 条（与"逐节点"口径一致，符合预期）。

### 4.5 不在本次范围

- ~~`code-review.md` 003（省略 `size` 时宽度塌成 0）~~：已由 `doc/layout-cross-axis-auto-size.md`（2026-10-26）修复——
  `VLayout`/`HLayout` 的 `sumContainerSize` 交叉轴在"子项非空且全为负"时返回 -1，省略 `size` 的布局因此继承父级可用尺寸。
- 新增"内容自适应"显式语义（如 `size` 高度写 `-2` / 新 style key）：属改 XSD 协议 + 同步插件校验（见台账"扩展面口径"），本次用"改默认分配算法"达成同样效果。
- 导出期 `addMergedRegion` 失败时补模板节点信息（可做但会把模板树塞进 `ReportContext`，另立议题）。

## 5. 任务列表

### 任务 1：`WeightSizeHelper` 分配算法重构（内容保底 / 按比例压缩）

| 属性 | 值 |
|------|-----|
| **描述** | 按 §4.1 在构造期算出 `Map<Container, Integer>` 分配表；`distributionSize` 改为查表（幂等）。保留 `getChildSize` / `getContainerWeight` 语义，新增对外可见的"分配明细"（`basis` / 是否溢出 / 可用与内容合计），供上层告警使用 |
| **依赖关系** | 无（可先行） |
| **验收标准** | `JAVA_HOME=D:\Java\jdk1.8.0_231 mvn -o -q -DskipTests compile` 通过；非弹性子节点返回值与旧实现一致；`distributionSize` 对同一 container 连续调用两次返回值相同 |

### 任务 2：`VLayout` / `HLayout` 接入 + 溢出告警

| 属性 | 值 |
|------|-----|
| **描述** | 两个布局的 `measure(...)` 保持签名不变（`public static void measure(List<Container>, Size)`），改用新的分配表；分配后对溢出的弹性子节点打 WARN（§4.3），含 tag 链、`size` 属性、分配值、内容值、缺口、父容器可用与内容合计。类型按轴措辞区分（行 / 列） |
| **依赖关系** | 依赖任务 1 |
| **验收标准** | 同类编译通过；`GridLayout` 合成布局不产生告警；同一次测量中同一子节点只告警一次；`VLayout`/`HLayout` 的 `measure` 方法签名与可见性不变 |

### 任务 3：文档与注释

| 属性 | 值 |
|------|-----|
| **描述** | ① `doc/code-review.md` 002 行状态更新为已修并追加"修复记录"小节（算法、边界、已知局限、回归建议、提交号）；② 下一个版本的 `version/Vx.y.z.md` + `VERSION.md` 记一条**行为变更**：弹性分配改为内容保底；③ 框架内注释遵守 `AGENTS.md` 约定：契约写 `/** */`，变更说明/实测数据写 `//` |
| **依赖关系** | 依赖任务 2（需拿到真实提交号与实测结论） |
| **验收标准** | 三处文档均能独立看懂"改了什么 / 为什么 / 什么情况下会变"；`doc/code-review.md` 的 002 与新小节互不矛盾 |

### 任务 4：端到端验证（用户执行，AI 只提供命令与判据）

| 属性 | 值 |
|------|-----|
| **描述** | 复用 `target/probe` 现有夹具（`orig.xml` + `data-eq2.json` / `data.json`），见 §7 |
| **依赖关系** | 依赖任务 2 |
| **验收标准** | 见 §7 的两组判据 |

## 6. 执行顺序

```
任务 1 ──► 任务 2 ──► 任务 4（用户验证）
                 └──► 任务 3（拿到提交号后补文档）
```

任务 3 可与任务 4 并行，但提交号必须在任务 2 落地后才有。

## 7. 验证方案（用户执行）

改动前后各跑一次，判据三条：

1. **等量场景必须零差异**（证明没改坏现有排版）：`orig.xml` + `data-eq2.json`（三组均 2 条）
   ⇒ 导出后 `xl/worksheets/sheet1.xml`（含 `<cols>`）与改动前**逐字节相同**。
2. **不等量场景从崩到对**：`orig.xml` + `data.json`（2/3/2）
   ⇒ 改动前：`EXPORT FAILED: IllegalStateException: Cannot add merged region ...`
   ⇒ 改动后：`EXPORT OK`，且三个包裹层分配高度 = 32 / 48 / 32（内容高度），无留白、无溢出。
3. **告警可见性**：`orig.xml` + 可用高度不足（`MeasureProbe orig.xml data.json 100`，模拟真的放不下）
   ⇒ 3 条 WARN，能直接看到模板标签链与"分到 X 行，内容需要 Y 行"。

### 已完成的实测（2026-10-23，JDK8 + 真实 POI 链）

| 用例 | 结果 |
|---|---|
| `orig.xml` + `data.json`（2/3/2） | 分配 **32/48/32**，位置 39/71/119，无重叠，`merge-able rects = 242`（与 001 手工修模板的参考值一致） |
| `orig.xml` + `data-eq2.json`（2/2/2） | 32/32/32，与旧分配一致，无 WARN |
| `MeasureProbe orig.xml data.json 100`（可用 100 行，真放不下） | 3 条 WARN（15/22/16 行按内容比例压缩）；文案见 §4.3 |
| 现有测试套件（60 用例） | 59 通过；`SimpleTemplateTest.testLayoutReport` 为**既有**失败（找 `TestLayoutReport.xlsx.xml`，资源只有 `TestLayoutReport.xml`） |


> 探针可继续用 `javac -encoding UTF-8` + `java -Dfile.encoding=UTF-8` 编 `target/probe/Probe.java`（打印树+两两判交）
> 与 `ExportProbe.java`（走真实 POI 链看异常/合并区）。

## 8. 影响面与兼容性

| 项 | 评估 |
|---|---|
| 公开 API | `WeightSizeHelper` 是公开类，`distributionSize` 签名不变、语义由"有副作用的顺序调用"改为"幂等查表"（更安全）；`VLayout/HList.measure` 签名不变 |
| 兄弟仓库 `excel-report-plugin` | 已 grep：不引用 `WeightSizeHelper` / `onMeasure` / `VLayout.measure`，本次无 import 同步需求（插件依赖 Central 上的 1.5.1 jar，时间差照旧） |
| 行为变更 | 仅影响"同一父级下有多个弹性子节点且内容尺寸不等"的模板：分配会向内容靠拢；内容打平时结果不变 |
| 性能 | 每个弹性子节点多一次 `getSize()`（递归累加，本来 `sumContainerSize` 也在调）；分配从"边算边扣"变为"一次算完"，量级不变 |
| XSD / 模板协议 | 不改 |

## Complements

### 任务 1：`WeightSizeHelper` 分配算法重构
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/util/WeightSizeHelper.java` — 删除有状态的 `waitForDistributionSize/Weight`，改为构造期建立 `IdentityHashMap<Container,Integer>` 分配表；新增 `distributeSurplus`（内容保底 + 剩余按 weight）/ `distributeDeficit`（按内容比例压缩、最小 1）/ `getContentSize`；`distributionSize` 改为查表（幂等）
- **审查结果**：见下方"审查"
- **完成时间**：2026-10-23 16:50

### 任务 2：`VLayout` / `HLayout` 接入 + 溢出告警
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/layout/AbstractLayout.java` — 加 `@Slf4j`、`overflowWarned` 去重集合，新增 `reportOverflow(int sizeType)` / `contentOf` / `nodePath`
  - `src/main/java/io/github/mengfly/excel/report/layout/VLayout.java` — `onMeasure` 末尾 `reportOverflow(SIZE_TYPE_HEIGHT)`
  - `src/main/java/io/github/mengfly/excel/report/layout/HLayout.java` — `onMeasure` 末尾 `reportOverflow(SIZE_TYPE_WIDTH)`
- **验收证据**：`orig.xml + data.json`（2/3/2）分配 32/48/32、无重叠、242 个合并区；`MeasureProbe` 可用 100 行时 3 条 WARN
- **完成时间**：2026-10-23 16:52

### 任务 3：文档与注释
- **状态**：✅ 已完成（一处缩小范围，见下）
- **修改文件**：
  - `doc/code-review.md` — 002 行标记已修 + 新增「002 修复记录」（根因、改动表、等价性、实测表、已知边界）
  - `doc/flex-layout-allocation.md` — §4.3 换成实际告警文案、§4.4 换成实现期确认的四条边界、§7 补实测结果
- **缩小的范围**：原计划的"下一个版本 `version/Vx.y.z.md` + `VERSION.md` 记行为变更"**未做**——版本号由发布流程决定，不宜由本次改动代填；行为变更已完整记入 `doc/code-review.md`，发布时可直接摘入版本说明
- **完成时间**：2026-10-23 16:55

### 审查

第一轮审查（子代理，对照本文档逐条核）：核心算法、幂等性、等价性、除零/溢出边界均通过；提出 8 条，处理如下。

| # | 问题 | 处理 |
|---|---|---|
| D1 | 告警只比"分配 < 内容"，会把 `size="31,0"` 的占位节点和 `<PageRowSplit/>`（`getSize()=(0,0)`）判成溢出 ⇒ 天天误报 | ✅ 已修：`WeightSizeHelper.isFlexSize` 抽出"是否弹性"的判定，`reportOverflow` 只看弹性子节点。实测含零尺寸节点与 `PageRowSplit` 的模板：0 条告警 |
| D2 | 计划里承诺"对外可见的分配明细"，实现未暴露；`contentOf` 与 `getContentSize` 是同一口径的两份拷贝 | ✅ 已修：`WeightSizeHelper.contentSizeOf(child, sizeType)` 公开，`AbstractLayout` 改用它（删掉重复实现） |
| D3 | 弹性子节点的 `getSize()` 由 1 次变 3~4 次，而 `Table` 的 `getSize()` 会重建整棵合成布局 | ✅ 已修：basis / weight 构造期各算一次并缓存；告警阶段内容尺寸单次预计算 |
| D6 | 同一处 `getSize()`/`getContainerWeight` 各算两次，理论上余和可能失配 | ✅ 同上（缓存即消除） |
| D4 | `overflowWarned` 生命周期 = 布局实例，同一实例"溢出→不溢出→再溢出"会静默 | 不改：布局实例由 `render` 逐次创建，实例寿命 = 一次导出，不存在跨数据复用 |
| D5 | 固定子节点本身超可用时不告警 | 不改：属另一类问题，且弹性叶子分到 1 行已等于其 basis；已记入 `code-review.md`「已知边界」 |
| D7 | 同一 Container 实例被 `addItem` 两次时 `IdentityHashMap` 只存一份 | 不改：框架不会重复 add，模板 `for` 每次迭代都新建实例 |
| D8 | 存量用例不覆盖弹性分配三个新分支 | 不改：按项目约定不写单测；改由 `target/probe` 的四个场景（2/3/2、2/2/2、可用 100 行、零尺寸）覆盖 |

修正后复验：`mvn -o -q -DskipTests compile` 通过；2/3/2 → 32/48/32 + 242 合并区；可用 100 行 → 3 条 WARN；零尺寸模板 → 0 条 WARN；测试套件 59/60（唯一失败为既有资源名问题）。

第二轮复审（针对上面的修正）结论：通过。仅剩一处口径不一致，已补：

- `isFlexSize` 原本只认"声明的 `size` 该轴为负"，而分配侧对 `preferredSize` 缺失的节点会走 `getSize()`，
  于是一个 `getSize()` 该轴为负、但没声明 `preferredSize` 的程序化构造节点会"按弹性分配却不告警"。
  现统一为：声明的 `size` 该轴为负 **或** 未声明而 `getSize()` 该轴为负 ⇒ 弹性（与分配侧完全同口径）。
  模板路径不会出现这种节点（`ContainerParser` 总会写入 `preferredSize`），此修正对模板零影响。
