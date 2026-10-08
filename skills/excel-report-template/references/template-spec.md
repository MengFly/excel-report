# 模板规范（标签 / 属性 / 样式 key 白名单）

来源：框架自带的模板规范 `excel-report-1.0.0.xsd`（即模板头 `xsi:schemaLocation` 指向的那份协议）。
**框架不做严格校验**，所以「能跑」≠「写法正确」；不在下表中的名字不会生效。新增标签或样式 key 属于改协议，不要自行发明。

## 1. 根节点

```xml
<template xmlns="http://mengfly.github.io/excel-report/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://mengfly.github.io/excel-report/1.0.0 https://mengfly.github.io/xsd/excel-report-1.0.0.xsd"
          name="..." version="1.0" author="..." createAt="2026-09-29" description="...">
```

| 属性 | 必填 | 说明 |
|---|---|---|
| `name` | ✅ | 模板名（与最终 Sheet 名无关） |
| `version` | | 默认 `1.0.0` |
| `description` / `author` / `createAt` | | `createAt` 是日期，`2026-09-29` 或 `2026-09-29 18:31:25` 都可 |

**子元素（顺序任意，各 0..1）**：`parameters`、`sheetStyle`、`styles`、`container`（`container` 必填）。

### `<container>` —— 关键约束

- **只能写 1 个元素子节点**。渲染时只读第一个，多写的兄弟节点**静默忽略** ⇒ 需要多个组件就用 `<VLayout>` 包一层。
- `<container>` 里没有任何元素子节点会直接报错。

## 2. `<parameters>` / `<parameter>`

纯文档用途（不影响渲染），用来向使用者说明模板需要哪些参数。

| 属性 | 必填 | 说明 |
|---|---|---|
| `id` | ✅ | 合法标识符（字母/下划线开头） |
| `name` | ✅ | 中文名 |
| `type` | | 建议写 Java 类型，泛型要转义：`List&lt;DataRow&gt;` |
| `description` | | |
| `required` | | 默认 true |

## 3. `<sheetStyle>` —— 27 个 key（子元素名即 key，大小写敏感）

| key | 类型 | 说明 / 取值 |
|---|---|---|
| `displayGuts` | bool | |
| `displayZeros` | bool | |
| `rowSumsBelow` / `rowSumsRight` | bool | |
| `displayGridlines` / `printGridlines` | bool | |
| `printRowAndColumnHeadings` / `displayRowColHeadings` | bool | |
| `autobreaks` | bool | 自动分页 |
| `forceFormulaRecalculation` | bool | 按 `true/false` 写 |
| `defaultColumnWidth` | int | |
| `defaultRowHeight` | **float** | **最常用**。对「没有单独设 `height` 的行」逐行生效；已设 `height` 的行不受影响 |
| `displayFormulas` | bool | |
| `fitToPage` / `horizontallyCenter` / `verticallyCenter` | bool | 打印相关 |
| `zoom` | int | 默认 100 |
| `tabColor` | 颜色名 | 用单元格颜色名或 `#RRGGBB` |
| `committed` | bool | |
| `margin` | double | 全边距。⚠️ 与 `marginLeft/Right/Top/Bottom` **同时写时结果不可预期**，二者选一种写 |
| `marginLeft` / `marginRight` / `marginTop` / `marginBottom` / `marginHeader` / `marginFooter` | double | |
| `password` | string | Sheet 保护密码 |

```xml
<sheetStyle>
    <defaultRowHeight>24</defaultRowHeight>
    <horizontallyCenter>true</horizontallyCenter>
    <fitToPage>true</fitToPage>
    <zoom>90</zoom>
</sheetStyle>
```

## 4. `<styles>` —— 32 个样式 key

`<style id="xxx">` 的子元素名即 key。**引用方式**（`style` / `dataStyle` / `header@style` 属性）：

```
style="idA idB"                        <!-- 多个样式 id，空格分隔，后写的覆盖前面的 -->
style="{fontBold:true,width:auto}"     <!-- 内联 JSON -->
style="idA {dataFormat:0.00 分}"        <!-- 混合，顺序即优先级 -->
```

解析规则（都是可观察行为）：

- 按**花括号外的空格**切分；`{}` 内部的空格保留（所以 `{dataFormat:0.00 分}` 是合法的）。
- token 以 `{` 开头 → 按 JSON 解析，value 会先当表达式求值一遍。
  ⇒ **value 含 `${}` 时必须加引号**：`{fontBold:'${index==0?true:false}'}`，否则 JSON 结构被破坏、整段样式被丢弃。
- token 不以 `{` 开头 → 按 id 查找样式；**从当前节点向上逐层找**，因此只要在最外层定义的样式在任意层级都能引用。
- id 找不到 / key 拼错 / JSON 解析失败 → **静默丢弃**，不报错。

### 4.1 样式 key 清单

| key | 类型 | 取值 |
|---|---|---|
| `borderTop` `borderBottom` `borderLeft` `borderRight` | 枚举 | `none` `thin` `medium` `dashed` `dotted` `thick` `double` `hair` `medium_dashed` `dash_dot` `medium_dash_dot` `dash_dot_dot` `medium_dash_dot_dot` `slanted_dash_dot` |
| `topBorderColor` `bottomBorderColor` `leftBorderColor` `rightBorderColor` | 颜色 | |
| `hidden` `locked` | bool | |
| `alignHorizontal` | 枚举 | `left` `center` `right` `fill` `justify` `center_selection` `distributed` |
| `alignVertical` | 枚举 | `top` `center` `bottom` `justify` `distributed` |
| `wrapText` | bool | 自动换行（长文本 + 固定列宽时必备） |
| `rotation` | 0–180 | 文字旋转 |
| `indention` | 整数 | 缩进空格数 |
| `fillPattern` | 枚举 | `no_fill` `solid_foreground`（默认，此时显示 `fillForegroundColor`）`fine_dots` `alt_bars` `sparse_dots` `thick_horz_bands` `thick_vert_bands` `thick_backward_diag` `thick_forward_diag` `big_spots` `bricks` `thin_horz_bands` `thin_vert_bands` `thin_backward_diag` `thin_forward_diag` `squares` `diamonds` `less_dots` `least_dots` |
| `fillForegroundColor` | 颜色 | **底色就用它**（默认白） |
| `fillBackgroundColor` | 颜色 | 仅图案填充可见；`solid` 时被 `fillForegroundColor` 盖住 |
| `readingOrder` | 枚举 | `context` `left_to_right` `right_to_left` |
| `shrinkToFit` | bool | |
| `width` | 字符串 | 列宽：数字（字符宽）或 **`auto`**（按内容自适应列宽）。⚠️ 见「坑」 |
| `height` | 数字 | 行高（磅） |
| `dataFormat` | 字符串 | Excel 数字格式：`0.0000` `#,##0.00` `0.00 分` `0%` `yyyy-mm-dd` |
| `weight` | 数字 | 弹性占比，默认 1。**仅当该方向 `size=-1` 时生效** |
| `fontName` | 字符串 | 默认 `Arial`；中文报表建议 `微软雅黑` / `宋体` |
| `fontHeight` | 数字 | 默认 12 |
| `fontBold` `fontItalic` `fontStrikeout` | bool | |
| `fontColor` | 颜色 | |
| `fontUnderline` | 枚举 | `none` `single` `double` `single_accounting` `double_accounting` |
| `fontFamily` | 枚举 | `not_applicable` `roman` `swiss` `modern` `script` `decorative` |

### 4.2 颜色写法（两套名字，不通用）

- 首选 **`#RRGGBB`**（唯一两套都认的写法）。
- **单元格 / 表头 / `tabColor`** 用这套名字（大小写不敏感）：
  `black` `brown` `olive_green` `dark_green` `dark_teal` `dark_blue` `indigo` `grey_80_percent` `orange` `dark_yellow`
  `green` `teal` `blue` `blue_grey` `grey_50_percent` `red` `light_orange` `lime` `sea_green` `aqua`
  `light_blue` `violet` `grey_40_percent` `pink` `gold` `yellow` `bright_green` `turquoise` `dark_red` `sky_blue`
  `plum` `grey_25_percent` `rose` `light_yellow` `light_green` `light_turquoise` `pale_blue` `lavender` `white`
  `cornflower_blue` `lemon_chiffon` `maroon` `orchid` `coral` `royal_blue` `light_cornflower_blue` `tan` `automatic`
- **图表系列 `data@color`** 是另一套（CSS 风格名字，共 138 个），常用：
  `alice_blue` `aqua` `aquamarine` `beige` `black` `blue` `blue_violet` `cadet_blue` `chartreuse` `chocolate`
  `coral` `cornflower_blue` `crimson` `cyan` `dark_blue` `dark_cyan` `dark_goldenrod` `dark_gray` `dark_green`
  `dark_khaki` `dark_magenta` `dark_orange` `dark_orchid` `dark_red` `dark_salmon` `dark_sea_green` `dark_slate_blue`
  `dark_slate_gray` `dark_turquoise` `dark_violet` `deep_pink` `deep_sky_blue` `dodger_blue` `firebrick` `forest_green`
  `fuchsia` `gainsboro` `gold` `goldenrod` `gray` `green` `green_yellow` `hot_pink` `indian_red` `indigo` `ivory`
  `khaki` `lavender` `lime` `lime_green` `magenta` `maroon` `medium_blue` `medium_orchid` `medium_purple`
  `medium_sea_green` `medium_slate_blue` `medium_spring_green` `midnight_blue` `navy` `olive` `olive_drab` `orange`
  `orange_red` `orchid` `pale_green` `pale_turquoise` `pale_violet_red` `peach_puff` `peru` `pink` `plum`
  `powder_blue` `purple` `red` `rosy_brown` `royal_blue` `saddle_brown` `salmon` `sandy_brown` `sea_green` `sienna`
  `silver` `sky_blue` `slate_blue` `slate_gray` `snow` `spring_green` `steel_blue` `tan` `teal` `thistle` `tomato`
  `turquoise` `violet` `wheat` `white` `yellow` `yellow_green`
  （完整 138 个见规范里图表颜色的枚举；记不住就直接用 `#RRGGBB`）
- 名字不在对应名单里 → **静默回退**（单元格黑、图表默认绿），不报错。把单元格颜色名用在图表上（或反之）是最常见的翻车点。

## 5. `size` 属性（占格与弹性）

`size="宽,高"`，两项都必须是整数（`-1` 合法）。格式不对会**退回该组件的默认尺寸**，不报错。

| 值 | 含义 |
|---|---|
| 正整数 | 固定占 N 个 Excel 列 / 行 |
| `-1` | **弹性**：在 `HLayout` 里弹性分配宽、在 `VLayout` 里弹性分配高，按剩余空间 × `weight` 分配（至少 1 格） |
| `-1,-1` | 两个方向都撑满父容器 |

⚠️ **`Table` / `List` 不吃正数 `size`**（它们按数据自算格数）⇒ 要它们撑满就写 `-1`。
⚠️ 未设 `size` 时用组件默认尺寸，见 `references/components.md` 默认尺寸表。

## 6. 表达式

- 语法：`${ ... }`，内部是标准 SpEL。一个字符串里可以有多个片段（`${a} 和 ${b}`）。
- 拼接行为：**只有一段表达式** → 返回原始对象（数字仍是数字）；**多段** → 拼成 `String`。
- 变量 = 调用方传进来的参数 Map（`DataContext`）。`for` / `include` 会往这个 Map 里注入子级变量，子级优先。
- Map 的 key 既可用点号也可用下标：`${map.key}`、`${map['key']}`（key 含特殊字符用后者）。
- 参数值是 `Optional` 时会自动解包（便于懒加载）。
- 可调静态方法：`${T(cn.hutool.core.util.RandomUtil).randomInt(1,100)}`。
- 三元 / 判空：`${flag ? '是' : '否'}`、`${name == null ? '-' : name}`。
- **表达式解析失败时按字面量处理**（不抛异常）⇒ 单元格里出现 `${typo}` 就是表达式写错了。

## 7. `if` / `for`

- `if="${cond}"` —— 计算结果转 boolean，false 则该组件**整个不渲染**。条件取不到值时会按真处理，慎用易失败的表达式。
- `for="item,index: ${list}"` / `for="item: ${list}"` / `for="k,v: ${map}"` —— 循环渲染该组件（每次一个子级变量环境）。
  - **冒号必须存在**，否则退化成「只渲染一次」（不报错）。
  - 列表：第 1 个变量是元素、第 2 个是下标；Map：第 1 个是 key、第 2 个是 value。
  - **只有布局类标签支持 `for`**：`VLayout` `HLayout` `GridLayout`，以及图表里的 `valueAxis1` / `valueAxis2`。
    ⇒ 要让一个 `Text` 或 `Table` 循环，外面套一层带 `for` 的 `VLayout`。

## 8. 富文本

`text` 值以 `<html>` 开头且以 `</html>` 结尾 → 走富文本；内部元素的**属性名就是样式 key**（只管字体类样式）：

```xml
<Text size="-1,1"
      text="&lt;html&gt;合计：&lt;font fontColor='#C00000' fontBold='true' fontHeight='16'&gt;123.45&lt;/font&gt; 元&lt;/html&gt;"/>
```

## 9. `<include>` —— 模板复用

```xml
<include ref="CommonHeader"/>
<include ref="DetailBlock" title="${title}" dataList="${rows}"/>
```

- `ref` = 被引用的模板 id（可省 `.xml`）。
- **除 `ref` 之外的所有属性都会作为子模板的参数**，值先当表达式求值。
- ⚠️ 子模板拿到的是**全新的参数集合**（只含这里显式传入的属性），**不继承父模板的变量**
  ⇒ 子模板需要的参数必须全部显式传进来。
- 被 include 的模板本身也必须是一个完整模板（有自己的 `<container>`），渲染结果就是它的根组件。
- 不要互相/自我递归引用。

## 10. 组件默认尺寸 & 默认样式

| 标签 | 默认 `size` |
|---|---|
| `Text` / `Link` / `Span` | `1,1` |
| `Image` / `Chart` | `4,10` |
| `Table` / `List` | 按数据自算（正数 `size` 无效） |
| `VLayout` / `HLayout` / `GridLayout` | 由子组件合成 |

**所有单元格的默认样式（基线，会被上层样式逐层覆盖）**：
`borderTop/Bottom/Left/Right = thin`、边框色 `#000000`、`alignHorizontal=left`、`alignVertical=center`、
底色 `#FFFFFF`、字体 `Arial` / 12 / 黑色 / 非粗体 / 无下划线、`wrapText=false`、`rotation=0`、`indention=0`。

**Sheet 默认**：默认行高 20 磅。

⇒ 想要「无边框」必须显式写 `borderXxx=none`；想要中文好看必须显式写 `fontName`。
