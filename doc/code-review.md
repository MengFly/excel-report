# Code Review 清单

> 最后更新：2026-10-13

## Bug 列表

| ID | 严重等级 | 文件 | 行号 | 描述 | 风险 | 建议方案 |
|----|---------|------|------|------|------|---------|
| 001 | 高 | `D:\job\b_project\b_backend\portal\src\main\resources\excel\WarningDayReport.xml` | 355, 360, 365 | 三个图片循环包裹层写 `<VLayout size="-1,-1" for="img: ...">`，高度 `-1` 是"弹性/按权重分剩余高度"而非内容自适应；父级按等权重均分得到 37/37/38 行，而三组内容实际为 32/48/32 行，3 条那组溢出 11 行压到下一个兄弟节点 | 导出直接失败：`IllegalStateException: Cannot add merged region A114:AE114 ... overlaps ... (A110:AE124)`；只要三组列表条数不等就必崩（2/3/2 崩、2/2/2 正常、4/3/3 崩） | **首选**：只去掉包裹层的 `size="-1,-1"`，子元素宽度保持 `-1` 即可继承父级可用宽度（003 已于 2026-10-26 修复，原先"不可只删 `size`"的限制不再成立）。**旧版本框架**退化为：删 `size` 后把循环体内 `Text`/`Image` 宽度写成 `31`（整表 A..AE） |
| 002 | 高 | `src/main/java/io/github/mengfly/excel/report/layout/VLayout.java` | 65-76（配合 `util/WeightSizeHelper.java:65-83`） | 弹性子节点（`preferredSize.height < 0`）按 `weight`（默认 1）均分剩余高度；当分配值小于该节点的内容高度时，VLayout 既不裁剪也不告警，子元素继续顺序排布、溢出到下一个兄弟节点的行区间 | 两张合并区相交，POI 在 `addMergedRegion` 抛 `IllegalStateException`；报错发生在**导出阶段**且信息里只有单元格坐标、与模板位置无关，定位成本高（本次靠探针才定位到模板行） | ✅ **已修（2026-10-23）**：`WeightSizeHelper` 改为"内容尺寸保底 + 剩余按 weight 分配 / 放不下按内容比例压缩"，`VLayout`/`HLayout` 分配后对"分配 < 内容"的子节点打 WARN。详见下方「002 修复记录」与 `doc/flex-layout-allocation.md` |
| 003 | 中 | `src/main/java/io/github/mengfly/excel/report/layout/VLayout.java` | 58-82（`sumContainerSize`）、97-110（`getChildContainerSuggestSize`），2026-10-26 修复后行号 | 省略 `size` 时 `preferredSize` 取 `container.getSize()`，而 `getSize().width = max(0, 子宽度)`，宽度为 `-1` 的子元素按 0 计入 ⇒ 包裹层自然宽度塌成 0，其子元素被测量成 0 宽 | 用户想表达"宽度自适应 + 高度随内容"时（`Size.of` 必须给两个值，写不出来）只能删 `size`，结果图片变成 0 宽；属静默错误，无任何提示 | ✅ **已修（2026-10-26）**：`VLayout.sumContainerSize` 宽度、`HLayout.sumContainerSize` 高度改为"子项非空且该轴全 `< 0` 时返回 `-1`，否则原 max"，省略 `size` 的布局因此继承父级可用尺寸。详见下方「003 修复记录」与 `doc/layout-cross-axis-auto-size.md` |

## 详情

### 001 模板侧：弹性高度用在图片循环包裹层上

- 复现（框架真实导出链，探针 `target/probe/ExportProbe.java`）：
  - 原模板 + `WarningDayReport.json`（图片列表 2/3/2）→ 精确复现报错 `A114:AE114` vs `A110:AE124`
  - 原模板 + 三组均 2 条 → 导出成功（等分 32/32/32 恰好等于内容高度）
  - 原模板 + 4/3/3 → 崩（`A93:AE93` vs `A89:AE103`）
- 坐标对应：`A110:AE124` = 0-based 109–123，即"煤体应力图表"第 3 张图（溢出后）；`A114:AE114` = 0-based 113，即紧随其后的"支架压力图表"第 1 个标题。
- 每张图固定 15 行、标题 1 行 ⇒ 每组内容高度 = 条数 × 16。
- 修复后验证：2/3/2 与 4/3/3 均导出成功，合并区 242 个，行区间连续无重叠（105–119 / 121–135 / 137–151）。
- 备选方案（不推荐）：高度写死 `size="-1,32"/"-1,48"/"-1,32"` —— 条数一变即复发。

### 002 框架侧：弹性子节点超额分配后静默溢出

- 实测分配过程：整表 159 行，其余固定高度组件合计 47 行，剩余 112 行 ÷ 3 个等权重弹性子节点 = 37/37/38。
- 溢出路径：`VLayout.measure`（`suggestSize.height` 用分配值）→ 子元素均为固定高度 ⇒ `WeightSizeHelper` 对它们按声明值返回、无人吸收负的 `waitForDistributionSize` ⇒ `onLayout` 里 `start += childSize.height` 继续累加，越过容器自身范围。
- 同源风险：`HLayout` 的宽度分配走同一套 `WeightSizeHelper`，横向也会出现同类溢出（本次未实测）。

### 003 框架侧：省略 `size` 时宽度塌陷

- 实测：图片循环包裹层只删 `size="-1,-1"`（子元素仍为 `-1`）时，包裹层 `measured = (0, 32)`，其子元素 `measured = (0,1)`、`(0,15)` —— 宽度全为 0。
- 相关：`ContainerParser.doParse` 在 `size` 属性为空时用 `container.getSize()` 当 `preferredSize`，`SizeStyleKey` 把它写进样式；随后 `getChildContainerSuggestSize` 因 `preferredSize.width = 0 >= 0` 判定为"有确定宽度"而不回退到父级可用宽度。

### 002 修复记录（2026-10-23）

**根因**：框架在分配那一刻已经知道内容高度，只是没用。`VLayout.getSize()` 对固定高度子节点累加声明高度，
所以"内容由固定高度子元素组成"的包裹层，`getSize().height` 就是内容高度（本例 32/48/32）；
而 `WeightSizeHelper` 只用 `getSize()` 判"是否弹性"，分配公式里只有 `weight` ⇒ 112 ÷ 3 = 37/37/38。

**改动**（3 个文件，无 API 签名变更，模板协议不变）：

| 文件 | 改动 |
|---|---|
| `util/WeightSizeHelper` | 分配改为构造期一次算完 + 查表（`distributionSize` 变幂等）。弹性子节点以 `max(1, getSize()[轴])` 为 flex-basis：空间够 ⇒ 保底 + 剩余按 weight；不够 ⇒ 按内容比例压缩，最小 1 |
| `layout/AbstractLayout` | 新增 `reportOverflow(sizeType)`：分配后对"**弹性**且分到的尺寸 < 内容尺寸"的子节点打 WARN，带模板标签链、`size` 属性、父容器内序号、分配/内容/缺口/可用/内容合计；`GridLayout` 临时 new 的合成布局无 `templateNode` 直接跳过，同一子节点只报一次 |
| `layout/VLayout`、`layout/HLayout` | `onMeasure` 末尾各调一次 `reportOverflow`（行 / 列） |

**等价性**：内容尺寸打平时，新分配 `b + (avail − n·b)/n` 恒等于旧分配 `avail/n`（整数顺序扣减的取整行为也保持一致），
所以等量场景输出不变；差异只落在旧算法必然分错的输入上。

**实测（`target/probe`，JDK8 + 真实 POI 链）**：

| 用例 | 改动前 | 改动后 |
|---|---|---|
| `orig.xml` + `data.json`（2/3/2） | 分配 37/37/38，导出抛 `IllegalStateException` | 分配 **32/48/32**，段间无留白无重叠，`merge-able rects = 242`（与 001 手工修模板的参考值一致） |
| `orig.xml` + `data-eq2.json`（2/2/2） | 32/32/32 | 32/32/32（不变），无 WARN |
| `orig.xml` + 可用高度只有 100 行（模拟真的放不下） | 无任何提示 | 3 条 WARN，形如：`布局内容溢出：VLayout > VLayout[size=-1,-1]（父容器第 13 个子节点）分到 22 行，内容需要 48 行（缺 26 行）；父容器可用 100 行，子节点内容合计 159 行` |
| 现有测试套件 60 个用例 | — | 59 通过；唯一失败 `SimpleTemplateTest.testLayoutReport` 是**既有**问题（用例找 `TestLayoutReport.xlsx.xml`，资源目录只有 `TestLayoutReport.xml`），与本次改动无关 |

**已知边界**（详见 `doc/flex-layout-allocation.md` §4.4）：

- 弹性叶子（`Text`/`Image` 自身 `height=-1`）没有内容尺寸，basis 仍按 1 —— 内容不可知，无法保底。
- 告警**只看弹性子节点**：非弹性子节点的分配值就是它自身声明尺寸（布局类的 `getSize()` 已覆盖成内容累加），不会溢出；
  若按"内容至少 1"去比，`size="31,0"` 的占位节点与 `<PageRowSplit/>`（`getSize()=(0,0)`）会天天误报。
- 固定子节点本身就超过可用尺寸时不告警（弹性叶子分到 1 行已等于其 basis）——属另一类问题，未纳入本次。
- `size` 声明的高度**对 layout 类子节点不生效**（`VLayout`/`HLayout` 的 `getSize()` 覆盖了 `preferredSize`，父级拿到的是内容累加值），
  所以"把高度写死写小"这条溢出路径只对叶子组件成立。这也是 001 的"备选方案（写死高度）"不稳定的原因之一。
- 横向同源风险已一并处理（`HLayout` 走同一算法）。

### 003 修复记录（2026-10-26）

**根因**：布局类 `getSize()` 的交叉轴是 `max(0, 子项该轴...)`，子项该轴全为 `-1`（弹性）时得到 0；
`ContainerParser` 在省略 `size` 时把这个 0 原样写入 `preferredSize`，父级 `getChildContainerSuggestSize`
看到 `0 >= 0` 便按"确定 0"处理、不再回退父级可用尺寸 ⇒ 子树被测量成 0 宽/0 高，且无异常无告警。

**改动**（2 个文件，无签名变更，模板协议不变）：

| 文件 | 改动 |
|---|---|
| `layout/VLayout` | `sumContainerSize` 宽度：子项非空且全部 `< 0` 时返回 -1，否则保持原 max；子项 `getSize()` 只取一次 |
| `layout/HLayout` | `sumContainerSize` 高度：与 VLayout 对称 |

**判定边界**：

- 仅严格全 `< 0` 才返回 -1；含 0（`size="0,0"` 占位、`PageRowSplit`）保持 0，不被相邻弹性项带成 -1。
- 空布局返回 0（"全为负数"不适用于空集合）。
- 含正数时保持原 max；主轴（`VLayout` 高度求和 / `HLayout` 宽度求和）不变。

**行为变化**：

- 仅"交叉轴子项全为负"的布局由 0 变 -1，从而走父级"继承可用尺寸"的弹性分支；其余输入结果不变。
- `GridLayout` / `TableComponent` 间接变化：`getSize()` 委托合成布局，全弹性交叉轴时对应轴由 0 变为组数量
  （横向 `(2,0)→(2,2)`、纵向 `(0,2)→(2,2)`），**不是直接继承父级**（合成根布局该轴走求和/取正，不会产出 -1）。
- **交叉轴 -1 落在父级主轴时按弹性分配**：交替嵌套（`VLayout` 在 `HLayout` 内、`HLayout` 在 `VLayout` 内）时，
  该 -1 是父级的主轴取值，`WeightSizeHelper.isFlexSize` 会把它判为弹性 ⇒ 按 `basis=1` + `weight` 分配，
  声明/内容尺寸不再直接生效（改动前是固定 0；实测父宽 100、固定兄弟 10 时，inner 由 `measured=(0,2)` 变 `(90,2)`）。
  与"继承父级"不同，需要在模板侧知道这是权重语义。

**实测（`target/probe`，JDK8）**：

| 用例 | 改动前 | 改动后 |
|---|---|---|
| 内层 `VLayout` 省略 size，父宽 31 | `getSize=(0,16)`、`measured=(0,16)`；子 `0,1` / `0,15` | `getSize=(-1,16)`、`measured=(31,16)`；子 `31,1` / `31,15` |
| 内层 `HLayout` 省略 size，父高 31 | `getSize=(16,0)`、`measured=(16,0)`；子 `1,0` / `15,0` | `getSize=(16,-1)`、`measured=(16,31)`；子 `1,31` / `15,31` |
| 子项 0 与 -1 混合 | `(0,2)` / `(2,0)` | 不变 |
| 空布局 | `(0,0)` | 不变 |
| `GridLayout` 子项全 `(-1,-1)` | 横向 `(2,0)`、纵向 `(0,2)` | `(2,2)` / `(2,2)` |
| `VLayout` 嵌在 `HLayout` 内（父宽 100，另有固定宽 10 的兄弟） | `getSize=(0,2)`、`measured=(0,2)` | `getSize=(-1,2)`、`measured=(90,2)` |
| `TestLayoutReport.xml` 导出 `sheet1.xml` | 3519 字节 | 逐字节相同 |
| 现有测试套件 60 例 | — | 1 error，仍为既有的 `SimpleTemplateTest.testLayoutReport`（缺 `TestLayoutReport.xlsx.xml`） |

**已知边界**：

- 只改 `VLayout`/`HLayout` 交叉轴；`GridLayout`/`Table` 的合成布局经主轴求和后得到正数，不继承父级。
  若确需"网格宽度自适应"，需另立议题（改 `GridLayout` 的合成/取尺寸逻辑）。
- 交替嵌套时该 -1 落在父级主轴，会按弹性/权重分配（见上一条行为变化），不保证"等于内容尺寸"。
- 省略 `size` 的布局在交叉轴现在等价于写了 `-1`；外部若直接把 `getSize()` 当正数做算术，需要容忍负数
  （框架内 `WeightSizeHelper.contentSizeOf` 已按最小 1 处理，`getChildContainerSuggestSize` 已判 `< 0`）。
- 真实模板（`WarningDayReport.xml` 删除包裹层 `size`）的端到端导出版式由用户验证，见
  `doc/layout-cross-axis-auto-size.md` 任务 3。

