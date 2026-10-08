#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""excel-report 报表模板离线自检（零依赖，Python 3.8+ stdlib）。

用法:
    python check_template.py <模板.xml> [更多模板...]

检查内容:
  ERROR  结构性错误：标签/属性不在模板规范内、container 非单根、size 格式非法、
         样式 id 未定义或重复、必填属性缺失、枚举取值非法、图表数据节点缺失……
  WARN   框架已知静默坑：Span/Chart 上 width:auto、Table/List 正数 size、
         for 缺冒号、颜色名可疑、表达式花括号不配对、样式定义后未引用……

退出码: 0 = 无 ERROR；1 = 存在 ERROR；2 = 用法/解析错误。
"""

import os
import re
import sys
import xml.etree.ElementTree as ET

LAYOUT_TAGS = {"VLayout", "HLayout", "GridLayout"}
COMPONENT_TAGS = {"VLayout", "HLayout", "GridLayout", "Text", "Table", "Image",
                  "List", "Span", "Link", "Chart"}
LAYOUT_EXTRA = {"include", "PageColSplit", "PageRowSplit"}
LEAF_TAGS = {"Text", "Link", "Span", "column", "header", "include",
             "PageColSplit", "PageRowSplit"}
CHART_NODES = {"ChartData", "Chart3dData", "PieData"}
VALUE_DATA_TYPES = {
    "ChartData": {"area", "bar", "line", "scatter"},
    "Chart3dData": {"area3d", "bar3d", "line3d"},
    "PieData": {"pie", "pie3d"},
}

STYLE_KEYS = {
    "borderTop", "borderBottom", "borderLeft", "borderRight",
    "topBorderColor", "bottomBorderColor", "leftBorderColor", "rightBorderColor",
    "hidden", "locked", "alignHorizontal", "alignVertical", "wrapText",
    "rotation", "indention", "fillPattern", "fillForegroundColor",
    "fillBackgroundColor", "readingOrder", "shrinkToFit", "width", "height",
    "dataFormat", "weight", "fontName", "fontHeight", "fontItalic",
    "fontStrikeout", "fontColor", "fontUnderline", "fontBold", "fontFamily",
}
SHEET_STYLE_KEYS = {
    "displayGuts", "displayZeros", "rowSumsBelow", "rowSumsRight",
    "displayGridlines", "printGridlines", "printRowAndColumnHeadings",
    "autobreaks", "forceFormulaRecalculation", "defaultColumnWidth",
    "defaultRowHeight", "displayRowColHeadings", "displayFormulas",
    "fitToPage", "horizontallyCenter", "verticallyCenter", "zoom", "tabColor",
    "committed", "margin", "marginLeft", "marginRight", "marginTop",
    "marginBottom", "marginHeader", "marginFooter", "password",
}

COMMON = {"id", "style", "if", "size"}
FOREACH = {"for"}
ATTRS = {
    "template": {"xmlns", "xmlns:xsi", "xsi:schemaLocation", "name", "version",
                 "description", "author", "createAt"},
    "parameters": set(),
    "parameter": {"id", "name", "type", "description", "required"},
    "sheetStyle": set(),
    "styles": set(),
    "style": {"id"},
    "container": set(),

    "VLayout": COMMON | FOREACH | {"alignPolicy"},
    "HLayout": COMMON | FOREACH | {"alignPolicy"},
    "GridLayout": COMMON | FOREACH | {"alignPolicy", "count", "orientation"},
    "Text": COMMON | {"text"},
    "Link": COMMON | {"text", "link", "label"},
    "Span": COMMON,
    "Table": COMMON | {"dataList", "headerVisible", "headerHeight"},
    "column": {"id", "name", "style", "dataStyle", "columnSpan"},
    "Image": COMMON | {"src", "anchorType", "scaleType", "scaleHeight", "padding"},
    "List": COMMON | {"dataList", "span", "orientation"},
    "header": {"style", "title", "span"},
    "Chart": COMMON | {"autoTitleDelete", "displayBlankAs", "anchorType"},
    "Title": {"text", "overlay", "bold", "fontSize", "underLine", "baseLine", "italic"},
    "Legend": {"position", "overlay"},
    "Marker": {"showPercent", "showBubbleSize", "showLeaderLines", "showSerName",
               "showCatName", "showVal", "showLegendKey"},
    "ChartData": set(),
    "Chart3dData": set(),
    "PieData": set(),
    "PageColSplit": {"if", "for"},
    "PageRowSplit": {"if", "for"},
    "include": None,  # None = 除 ref 外允许任意附加属性
}
AXIS = {"title", "numberFormat", "logBase", "majorUnit", "minorUnit", "minimum",
        "maximum", "orientation", "majorTickMark", "minorTickMark", "tickLabelPosition"}
GRID = {"showMajorGridLines", "showMinorGridLines"}
DATA = {"title", "valueType", "dataList", "address", "varyColors", "smooth",
        "showLeaderLines", "color"}
ATTRS["labelAxis"] = AXIS | GRID | {"type", "labelAlignment"}
ATTRS["valueAxis1"] = AXIS | GRID | FOREACH
ATTRS["valueAxis2"] = AXIS | GRID | FOREACH
ATTRS["valueAxis"] = AXIS
ATTRS["data"] = DATA | {"type"}

CHILDREN = {
    "template": {"parameters", "sheetStyle", "styles", "container"},
    "parameters": {"parameter"},
    "styles": {"style"},
    "style": STYLE_KEYS,
    "sheetStyle": SHEET_STYLE_KEYS,
    "container": COMPONENT_TAGS,
    "VLayout": COMPONENT_TAGS | LAYOUT_EXTRA,
    "HLayout": COMPONENT_TAGS | LAYOUT_EXTRA,
    "GridLayout": COMPONENT_TAGS | LAYOUT_EXTRA,
    "Chart": {"Title", "Legend", "Marker"} | CHART_NODES,
    "Table": {"column"},
    "List": {"header"},
    "ChartData": {"labelAxis", "valueAxis1", "valueAxis2"},
    "Chart3dData": {"labelAxis", "valueAxis1", "valueAxis2"},
    "PieData": {"labelAxis", "valueAxis"},
    "labelAxis": {"data"},
    "valueAxis1": {"data"},
    "valueAxis2": {"data"},
    "valueAxis": {"data"},
}

BORDER_ENUM = {"none", "thin", "medium", "dashed", "dotted", "thick", "double",
               "hair", "medium_dashed", "dash_dot", "medium_dash_dot",
               "dash_dot_dot", "medium_dash_dot_dot", "slanted_dash_dot"}
STYLE_ENUMS = {
    "borderTop": BORDER_ENUM, "borderBottom": BORDER_ENUM,
    "borderLeft": BORDER_ENUM, "borderRight": BORDER_ENUM,
    "alignHorizontal": {"left", "center", "right", "fill", "justify",
                        "center_selection", "distributed"},
    "alignVertical": {"top", "center", "bottom", "justify", "distributed"},
    "fillPattern": {"no_fill", "solid_foreground", "fine_dots", "alt_bars",
                    "sparse_dots", "thick_horz_bands", "thick_vert_bands",
                    "thick_backward_diag", "thick_forward_diag", "big_spots",
                    "bricks", "thin_horz_bands", "thin_vert_bands",
                    "thin_backward_diag", "thin_forward_diag", "squares",
                    "diamonds", "less_dots", "least_dots"},
    "readingOrder": {"context", "left_to_right", "right_to_left"},
    "fontUnderline": {"none", "single", "double", "single_accounting",
                      "double_accounting"},
    "fontFamily": {"not_applicable", "roman", "swiss", "modern", "script",
                   "decorative"},
}
BOOL_STYLE_KEYS = {"hidden", "locked", "wrapText", "shrinkToFit", "fontItalic",
                   "fontStrikeout", "fontBold"}
COLOR_STYLE_KEYS = {"topBorderColor", "bottomBorderColor", "leftBorderColor",
                    "rightBorderColor", "fillForegroundColor",
                    "fillBackgroundColor", "fontColor"}

ANCHOR_ENUM = {"MOVE_AND_RESIZE", "DONT_MOVE_DO_RESIZE", "MOVE_DONT_RESIZE",
               "DONT_MOVE_AND_RESIZE"}
ATTR_ENUMS = {
    ("VLayout", "alignPolicy"): {"start", "center", "end"},
    ("HLayout", "alignPolicy"): {"start", "center", "end"},
    ("GridLayout", "alignPolicy"): {"start", "center", "end"},
    ("GridLayout", "orientation"): {"horizontal", "vertical"},
    ("List", "orientation"): {"horizontal", "vertical"},
    ("Image", "scaleType"): {"FIT_START", "FIT_END", "FIT_XY", "CENTER"},
    ("Image", "anchorType"): ANCHOR_ENUM,
    ("Chart", "anchorType"): ANCHOR_ENUM,
    ("Chart", "displayBlankAs"): {"gap", "span", "zero"},
    ("Legend", "position"): {"bottom", "left", "right", "top", "top_right"},
    ("labelAxis", "type"): {"category", "date", "values"},
    ("labelAxis", "labelAlignment"): {"center", "left", "right"},
    ("Title", "underLine"): {"DASH", "DASH_HEAVY", "DASH_LONG", "DASH_LONG_HEAVY",
                             "DOUBLE", "DOT_DASH", "DOT_DASH_HEAVY",
                             "DOT_DOT_DASH", "DOT_DOT_DASH_HEAVY", "DOTTED",
                             "DOTTED_HEAVY", "HEAVY", "NONE", "SINGLE", "WAVY",
                             "WAVY_DOUBLE", "WAVY_HEAVY", "WORDS"},
}
for _t in ("labelAxis", "valueAxis1", "valueAxis2", "valueAxis"):
    ATTR_ENUMS[(_t, "orientation")] = {"minMax", "maxMin"}
    ATTR_ENUMS[(_t, "majorTickMark")] = {"none", "cross", "in", "out"}
    ATTR_ENUMS[(_t, "minorTickMark")] = {"none", "cross", "in", "out"}
    ATTR_ENUMS[(_t, "tickLabelPosition")] = {"nextTo", "none", "high", "low"}

HSSF_COLORS = {
    "BLACK", "BROWN", "OLIVE_GREEN", "DARK_GREEN", "DARK_TEAL", "DARK_BLUE",
    "INDIGO", "GREY_80_PERCENT", "ORANGE", "DARK_YELLOW", "GREEN", "TEAL",
    "BLUE", "BLUE_GREY", "GREY_50_PERCENT", "RED", "LIGHT_ORANGE", "LIME",
    "SEA_GREEN", "AQUA", "LIGHT_BLUE", "VIOLET", "GREY_40_PERCENT", "PINK",
    "GOLD", "YELLOW", "BRIGHT_GREEN", "TURQUOISE", "DARK_RED", "SKY_BLUE",
    "PLUM", "GREY_25_PERCENT", "ROSE", "LIGHT_YELLOW", "LIGHT_GREEN",
    "LIGHT_TURQUOISE", "PALE_BLUE", "LAVENDER", "WHITE", "CORNFLOWER_BLUE",
    "LEMON_CHIFFON", "MAROON", "ORCHID", "CORAL", "ROYAL_BLUE",
    "LIGHT_CORNFLOWER_BLUE", "AUTOMATIC", "TAN",
}
# 这些名字只属于「单元格颜色名」，图表颜色是另一套 ⇒ 用在图表上一定是错的
HSSF_ONLY = {"GREY_50_PERCENT", "GREY_25_PERCENT", "GREY_40_PERCENT",
             "GREY_80_PERCENT", "LIGHT_CORNFLOWER_BLUE", "BLUE_GREY",
             "AUTOMATIC"}
MERGE_ONLY_TAGS = {"Span", "Chart"}
DATA_SIZED_TAGS = {"Table", "List"}

HEX_COLOR = re.compile(r"#[0-9A-Fa-f]{6}\Z")
XML_ID = re.compile(r"[A-Za-z_][\w.\-]*\Z")


class Report:
    def __init__(self, path):
        self.path = path
        self.items = []

    def error(self, where, msg):
        self.items.append(("ERROR", where, msg))

    def warn(self, where, msg):
        self.items.append(("WARN", where, msg))

    def dump(self):
        names = os.path.basename(self.path)
        for level, where, msg in self.items:
            print("[%s] %s @ %s: %s" % (level, names, where, msg))
        n_err = sum(1 for i in self.items if i[0] == "ERROR")
        n_warn = sum(1 for i in self.items if i[0] == "WARN")
        print("--- %s: %d ERROR, %d WARN" % (names, n_err, n_warn))
        return n_err


def local(tag):
    return tag.split("}", 1)[1] if "}" in tag else tag


def split_tokens(raw):
    """按花括号外的空格切分 style 属性（对齐 StyleUtil.analysisStyle）。"""
    tokens, cur, depth = [], [], 0
    for ch in raw:
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        if ch == " " and depth == 0:
            if cur:
                tokens.append("".join(cur))
                cur = []
        else:
            cur.append(ch)
    if cur:
        tokens.append("".join(cur))
    return tokens, depth


def split_json(inner):
    parts, cur, depth = [], [], 0
    for ch in inner:
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(cur))
            cur = []
        else:
            cur.append(ch)
    if cur:
        parts.append("".join(cur))
    return [p.strip() for p in parts if p.strip()]


def expr_balanced(val):
    """检查所有 ${ ... } 是否配对（允许内部再出现花括号）。"""
    i = 0
    while True:
        i = val.find("${", i)
        if i < 0:
            return True
        depth, j = 0, i + 2
        while j < len(val):
            if val[j] == "{":
                depth += 1
            elif val[j] == "}":
                if depth == 0:
                    break
                depth -= 1
            j += 1
        if j >= len(val):
            return False
        i = j + 1


def check_style_attr(rep, where, raw, style_ids, used_ids):
    tokens, depth = split_tokens(raw)
    if depth != 0:
        rep.error(where, "style 花括号不平衡（%d 个未闭合），整段样式会被丢弃: %r" % (depth, raw))
    for tk in tokens:
        if tk.startswith("{"):
            inner = tk[1:-1] if tk.endswith("}") else tk[1:]
            for pair in split_json(inner):
                if ":" not in pair:
                    rep.error(where, "内联样式片段缺少 ':'（JSON 解析失败会丢掉整段）: %r" % pair)
                    continue
                key, val = pair.split(":", 1)
                key = key.strip().strip("\"'")
                if key and key not in STYLE_KEYS:
                    rep.error(where, "内联样式 key %r 不在允许的样式 key 清单中（会被静默丢弃）" % key)
                elif val and "${" in val and not re.match(r"\s*['\"]", val):
                    rep.warn(where, "内联样式 value 含 ${} 但未加引号（{} 会破坏 JSON 结构）: %r"
                             % val.strip())
        else:
            used_ids.add(tk)
            if tk not in style_ids:
                rep.error(where, "引用未定义的样式 id: %r" % tk)


def is_cell_color(val):
    v = (val or "").strip()
    return bool(HEX_COLOR.match(v)) or v.upper() in HSSF_COLORS


def walk(rep, node, parents, style_ids, used_ids):
    tag = local(node.tag)
    path = "/".join(parents + [tag])

    # `<style>` / `<sheetStyle>` 的子元素是「样式 key」，不是组件，父节点已校验
    if tag in STYLE_KEYS or tag in SHEET_STYLE_KEYS:
        return

    if tag not in ATTRS:
        rep.error(path, "标签不在允许的组件列表中（渲染时会直接报错）")
        return

    if ATTRS[tag] is not None:
        for name in node.attrib:
            if name.startswith("{") or name.startswith("xmlns"):
                continue
            if name not in ATTRS[tag]:
                rep.error(path, "属性 %r 不在模板规范内（可能被忽略）" % name)

    if tag in LAYOUT_TAGS or tag in ("Text", "Link", "Span", "Image", "Table", "List", "Chart"):
        raw_size = node.get("size")
        if raw_size is not None:
            if not re.match(r"\s*-?\d+\s*,\s*-?\d+\s*\Z", raw_size):
                rep.error(path, "size 必须形如 `宽,高`（整数，-1 表示弹性），实际: %r" % raw_size)
            elif tag in DATA_SIZED_TAGS:
                w, h = [int(x) for x in raw_size.replace(" ", "").split(",")]
                if w > 0 or h > 0:
                    rep.warn(path, "%s 按数据自算格数，正数 size 被忽略；要撑满请写 -1" % tag)

    for (t, attr), mapping in ATTR_ENUMS.items():
        if t == tag and node.get(attr) is not None and node.get(attr) not in mapping:
            rep.error(path, "%s=%r 非法，允许: %s" % (attr, node.get(attr), sorted(mapping)))

    if tag == "GridLayout" and node.get("count") is not None:
        if not re.match(r"\d+\Z", node.get("count")) or int(node.get("count")) < 1:
            rep.error(path, "GridLayout@count 必须是 >= 1 的整数，实际 %r" % node.get("count"))

    if tag == "template" and node.get("name") is None:
        rep.error(path, "<template> 缺少必填属性 name")

    if tag == "style":
        sid = node.get("id")
        if not sid:
            rep.error(path, "<style> 缺少必填属性 id")
        elif not XML_ID.match(sid):
            rep.error(path, "样式 id %r 不是合法 XML ID（须以字母/下划线开头）" % sid)
        for child in node:
            key = local(child.tag)
            if key not in STYLE_KEYS:
                rep.error("%s/%s" % (path, key), "样式 key 不在允许的样式 key 清单中（会被静默丢弃）")
                continue
            val = (child.text or "").strip()
            if key in STYLE_ENUMS and val.lower() not in STYLE_ENUMS[key]:
                rep.error("%s/%s" % (path, key), "取值 %r 非法，允许: %s" % (val, sorted(STYLE_ENUMS[key])))
            elif key in BOOL_STYLE_KEYS and val.lower() not in ("true", "false"):
                rep.error("%s/%s" % (path, key), "应为 true/false，实际 %r" % val)
            elif key in COLOR_STYLE_KEYS and not is_cell_color(val):
                rep.warn("%s/%s" % (path, key),
                         "颜色 %r 既不是 #RRGGBB 也不在单元格颜色名清单中 ⇒ 静默回退黑色" % val)

    if tag == "sheetStyle":
        for child in node:
            key = local(child.tag)
            if key not in SHEET_STYLE_KEYS:
                rep.error(path, "sheetStyle key %r 不在允许的 key 清单中（会被静默丢弃）" % key)

    if tag == "parameter" and (node.get("id") is None or node.get("name") is None):
        rep.error(path, "<parameter> 需要 id 与 name")

    if tag == "Text" and node.get("text") is None:
        rep.error(path, "<Text> 缺少必填属性 text")

    if tag == "Link" and (node.get("text") is None or node.get("link") is None):
        rep.error(path, "<Link> 需要 text 与 link")

    if tag == "Image" and node.get("src") is None:
        rep.error(path, "<Image> 缺少必填属性 src")

    if tag in DATA_SIZED_TAGS and node.get("dataList") is None:
        rep.error(path, "<%s> 缺少必填属性 dataList" % tag)
    if tag in DATA_SIZED_TAGS and node.get("dataList") == "":
        rep.warn(path, "<%s>@dataList 是空字符串 ⇒ 取不到列表数据，只会渲染表头" % tag)

    if tag == "column" and (node.get("id") is None or node.get("name") is None):
        rep.error(path, "<column> 需要 id 与 name")

    if tag == "header" and node.get("title") is None:
        rep.error(path, "<header> 缺少必填属性 title")

    if tag == "Table":
        cols = [c for c in node if local(c.tag) == "column"]
        if not cols:
            rep.error(path, "<Table> 至少要有一个 <column>")

    if tag == "Chart":
        nodes = [c for c in node if local(c.tag) in CHART_NODES]
        if len(nodes) != 1:
            rep.error(path, "图表必须且只能有 1 个数据节点（ChartData/Chart3dData/PieData），"
                            "实际 %d 个：0 个时组件静默消失，多个只取第一个" % len(nodes))
        for c in node:
            if local(c.tag) in CHART_NODES:
                if not any(local(g.tag) in ("valueAxis1", "valueAxis2", "valueAxis") for g in c):
                    rep.error("%s/%s" % (path, local(c.tag)), "缺少数值轴（valueAxis1/valueAxis2/valueAxis）")

    if tag == "data":
        container = next((a for a in reversed(parents) if a in VALUE_DATA_TYPES), None)
        dt = node.get("type")
        if dt and container and dt not in VALUE_DATA_TYPES[container]:
            rep.error(path, "type=%r 不属于 <%s> 允许的类型 %s"
                      % (dt, container, sorted(VALUE_DATA_TYPES[container])))
        if not node.get("dataList") and not node.get("address"):
            rep.warn(path, "<data> 既无 dataList(valueType=value) 也无 address(valueType=relation)"
                           " ⇒ 系列会被静默丢弃")
        color = node.get("color")
        if color and not HEX_COLOR.match(color) and color.upper() in HSSF_ONLY:
            rep.warn(path, "图表颜色 %r 只属于单元格颜色名，图表颜色是另一套 ⇒ 会回退默认色" % color)

    if tag in MERGE_ONLY_TAGS and "width:auto" in (node.get("style") or "").replace(" ", ""):
        rep.warn(path, "%s 只 merge 不写值，width:auto 在收尾补算时会得到 3 个字符宽的垃圾值" % tag)

    for attr in ("style", "dataStyle"):
        if node.get(attr):
            check_style_attr(rep, "%s@%s" % (path, attr), node.get(attr), style_ids, used_ids)

    for name, val in node.attrib.items():
        if "${" in val and not expr_balanced(val):
            rep.warn("%s@%s" % (path, name),
                     "表达式花括号不配对（SpEL 解析失败会按字面量输出）: %r" % val)

    fe = node.get("for")
    if fe is not None and ":" not in fe:
        rep.error(path, "for=%r 缺少 ':' ⇒ 只会渲染一次（静默）" % fe)

    # 子元素白名单（style / sheetStyle 的子元素是样式 key，已在上面分支校验）
    expect = CHILDREN.get(tag)
    if expect is not None and tag not in ("style", "sheetStyle"):
        for child in node:
            if local(child.tag) not in expect:
                rep.error("%s/%s" % (path, local(child.tag)),
                          "不允许出现在 <%s> 下，允许: %s" % (tag, sorted(expect)))
    elif expect is None and tag in LEAF_TAGS:
        for child in node:
            rep.error("%s/%s" % (path, local(child.tag)), "<%s> 不应有子元素" % tag)

    if tag in ("style", "sheetStyle"):
        return

    for child in node:
        if local(child.tag) in ATTRS:
            walk(rep, child, parents + [tag], style_ids, used_ids)


def check_file(path):
    rep = Report(path)
    try:
        root = ET.parse(path).getroot()
    except Exception as e:  # noqa: BLE001
        print("[ERROR] %s: XML 解析失败: %s" % (os.path.basename(path), e))
        return 1
    if local(root.tag) != "template":
        print("[ERROR] %s: 根节点应为 <template>，实际 <%s>" % (os.path.basename(path), local(root.tag)))
        return 1

    containers = [c for c in root if local(c.tag) == "container"]
    if not containers:
        rep.error("template", "<template> 缺少必填的 <container>")
    for c in containers:
        n = len(list(c))
        if n != 1:
            rep.error("container", "<container> 下必须恰好 1 个根组件，实际 %d 个"
                                  "（运行期只取第一个，其余静默丢弃）" % n)

    style_ids = set()
    for el in root.iter():
        if local(el.tag) == "styles":
            for st in el:
                if local(st.tag) == "style" and st.get("id"):
                    if st.get("id") in style_ids:
                        rep.error("style@id", "重复的样式 id: %r" % st.get("id"))
                    style_ids.add(st.get("id"))

    used_ids = set()
    for child in root:
        walk(rep, child, [], style_ids, used_ids)

    for sid in sorted(style_ids - used_ids):
        rep.warn("style@id", "样式 id %r 定义后未被引用" % sid)

    return rep.dump()


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    rc = 0
    for p in argv[1:]:
        if check_file(p):
            rc = 1
    return rc


if __name__ == "__main__":
    sys.exit(main(sys.argv))
