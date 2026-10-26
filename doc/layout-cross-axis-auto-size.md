---
date: 2026-10-26
description: 修正 VLayout / HLayout 交叉轴尺寸：省略 size 的内层布局在子项全部为弹性负数时输出 -1，从而继承父级可用尺寸，消除宽度/高度静默塌陷
---

# 省略 size 的布局应继承父级尺寸 - 任务规划

> 需求来源：AI 巡检 2026-10-24（任务 `r_mvmai9oo9b28g` / #4）
> 关联文档：`doc/code-review.md` 003（本需求正身）、`doc/flex-layout-allocation.md` §4.5（上一轮明确延后本项）

## 1. 需求概述

**现状问题**：模板作者要表达「宽度自适应、高度随内容」时，`Size` 必须写两个值，唯一不写死数字的写法是省略 `size`。
但布局类 `getSize()` 的交叉轴取「`max(0, 子项该轴尺寸...)`」，当子项该轴全为 `-1`（弹性）时结果是 **0**；
`ContainerParser` 又把省略 `size` 时的 `getSize()` 原样写入 `preferredSize`，父级看到 `0 >= 0` 便按「确定 0」
处理、不再回退父级可用尺寸 ⇒ 整块子树被测量成 0 宽/0 高，图片不可见、文本 0 宽，无异常也无告警。

**目标**：让「省略 `size` 的内层布局」在交叉轴报告 `-1`（弹性/auto），由父级用自己的可用尺寸填充；
主轴仍按内容累加。这样「宽度（或高度）自适应 + 另一轴随内容」可以只靠省略 `size` 表达。

**本次范围**：仅改 `VLayout.sumContainerSize` 的宽度与 `HLayout.sumContainerSize` 的高度（交叉轴）。
不新增 `size` 取值语义、不改 XSD、不改公开方法签名。

## 2. 需求澄清记录

| 决策点 | 结论 | 理由 |
|---|---|---|
| 「全为弹性负数」遇到 0 怎么算 | **仅严格全部 `< 0` 才返回 -1**；含 0 时保持现有 max（通常为 0） | `size="0,0"`（如 `PageRowSplit`）是显式声明，不应因相邻 `-1` 而整块变弹性 |
| 空布局（无子项） | **返回 0**，保持现状 | 否则「全为负数」在空集合上恒真，空布局会意外撑满父级 |
| 变动范围 | **`VLayout` 宽度与 `HLayout` 高度都改**，接受 `GridLayout` 间接变化 | 横纵语义对称；`GridLayout.getSize()` 委托临时合成的布局，全弹性子项的网格会由 0 变为继承父级，属预期行为变化，写入文档 |
| 验证方式 | **`target/probe` 临时探针（不入库）+ 真实模板人工验证** | 遵循项目「不写单元测试」约定；上一轮 `flex-layout-allocation` 同样采用探针 + 真实模板 |
| 判定的另一轴 | 主轴（`VLayout` 高度 / `HLayout` 宽度）**不动** | 主轴是累加值，`-1` 会破坏求和；且该值已被 `WeightSizeHelper` 当作弹性子节点的 flex-basis 使用 |

## 3. 现状与根因（关键证据）

真实模板（`code-review.md` 003，图片循环包裹层删掉 `size="-1,-1"`）：

```xml
<VLayout for="img : images">          <!-- 省略 size，本意：宽度撑满、高度随内容 -->
  <Text  size="-1,1">标题</Text>
  <Image size="-1,15"/>
  ...
</VLayout>
```

| 步骤 | 代码位置 | 结果 |
|---|---|---|
| 内层 `getSize()` | `VLayout.java:50` | `width = max(0, -1, -1) = 0`；`height = 1 + 15 + ... = 32` |
| 省略 size ⇒ `preferredSize` | `ContainerParser.java:33-36` | `preferredSize = (0, 32)` |
| 父级取子项宽度 | `VLayout.java:79-92` | `preferredSize.width = 0`，不满足 `< 0`，返回 0，**不回退父级宽度** |
| 子树测量 | `VLayout.measure` | 包裹层 `measured = (0, 32)`，子元素拿到建议宽度 0 |

实测（`code-review.md` 003）：包裹层 `measured=(0,32)`，其子元素 `measured=(0,1)`、`(0,15)`。

## 4. 方案设计

### 4.1 改动点

`VLayout.sumContainerSize`（交叉轴 = 宽度）：

```text
width = 子项为空 ? 0
      : 所有子项 width < 0 ? -1
      : max(0, 所有子项 width)
```

`HLayout.sumContainerSize`（交叉轴 = 高度）对称处理。主轴逻辑（VLayout 高度求和 / HLayout 宽度求和）保持不变。

实现时每个子项只取一次 `container.getSize()`，避免同一循环里重复递归计算。

### 4.2 为什么这样就能「继承」

`-1` 在框架内已经是「弹性/未定」的统一信号：

- `VLayout.getChildContainerSuggestSize`：`preferredSize.width < 0` ⇒ 返回父级 `suggestSize.width`；
- `HLayout.getChildContainerSuggestSize`：`preferredSize.height < 0` ⇒ 返回父级 `suggestSize.height`；
- `WeightSizeHelper.isFlexSize`：把该轴为负的节点判为弹性，交给父级分配。

因此省略 `size` 的内层布局在交叉轴报告 `-1` 后，父级会用自己的可用尺寸填充它，其子元素再拿到正确宽度。

改动后同一条链：`getSize() = (-1, 32)` ⇒ `preferredSize = (-1, 32)` ⇒ 父 `VLayout` 可用宽 31 ⇒
包裹层 `measured = (31, 32)` ⇒ 子元素宽度 31。

### 4.3 行为变化与影响面

| 项 | 评估 |
|---|---|
| 触发条件 | 仅当布局的子项在交叉轴上**全部为负**（现实中即「只删 size、子项仍写 `-1`」）时结果由 0 变 -1 |
| 非触发条件 | 有任一子项该轴 `>= 0` 时结果完全不变；含 0 的组合、空布局、主轴累加均不变 |
| `GridLayout` | `getSize()` 委托合成 `VLayout/HLayout`；2026-10-26 实测：全弹性交叉轴的网格由 0 变为“组数量”（横向 `(2,0)→(2,2)`、纵向 `(0,2)→(2,2)`），**不是直接继承父级**（合成根布局的对应轴是求和/取正，不会产出 -1）。属预期修复，但确切数值以实测为准 |
| `TableComponent` | `getSize()` 委托 `GridLayout`，同上；普通表格子项均有声明尺寸，不触发 |
| 交替嵌套 | `VLayout` 在 `HLayout` 内（或反之）时，交叉轴的 -1 落在父级主轴，会被判为弹性并按 `weight` 分配（实测 `measured` 由 0 变 90），不是“继承父级尺寸” |
| 公开 API | `VLayout.sumContainerSize` / `HLayout.sumContainerSize` 为 `public static`，签名不变、返回值可能新增 `-1`；`getSize()` 的契约需在 javadoc 中补充 |
| 兄弟仓库 `excel-report-plugin` | 按上一轮核查结论未引用 `sumContainerSize` / 布局测量 API；若可访问，提交前再 grep 一次确认 |
| XSD / 模板协议 | 不改；省略 `size` 的既有模板可能因此从「0 宽」变为「撑满」，属修复 |
| 性能 | 每个子项一次 `getSize()`（原实现两次），量级不变 |

## 5. 任务列表

### 任务 1：`VLayout` / `HLayout` 交叉轴 auto 语义

| 属性 | 值 |
|------|-----|
| **描述** | 按 §4.1 修改两个 `sumContainerSize`：交叉轴在「子项非空且全部 `< 0`」时返回 -1，否则保持现有 max；主轴逻辑不变。按 `AGENTS.md` 注释约定：方法契约（交叉轴可能返回 -1 表示继承父级）写 `/** */`，变更原因与实测写 `//` |
| **依赖关系** | 无（可先行） |
| **验收标准** | ① `JAVA_HOME=D:\Java\jdk1.8.0_231 mvn -o -q -DskipTests compile` 通过；② 构造检查：全 `-1` 子项 ⇒ 交叉轴 -1；含 0 ⇒ 0；含正数 ⇒ 原 max；空布局 ⇒ 0；主轴结果与改动前一致；③ 公开方法签名与可见性不变 |

### 任务 2：探针实测与回归

| 属性 | 值 |
|------|-----|
| **描述** | 在 `target/probe` 下编写临时探针（`javac -encoding UTF-8` 编译，**不入库**），覆盖：① 省略 size 的内层 `VLayout` 在父宽 31 下的 `getSize()` 与 `measured`；② `HLayout` 高度对称场景；③ 子项全 `-1` 混合 `0` ⇒ 0；④ 空布局 ⇒ 0；⑤ `GridLayout` 间接影响实测（记录实际数值，不预设“继承父级”）；⑥ 交替嵌套（`VLayout` 在 `HLayout` 内）实测 -1 落在父级主轴时按弹性/权重分配；⑦ 回归：`src/test/resources/TestLayoutReport.xml` 导出 `xl/worksheets/sheet1.xml` 与改动前逐字节比对。同时跑一次现有测试套件，确认失败集合与改动前一致（含既有失败） |
| **依赖关系** | 依赖任务 1 |
| **验收标准** | ① 探针输出符合 §4.2 的预期值（内层 `measured.width` 由 0 变 31）；② 未触发场景（含 0/正数/空）与改动前一致；③ 回归模板导出逐字节相同；④ 测试套件结果不新增失败 |

### 任务 3：真实模板人工验证（用户执行，AI 提供命令与判据）

| 属性 | 值 |
|------|-----|
| **描述** | 使用 `WarningDayReport.xml`（兄弟工程），删除图片循环包裹层的 `size="-1,-1"`、子元素保持 `-1`，导出验证：图片宽度铺满 A..AE、三组列表不与后续节点重叠 |
| **依赖关系** | 依赖任务 2 |
| **验收标准** | ① 导出成功、无 `Cannot add merged region ... overlaps`；② 图片不再 0 宽；③ 若保留原 `size="-1,-1"` 写法，导出结果与改动前一致 |

### 任务 4：文档同步

| 属性 | 值 |
|------|-----|
| **描述** | ① `doc/code-review.md` 003 行标记已修并追加「003 修复记录」（根因、改动、边界、实测）；② `doc/flex-layout-allocation.md` §4.5 更新「003 需改交叉轴语义」的表述为已完成/已评估；③ 行为变更说明（含 `GridLayout` 间接影响、交替嵌套按权重分配）写入上述修复记录，发布时可摘入版本说明（版本号由发布流程决定，本次不代填） |
| **依赖关系** | 依赖任务 1、2（需实测数据；提交号/版本号由发布流程决定，本次不代填） |
| **验收标准** | 两处文档互不矛盾，能独立看懂「改了什么 / 为什么 / 什么情况下会变」 |

## 6. 执行顺序

```
任务 1 ──► 任务 2 ──► 任务 3（用户验证）
                 └──► 任务 4（实测与代码落地后补文档）
```

任务 4 可与任务 3 并行，但需在任务 1/2 落地、拿到实测数据后编写。

## Complements

### 1. 实现 VLayout / HLayout 交叉轴 auto 语义
- **状态**：✅ 已完成
- **修改文件**：
  - `src/main/java/io/github/mengfly/excel/report/layout/VLayout.java` — `sumContainerSize` 宽度：子项非空且全部 `<0` 时返回 -1，否则保持原 max；子项只取一次 `getSize()`；补 javadoc 契约与 `//` 变更说明
  - `src/main/java/io/github/mengfly/excel/report/layout/HLayout.java` — 高度对称处理
- **审查结果**：通过（空集合 0 / 全负 -1 / 含 0 为 0 / 含正数为原 max；主轴与签名未变）
- **验证**：`JAVA_HOME=D:\Java\jdk1.8.0_231 mvn -o -q -DskipTests compile` 通过
- **完成时间**：2026-10-26 09:47

### 2. 探针实测与回归
- **状态**：✅ 已完成
- **产物**：`target/probe/CrossAxisProbe.java`（临时探针，不入库）、`target/probe/baseline-classes`（`git show HEAD` 取改动前 VLayout/HLayout 单独编译）、`before.txt` / `after.txt`、`before.xlsx` / `after.xlsx`
- **实测结果**：

| 场景 | 改动前 | 改动后 |
|---|---|---|
| 内层 `VLayout` 省略 size，父级可用宽 31 | `size=0,16`、`measured=0,16`；子元素 `0,1` / `0,15` | `size=-1,16`、`measured=31,16`；子元素 `31,1` / `31,15` |
| 内层 `HLayout` 省略 size，父级可用高 31 | `size=16,0`、`measured=16,0`；子元素 `1,0` / `15,0` | `size=16,-1`、`measured=16,31`；子元素 `1,31` / `15,31` |
| 子项 `0` 与 `-1` 混合 | `VLayout=(0,2)`、`HLayout=(2,0)` | 不变 |
| 空布局 | `VLayout/HLayout=(0,0)` | 不变 |
| `GridLayout` 子项全 `(-1,-1)` | 横向 `(2,0)`、纵向 `(0,2)` | 横向 `(2,2)`、纵向 `(2,2)`（对应轴由 0 变为组数） |
| `VLayout` 嵌在 `HLayout` 内（父宽 100，另有固定宽 10 的兄弟） | `getSize=(0,2)`、`measured=(0,2)` | `getSize=(-1,2)`、`measured=(90,2)`（按弹性/权重分配） |
| `TestLayoutReport.xml` 导出 `xl/worksheets/sheet1.xml` | 3519 字节 | 逐字节相同 |

- **回归**：现有测试套件 60 例、1 error（既有的 `SimpleTemplateTest.testLayoutReport` 找不到 `TestLayoutReport.xlsx.xml`），与改动前失败集合一致
- **审查结果**：通过（基线取自 `git HEAD` 原版两文件单独编译，无循环论证；七项数值与表格一致；探针均在 `target/` 下未入库）
- **完成时间**：2026-10-26 09:50

### 3. 真实模板人工验证（用户执行）
- **状态**：⏳ 待用户执行（AI 已提供步骤与判据）
- **步骤**：
  1. 备份 `D:\job\b_project\b_backend\portal\src\main\resources\excel\WarningDayReport.xml`，在副本上删除三个图片循环包裹层 `<VLayout size="-1,-1" for="img: ...">` 的 `size="-1,-1"`（约 355/360/365 行），子元素保持 `-1`。
  2. 用该工程现有预览/导出入口 + `.idea/excel-report-preview/.../WarningDayReport.json` 导出。
  3. 判据：无 `Cannot add merged region ... overlaps`；三个包裹层宽度 = 31（A..AE）；行区间连续且无重叠（修复后的参考值 105–119 / 121–135 / 137–151）；图片非 0 宽。
  4. 回归：保留原 `size="-1,-1"` 的模板导出结果与本次改动前一致。
- **说明**：外部工程数据不在本工作区读取范围内；探针 case1 已覆盖同构结构（省略 size 的内层 VLayout + 全 `-1` 子项）。
- **完成时间**：待用户回执

### 4. 文档同步
- **状态**：✅ 已完成
- **修改文件**：
  - `doc/code-review.md` — 003 行标记已修；追加「003 修复记录」（根因、改动、判定边界、行为变化、实测、已知边界）；001 建议更新为"首选只删 size"；003 行号更新为修复后
  - `doc/flex-layout-allocation.md` — §4.5 中 003 标记已修；§4.4-3 的 `getSize()` 表述更新为"主轴累加、交叉轴可能为 -1"
- **审查结果**：通过（三轮；修正了 001 旧建议、§4.4 表述、失效行号、交替嵌套边界、提交号口径）
- **完成时间**：2026-10-26 09:57
