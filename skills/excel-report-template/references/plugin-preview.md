# IDEA 插件预览参数（`.idea/excel-report-preview`）

`excel-report-plugin` 的**模板预览**和**导出 Excel** 都由一份样本参数 JSON 驱动。路径固定按模板位置镜像：

```
<项目根>/.idea/excel-report-preview/<模板相对项目根的路径，去掉 .xml>.json
```

| 模板文件 | 参数文件 |
|---|---|
| `portal/src/main/resources/excel/Quake.xml` | `.idea/excel-report-preview/portal/src/main/resources/excel/Quake.json` |
| `src/main/resources/excel/demo/Report.xml` | `.idea/excel-report-preview/src/main/resources/excel/demo/Report.json` |

层级写错（少一层目录、换了文件名）预览就读不到，只会回退到自动生成的空骨架。

## 什么时候写

- **项目根存在 `.idea/excel-report-preview/` 目录时**：新增 / 修改模板后，同步新建或更新对应 JSON。
- 目录不存在时**不要创建**：说明该项目没用插件预览，跳过这一步。

## 写什么

一个合法 JSON 对象，key = 模板里实际用到的**顶层参数名**，value = 预览样本值：

```json
{
  "title": "各工区指标明细表",
  "tags": ["单位：天", "数据截至 2026-09-29"],
  "groups": [
    [
      { "seq": 1, "name": "甲工区", "minDayIndex": 3, "maxDayIndex": 18, "sensitivity": 0.87 }
    ]
  ]
}
```

对应模板：

```xml
<Text text="${title}"/>
<List dataList="${tags}" orientation="horizontal"/>
<VLayout for="group: ${groups}">
    <Table dataList="${group}" headerVisible="true">
        <column id="name" name="名称"/>
    </Table>
</VLayout>
```

规则：

- 顶层 key 必须与模板变量名**逐字一致**（Java 参数 Map 的 key、`Table@dataList`、`for` 的集合根）。
- 值用**真实类型**：数字写 `1` / `0.87`，不要写 `"1"`；布尔写 `true`；集合元素字段与 `${item.xxx}` / `column@id` 对齐。
- 样本要让每个区域都能渲染出内容：集合至少 1~2 项；`if` 依赖的字段要覆盖你想看到的分支。
- `for="k,v: ${map}"` 的集合根写成 JSON **对象**（`{"甲": 1, "乙": 2}`），不要写成数组。
- 纯 JSON：无注释、无尾逗号、双引号。整份解析失败时预览会忽略自定义参数（相当于所有变量缺失）。
- 图片字段可以写**相对项目根**的路径（如 `"img/logo.png"`），预览会临时展开为绝对路径；导出 / 线上数据仍需真实路径。
- 这是预览样本，**不要放生产数据或敏感信息**，也不要把大体量数据抄进来。

## 缺失和写错的表现

| 情况 | 预览表现 |
|---|---|
| 缺某个 key | 该表达式原样显示 `${xxx}`；`if` 条件解析失败按 `true` 处理 |
| 类型不对（数字写成字符串） | 不报错，但数字列变成文本，`dataFormat` 不生效 |
| 路径 / 文件名不对 | 读不到自定义参数，回退到空骨架，看起来像「模板没数据」 |
| JSON 非法 | 整份参数被忽略，表现同上 |

## 自动骨架 vs 手写

参数文件不存在时，插件会自动生成骨架并落盘，但推断能力有限：

- 只识别纯标识符路径（`a` / `a.b.c`）；含函数调用、运算符、字面量的表达式直接跳过。
- `for="item,idx: ${items}"` 的 `item` / `idx` 会折叠到集合根 `items`，并把 `items` 标成数组。
- 叶子一律 `""`，对象写 `{}`，数组写 `[""]` 或 `[{}]`（保证 `for` 至少渲染一行）。

⇒ 目录已存在时，按上面的规则手写完整样本，比等插件生成更接近真实数据，也更容易发现字段名对不上的问题。

> 插件「导出 Excel」复用同一份参数 JSON，填好即可同时用于预览和导出。
