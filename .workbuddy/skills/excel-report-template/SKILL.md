---
name: excel-report-template
description: 为 excel-report 框架（io.github.mengfly:excel-report）编写/修改 Excel 报表模板 XML。当用户要求「写报表模板 / 生成报表模板 / 根据这份数据做个 Excel 报表 / 加个 Sheet / 模板不生效帮我看看」时使用。含模板规范白名单（标签、属性、样式 key、颜色名）、SpEL 表达式、for/if、组件选型（Table/List/Chart/Image/include 优先）、尺寸与布局规则、用户数据接口对接方法、以及可离线运行的自检脚本。
agent_created: true
---

# excel-report 报表模板编写

报表面向框架为 `io.github.mengfly:excel-report`。**一个模板 = 一个 XML 文件 = 一个 Sheet 页**。

模板的标签集、属性集、样式 key 集都由框架自带的规范 `excel-report-1.0.0.xsd` 封闭定义
（模板头 `xsi:schemaLocation` 指向的就是它），不在规范内的写法不会生效 —— 完整白名单见
`references/template-spec.md`，本文只讲「必须记住的规则 + 怎么写」。

## 铁律（违反 = 跑不通或静默失效）

1. **`<container>` 下只能有 1 个根组件**。渲染时只读 `<container>` 的第一个子节点，多写的兄弟节点会被**静默忽略**。
   需要多个组件时，一律用 `<VLayout>` 包一层当根。
2. **标签、属性、样式 key 必须用规范内的名字**。框架**不做严格校验**，写错不报错，只会被丢弃或忽略 —— 交付前必须跑 `scripts/check_template.py`。
3. **样式 key 大小写敏感**；拼错的 key、不存在的样式 id、解析不了的样式写法都会被**静默丢弃**。
4. **颜色名有两套，不通用**：单元格/表头/`tabColor` 一套名字（`grey_50_percent`、`light_green`…），
   图表系列另一套名字（`alice_blue`、`dark_slate_gray`…）。用错名字**静默回退**（单元格变黑、图表变默认绿）。
   ⇒ **优先用 `#RRGGBB`**，它是两套都认的写法。两套名单见 `references/template-spec.md §4.2`。
5. **表达式是 SpEL**：`${...}`。`if` / `for` 也是表达式：`if="${index != 1}"`、`for="item,index: ${dataList}"`。
   表达式写错不报错，会**原样把 `${...}` 当文本输出到单元格**。
6. **数字单元格只能靠单个表达式**：`text="${num}"` → 数值单元格；`text="${a}${b}"`（多片段拼接）→ **字符串**。
7. **不要给 `Table` / `List` 设正数 `size`**：这两者按数据自算格数，正数会被忽略。只有 `-1`（撑满）对它们有效。
   `Text`/`Link`/`Span`/`Image`/`Chart` 的 `size` 才是真正生效的占格数。
8. **模板解析结果会被缓存**：改了模板文件却看不出变化，先清理模板缓存 / 重新部署再验证，不要怀疑写法。

## 工作流

### 步骤 1 · 摸清数据（不可跳过）

目标：产出「模板参数表」，参数 id 与用户数据**实际字段名逐一对齐**。

- 定位数据来源：Service / Mapper / Controller / DTO / VO / entity，或已有报表的参数装配处。
- 产出表格：`参数id | Java 类型 | 结构（字段清单 / List<T> / Map）| 数据来源文件:行`。
- **不修改用户的数据接口与数据结构**。命名/嵌套不匹配时，优先在模板侧用 SpEL 适配：
  - 嵌套属性：`${row.user.name}`，列 `id="user.name"`
  - Map：`${row['key-name']}`
  - 下标：`${list[0]}`；三元：`${flag ? '是' : '否'}`；判空：`${x == null ? '-' : x}`
- 确实需要改数据侧（缺字段、返回结构不适合直接渲染）→ **先问用户**，不要擅自改。

### 步骤 2 · 选组件（高级优先，低级只兜底）

| 需求 | 首选（高级） | 不要用 |
|---|---|---|
| 二维列表数据 | `Table`（`dataList` + `column`，自带表头 / `columnSpan`） | `VLayout`+`Text` 手搓每一行 |
| 一维数据（键值 / 标签集合） | `List`（`orientation` + `<header>`） | 手写多个 `Text` |
| 图表 | `Chart` + `ChartData` / `PieData` / `Chart3dData` | 截图贴 `Image` |
| 图片 | `Image`（文件 / 流 / URL / Base64，`scaleType` 控制适配） | — |
| 超链接 | `Link` | 模板层给 `Text` 加链接（做不到） |
| 多 Sheet / 复用版式 | 多次导出（每次一个 Sheet）；或用 `include` 抽公共模板 | 复制粘贴整份 XML |
| 重复结构 | `for` 属性 | 复制 N 份节点 |
| 条件显示 | `if` 属性 | 在 Java 侧过滤数据 |
| 规则网格 | `GridLayout count= orientation=` | 手搓 HLayout/VLayout 矩阵 |
| 组合排版 / 多级表头 | `HLayout` / `VLayout`（中级，只在需要拼装时用） | — |
| 标题 / 单位 / 说明 / 表头文字 | `Text`（表头也只能用它实现） | — |
| 纯合并留白 / 补边框 | `Span` | — |
| 打印分页 | `PageRowSplit` / `PageColSplit` | — |

> **唯一必须混合低级组件的场景**：框架没有「跨列表头」能力。多级表头 =
> 用 `HLayout` + `VLayout` + `Text` 手拼表头块（各 `Text` 的 `size` 宽度之和，必须等于下面表格各列 `columnSpan` 之和），
> 下面接 `<Table headerVisible="false">`。见 `examples/standard-table.xml`。

### 步骤 3 · 写模板

四个子节点在 `<template>` 下**顺序任意**：`parameters`（可选，纯文档）→ `sheetStyle`（可选）→ `styles`（可选）→ `container`（必填）。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<template xmlns="http://mengfly.github.io/excel-report/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://mengfly.github.io/excel-report/1.0.0 https://mengfly.github.io/xsd/excel-report-1.0.0.xsd"
          name="MyReport" version="1.0" author="MengFly"
          createAt="2026-09-29" description="报表说明">

    <parameters>
        <parameter id="title" name="报表标题" type="string"/>
        <parameter id="dataList" name="明细数据" type="List&lt;DataRow&gt;"/>
    </parameters>

    <sheetStyle>
        <defaultRowHeight>24</defaultRowHeight>
        <horizontallyCenter>true</horizontallyCenter>
    </sheetStyle>

    <styles>
        <style id="titleStyle">
            <fontName>微软雅黑</fontName>
            <fontBold>true</fontBold>
            <fontHeight>18</fontHeight>
            <height>36</height>
            <alignHorizontal>center</alignHorizontal>
            <alignVertical>center</alignVertical>
        </style>
        <style id="tableHeaderStyle">
            <width>auto</width>
            <height>26</height>
            <fontColor>white</fontColor>
            <fillForegroundColor>grey_50_percent</fillForegroundColor>
            <alignHorizontal>center</alignHorizontal>
        </style>
        <style id="tableDataStyle"><alignHorizontal>center</alignHorizontal></style>
        <style id="num4"><dataFormat>0.0000</dataFormat></style>
    </styles>

    <container>
        <VLayout>
            <Text size="-1,1" style="titleStyle" text="${title}"/>
            <Text size="-1,1" style="{alignHorizontal:right}" text="单位：元"/>
            <Table dataList="${dataList}" headerHeight="1">
                <column id="name"   name="名称" style="tableHeaderStyle" dataStyle="tableDataStyle"/>
                <column id="amount" name="金额" style="tableHeaderStyle" dataStyle="tableDataStyle num4"/>
            </Table>
            <Chart size="10,15">
                <Title text="金额分布" fontSize="16"/>
                <Legend position="top" overlay="false"/>
                <Marker showVal="true"/>
                <ChartData>
                    <labelAxis type="category">
                        <data title="名称" valueType="value" dataList="${labelList}"/>
                    </labelAxis>
                    <valueAxis1 title="金额" minimum="0" showMajorGridLines="true">
                        <data title="金额" type="bar" valueType="value" dataList="${valueList}" color="#4F81BD"/>
                    </valueAxis1>
                </ChartData>
            </Chart>
        </VLayout>
    </container>
</template>
```

要点（细节见 `references/template-spec.md`）：

- **`size="宽,高"`**：正整数 = 占几个 Excel 列 / 行；**`-1` = 弹性**（在 `HLayout` 里弹性宽、`VLayout` 里弹性高，
  按 `weight` 分配剩余空间）；`-1,-1` 即撑满父容器。
- **`style="..."`** 三种写法可混用：样式 id 列表（空格分隔）、内联 JSON `{key:value,...}`、混写 `"tableDataStyle {dataFormat:0.00 分}"`。
  内联 JSON 的 value 若含 `${}`，**必须用引号包住**：`style="{fontBold:'${index==0?true:false}'}"`（否则整段样式被丢弃）。
- 富文本：`text` 以 `<html>` 开头、`</html>` 结尾即走富文本，内部元素属性用样式 key：
  `text="&lt;html&gt;合计 &lt;font fontColor='red' fontBold='true'&gt;100&lt;/font&gt;&lt;/html&gt;"`
- `dataFormat` 直接用 Excel 格式串：`0.0000`、`#,##0`、`0.00 分`、`0%`。
- 中文报表建议显式设 `fontName`（默认字体是 `Arial`，中文会走字体回退，观感不稳）。

### 步骤 4 · 自检（交付前必做）

```bash
python "<skill_dir>/scripts/check_template.py" path/to/Template.xml
```

零依赖（Python stdlib），检查：标签/属性/样式 key 是否在规范内、`container` 单根、`size` 格式、样式 id 是否已定义、
枚举取值、必填属性，以及已知的静默坑（误导性 `width:auto`、`for` 缺 `:`、颜色名用错…）。
退出码 0 = 通过；`ERROR` 必须修；`WARN` 逐条确认。最后再过一遍 `references/pitfalls-checklist.md`。

脚本自带回归夹具：`examples/self-test-broken.xml`（预期 19 ERROR / 5 WARN，退出码 1）；`examples/` 里另外两个必须是 0 ERROR。

## 交付物与接线

| 事项 | 做法 |
|---|---|
| 模板放哪 | classpath 根目录（Maven 项目即 `src/main/resources/`）。默认从 classpath 加载 |
| id 命名 | 引用模板时可**不带 `.xml`**，会自动补 `.xml` |
| 开发期热改 | 可把模板来源配置成本地目录以便改完即生效；但**模板解析结果有缓存**，改完仍需清理缓存 |
| 渲染 | 每调用一次「导出模板」= 多一个 Sheet；全部导出后统一保存为文件 |
| Sheet 名 | 由调用时的 name 参数决定（与 `template@name` 无关）；重名会自动加 `_序号` |
| 数据传递 | 参数就是一个字符串键的 Map（`DataContext`），键名即模板里的变量名 |

调用方式见框架 README 的「使用方式（模板方式）」小节 —— 引入依赖 → 装配参数 → 加载模板 → 导出 → 保存。

## 参考文件

- `references/template-spec.md` — 完整骨架、`sheetStyle`(27 key) / `Style`(32 key) 名单与取值、颜色名单、`size` 语义、表达式、`include`、默认样式与默认尺寸。
- `references/components.md` — 14 个标签逐个说明（属性、子元素、默认尺寸、语义、示例）+ 布局规则。
- `references/pitfalls-checklist.md` — 静默失效「症状 → 根因 → 规避」索引 + 交付前 / 数据对接 Checklist。
- `examples/standard-table.xml` — 标题 + 单位 + 多级表头 + 表格 + 柱图 + 饼图。
- `examples/grouped-report.xml` — `for` 分组 + `if` 条件 + 组内表格 + 横向 `List` + 打印分页 + `include` 用法（注释形式）。
- `examples/self-test-broken.xml` — 自检脚本的负向夹具，**不是可用模板**。
