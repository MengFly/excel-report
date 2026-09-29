# 组件清单与用法（高级优先）

标签一共 14 个：`VLayout` `HLayout` `GridLayout` `Text` `Table` `Image` `List` `Span` `Link` `Chart` `include`
`PageColSplit` `PageRowSplit`，外加首页用到的 `column` / `header` / 图表子节点等。除下表特别说明外都用不到别的标签。

通用属性（除特别说明外所有组件都有）：

| 属性 | 说明 |
|---|---|
| `id` | 合法标识符，可选，仅作唯一标识 |
| `style` | 样式引用（id 列表 / 内联 JSON / 混合） |
| `if` | 表达式条件，false 则不渲染 |
| `size` | `宽,高`，见 `template-spec.md §5` |
| `for` | 仅布局类（`VLayout`/`HLayout`/`GridLayout`）与图表的 `valueAxis1`/`valueAxis2` 支持 |

## 1. 布局容器

### VLayout（垂直布局，最常用的根组件）

```xml
<VLayout alignPolicy="start" size="-1,-1">
    <Text size="-1,1" text="A"/>
    <Text size="-1,1" text="B"/>
</VLayout>
```

- 属性：`alignPolicy`（`start` 默认 / `center` / `end`，控制子组件在**水平方向**的落位）、通用属性、`for`。
- 子元素：任意组件 + `include` + `PageRowSplit` + `PageColSplit`。
- 语义：竖向堆叠；子组件 `size` 高度为 `-1` 时按 `weight` 分配剩余高度。

### HLayout（水平布局）

同上，`alignPolicy` 控制子组件**垂直方向**的落位；子组件 `size` 宽度 `-1` 时弹性分配。
自身宽度 = 各子组件宽度之和（`-1` 的子组件按「至少占 1」计入）。

### GridLayout（网格）

```xml
<GridLayout count="3" orientation="horizontal" alignPolicy="center">
    <Text text="A"/> <Text text="B"/> <Text text="C"/>
    <Text text="D"/>
</GridLayout>
```

- `count`（≥1）：`orientation=horizontal` 时是**列数**，`vertical` 时是**行数**。
- `orientation` 默认 `horizontal`；`alignPolicy` 默认 `center`（其他布局默认 `start`）。
- 适合「等宽卡片 / 指标格 / 图例矩阵」。**表格式数据仍应优先用 `Table`**。

## 2. Table（二维数据，首选）

```xml
<Table dataList="${dataList}" headerVisible="true" headerHeight="1">
    <column id="name"   name="名称" style="tableHeaderStyle" dataStyle="tableDataStyle"/>
    <column id="amount" name="金额" style="tableHeaderStyle" dataStyle="tableDataStyle num4"
            columnSpan="2"/>
</Table>
```

| 属性 | 必填 | 说明 |
|---|---|---|
| `dataList` | ✅ | 表达式，结果须是列表 / 数组 / Iterable |
| `headerVisible` | | 默认 true |
| `headerHeight` | | 表头占几行，默认 1 |

`<column>`（至少 1 个）：

| 属性 | 必填 | 说明 |
|---|---|---|
| `id` | ✅ | 取值路径，对应数据对象的**字段名**（Java 对象按字段取值，Map 按 key 取值）。**支持 `a.b.c` 逐级下钻** |
| `name` | ✅ | 表头文字 |
| `style` | | **表头**样式 |
| `dataStyle` | | **数据单元格**样式 |
| `columnSpan` | | 该列占几个 Excel 列，默认 1（该列数据会横向合并） |

行为细节：

- 表头样式 = 内置默认（粗体、13 号、居中）**再叠加** `column@style`；数据样式只由 `column@dataStyle` 决定。
- `<Table>` 自身的 `style` 会同时作用于表头和所有数据单元格。
- `headerVisible="false"` 时**不画表头**，此时 `<Table>` 的 `style` 也不影响表头 —— 用于外部手拼多级表头的场景。
- `dataList` 为空时只画表头。
- `size`：正数无效；要撑满写 `size="-1,-1"`。

## 3. List（一维数据）

```xml
<List dataList="${names}" orientation="vertical" span="2" style="{alignHorizontal:center}">
    <header title="姓名" span="1" style="{fontBold:true}"/>
</List>
```

| 属性 | 说明 |
|---|---|
| `dataList` | ✅ 表达式 |
| `orientation` | `vertical`（默认，纵向一项一行）/ `horizontal`（横向一项一格） |
| `span` | 默认 1。vertical ⇒ 每项占几列；horizontal ⇒ 每项占几行 |
| `size` | 正数无效，`-1` 才生效 |

`<header>`（可选，0..1）：`title`（✅）、`span`（默认 1）、`style`。
- `vertical`：表头占 `span × header.span`，数据依次向下；`horizontal`：表头占 `header.span × span`，数据依次向右。
- 数据单元格**没有逐项样式**，样式只能来自 `<List>` 自身或外层容器。

适合「指标卡 / 标签云 / 一行多值的键值区」，不适合二维表（用 `Table`）。

## 4. Chart（图表）

```xml
<Chart size="10,15" displayBlankAs="gap">
    <Title text="标题" fontSize="16" bold="true" italic="false" underLine="NONE" overlay="false"/>
    <Legend position="top" overlay="false"/>
    <Marker showVal="true" showCatName="false" showPercent="false"/>
    <ChartData> ... </ChartData>
</Chart>
```

| 属性 | 说明 |
|---|---|
| `size` | 默认 `4,10` |
| `autoTitleDelete` | bool |
| `displayBlankAs` | `gap` / `span` / `zero`（空值怎么画） |
| `anchorType` | `MOVE_AND_RESIZE` / `DONT_MOVE_DO_RESIZE` / `MOVE_DONT_RESIZE` / `DONT_MOVE_AND_RESIZE`（单元格尺寸变化时图表怎么动） |

- `<Title>`：`text`（✅）、`fontSize`（默认 12）、`bold`（默认 true）、`italic`、`underLine`（`NONE` `SINGLE` `DOUBLE` `DASH` `DASH_HEAVY` `DASH_LONG` `DASH_LONG_HEAVY` `DOT_DASH` `DOT_DASH_HEAVY` `DOT_DOT_DASH` `DOT_DOT_DASH_HEAVY` `DOTTED` `DOTTED_HEAVY` `HEAVY` `WAVY` `WAVY_DOUBLE` `WAVY_HEAVY` `WORDS`）、`baseLine`。
- `<Legend>`：`position`（`bottom` `left` `right` `top` `top_right`）、`overlay`（是否覆盖在图上）。
- `<Marker>`（数据标签）：`showPercent` `showBubbleSize` `showLeaderLines` `showSerName` `showCatName` `showVal` `showLegendKey`，全 bool，默认 false。
- **数据节点三选一**：`<ChartData>`（二维柱/线/散点/面积）、`<Chart3dData>`（三维柱/线/面积）、`<PieData>`（饼 / 三维饼）。
  **一个 `<Chart>` 只能有一个数据节点**，没有数据节点时图表会消失（不报错！）。饼图和柱图要分成两个 `<Chart>`。

### ChartData / Chart3dData

```xml
<ChartData>
    <labelAxis type="category" showMajorGridLines="true" majorTickMark="out"
               labelAlignment="left" tickLabelPosition="nextTo">
        <data title="X" valueType="value" dataList="${labels}"/>
    </labelAxis>

    <valueAxis1 title="系列1" minimum="0" maximum="100" numberFormat="0.00"
                showMajorGridLines="true" showMinorGridLines="false">
        <!-- type: area | bar | line | scatter（Chart3dData: area3d | bar3d | line3d） -->
        <data title="实际" type="bar" valueType="value" dataList="${series1}" color="#4F81BD"/>
        <data title="预测" type="line" valueType="value" dataList="${series2}" smooth="false"/>
    </valueAxis1>

    <valueAxis2 title="系列2" tickLabelPosition="nextTo">
        <data title="占比" type="line" valueType="value" dataList="${series3}"/>
    </valueAxis2>
</ChartData>
```

- `<labelAxis>`：只放一个 `<data>`。`type` = `category` / `date` / `values`；`labelAlignment` = `center` / `left` / `right`。
- `<valueAxis1>` / `<valueAxis2>`：可放多个 `<data>`，每个 = 一个系列；**双轴**就是分别挂在 axis1 / axis2 上。
  两个轴都支持 `for`，可以在模板里循环生成系列。
- 轴共有属性：`title` `numberFormat` `logBase` `majorUnit` `minorUnit` `minimum` `maximum` `orientation`
  （`minMax` / `maxMin`）`majorTickMark` `minorTickMark`（`none` `cross` `in` `out`）
  `tickLabelPosition`（`nextTo` `none` `high` `low`）。
- 网格线（三个轴标签都支持）：`showMajorGridLines`、`showMinorGridLines`，默认 false。
- `<data>`：`title`（✅ 系列名）、`valueType`（`value` / `relation`）、
  `dataList`（`valueType=value` 时用，表达式取数值/字符串列表）、
  `address`（`valueType=relation` 时用，**引用已渲染出的单元格区域**，格式 `首行,末行,首列,末列`）、
  `color`、`varyColors`（是否每个数据点不同色）、`smooth`（曲线是否平滑，默认 true）、`showLeaderLines`。
  ⚠️ `dataList` 和 `address` 都不给，该系列会被静默丢弃。

### PieData

```xml
<PieData>
    <labelAxis type="category"><data title="X" valueType="value" dataList="${labels}"/></labelAxis>
    <valueAxis title="占比">
        <data title="占比" type="pie" valueType="value" dataList="${values}" varyColors="true"/>
    </valueAxis>
</PieData>
```

`type` = `pie` / `pie3d`。饼图建议 `varyColors="true"` 并配 `<Marker showCatName="true" showPercent="true"/>`；
`varyColors=false` 时所有扇区同色。

## 5. Image

```xml
<Image size="9,15" src="${imageSrc}" scaleType="FIT_XY" anchorType="MOVE_AND_RESIZE" padding="2,2,2,2"/>
```

| 属性 | 说明 |
|---|---|
| `src` | ✅ 表达式，结果可以是：图片文件路径 / `java.io.File` / `InputStream` / 网络 URL（`http(s)://`）/ Base64 字符串 |
| `scaleType` | `FIT_XY`（默认，拉伸填满）/ `FIT_START` / `FIT_END` / `CENTER`（原尺寸居中） |
| `scaleHeight` | 高度缩放比例，默认 1.0 |
| `padding` | 内边距，4 值 `上,右,下,左`，默认 `2,2,2,2`（像素） |
| `anchorType` | 同 Chart |

- `size` 默认 `4,10`，表示**图片占用的单元格区域**，图片按 `scaleType` 适配该区域。
- URL 图片有超时保护（约 5s 连接 / 10s 读取），失败会把错误信息写进单元格。
- 图片最终像素依赖列宽/行高 ⇒ 需要精确排版时先用 `style.width` / `style.height` 固定列宽行高。

## 6. Text / Link

```xml
<Text size="13,1" text="${title}" style="titleStyle"/>
<Text size="-1,2" text="&lt;html&gt;合计 &lt;font fontColor='#C00000' fontBold='true'&gt;100&lt;/font&gt; 元&lt;/html&gt;"/>
<Link link="https://example.com" text="官网" label="点击访问" size="-1,3" style="{fontUnderline:single}"/>
```

- `Text`：`text`（✅ 必填）。值类型会保留 —— 单个表达式返回数字就是数值单元格；多片段或字面量就是字符串。
- `Link`：`text`（显示文字，✅）、`link`（地址，✅）、`label`（鼠标悬停提示）。默认样式 = 蓝色 + 下划线。
- 字面量里的 XML 特殊字符必须转义：`&lt;` `&gt;` `&amp;` `&quot;`。
- `text` 里写 `\n` **不会换行**（Excel 需要真实换行符 + `wrapText`）⇒ 多行文字请拆成多个 `Text`。

## 7. Span / PageRowSplit / PageColSplit

```xml
<Span size="2,1" style="{borderTop:thin}"/>   <!-- 合并区留白：默认四边无边框 -->
<PageRowSplit/>                               <!-- 此处插入打印分页（行） -->
<PageColSplit/>                               <!-- 此处插入打印分页（列） -->
```

- `Span` 只做「合并单元格」，**不写任何值**，默认四边无边框。可用它占位、补细边框、或扩大合并区范围。
- `PageRowSplit` / `PageColSplit` 不占格，只在导出时插入打印分页符。放在有内容的行之后，别放在首行。

## 8. include

见 `references/template-spec.md §9`。要点：子模板**不继承父模板的变量**，需要的参数要显式传。

## 附：布局规则（决定「为什么没占满 / 为什么跑偏」）

1. **两趟走完**：先按各组件 `size` 自内向外算出占格并定位，再往单元格里写内容。
2. **固定与弹性**：`size` 里正整数就是固定占格；`-1`（或数据自算为负）表示弹性。
   在 `VLayout` 里弹性作用于高度、在 `HLayout` 里作用于宽度；
   另一方向（交叉轴方向）取父容器给的建议尺寸。
3. **弹性如何分配**：
   - 先从总额里扣掉固定尺寸子组件；
   - 剩下的按 `weight`（默认 1，≤0 视为 1）比例分配；
   - 每次取整**向下**，余数留给后面的组件；最小 1 格。
   - ⇒ 多个 `-1` 且权重相同时，靠前的组件可能多 1 格，属于正常现象。
4. **`alignPolicy`** 只影响**交叉轴**落位（VLayout 管水平、HLayout 管垂直），不影响主轴堆叠顺序。
5. **`width:auto` / `height` 的生效时机**：
   - 列宽/行高在该单元格**内容、样式、合并都确定之后**才计算，同列取**最大值**。
   - 只合并、不写内容的区域（`Span` / `Chart`）在收尾阶段补算，此时单元格是空的 ⇒ 算出来是无效小值（约 3 个字符宽）。
     **⇒ 不要给 `Span` / `Chart` 写 `width:auto`。**
   - 同一列里混「带 auto」和「不带 auto」的单元格会让结果不可预期；`width:auto` 是**列级**结果。
6. **单位**：`width` 数字 = Excel 字符宽；`height` 数字 = 磅（point）。
