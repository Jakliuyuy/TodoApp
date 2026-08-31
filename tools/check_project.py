#!/usr/bin/env python3
"""工程静态自检：在没有 Android SDK 的环境下，尽可能提前发现会导致编译失败的问题。

检查项：
1. 所有 XML 文件能否被解析
2. Kotlin 文件的括号是否配对
3. stringResource(R.string.xxx) 引用的字符串是否都已定义
4. XML 资源交叉引用（@string / @mipmap / @style / @xml）是否存在
5. 代码中用到的首字母大写的类型，是否都有对应 import（粗粒度，仅提示）

用法: python3 tools/check_project.py
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main")
RES = os.path.join(SRC, "res")

# Kotlin / Java 标准库里首字母大写的常用标识符，无需 import
BUILTIN = {
    "String", "Int", "Long", "Float", "Double", "Boolean", "Byte", "Char", "Short",
    "Number", "Any", "Unit", "Nothing", "List", "MutableList", "Set", "MutableSet",
    "Map", "MutableMap", "Array", "Pair", "Triple", "IntArray", "StringBuilder",
    "Exception", "RuntimeException", "IOException", "IllegalArgumentException",
    "IllegalStateException", "System", "UUID", "Math", "Comparable", "Iterable",
    "Sequence", "Regex", "CharSequence", "Enum", "Annotation", "Deprecated",
    "OptIn", "Composable", "Suppress", "JvmName", "JvmStatic", "Serializable",
    "Application", "Context", "Bundle", "ComponentActivity", "R", "Filter", "Task",
}

problems = []
notes = []


def add_problem(msg):
    problems.append(msg)


def add_note(msg):
    notes.append(msg)


def check_xml():
    for dirpath, _, filenames in os.walk(RES):
        for name in filenames:
            if not name.endswith(".xml"):
                continue
            path = os.path.join(dirpath, name)
            try:
                ET.parse(path)
            except ET.ParseError as e:
                add_problem(f"[XML 语法错误] {os.path.relpath(path, ROOT)}: {e}")
    manifest = os.path.join(SRC, "AndroidManifest.xml")
    if os.path.exists(manifest):
        try:
            ET.parse(manifest)
        except ET.ParseError as e:
            add_problem(f"[XML 语法错误] AndroidManifest.xml: {e}")


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def strip_comments_and_strings(code):
    code = re.sub(r"/\*.*?\*/", "", code, flags=re.S)
    code = re.sub(r"//[^\n]*", "", code)
    code = re.sub(r'"(?:\\.|[^"\\])*"', '""', code)
    return code


def check_brackets(path, code):
    pairs = {"(": ")", "{": "}", "[": "]"}
    for open_ch, close_ch in pairs.items():
        if code.count(open_ch) != code.count(close_ch):
            add_problem(
                f"[括号不配对] {os.path.relpath(path, ROOT)}: "
                f"{open_ch} 出现 {code.count(open_ch)} 次，{close_ch} 出现 {code.count(close_ch)} 次"
            )


def collect_defined_strings():
    names = set()
    for dirpath, _, filenames in os.walk(RES):
        for name in filenames:
            if name != "strings.xml":
                continue
            path = os.path.join(dirpath, name)
            try:
                root = ET.parse(path).getroot()
            except ET.ParseError:
                continue
            for child in root:
                if child.tag == "string":
                    names.add(child.attrib.get("name", ""))
    return names


def collect_res_names():
    """收集各类资源名，用于校验 @string/@mipmap/@style/@xml 引用"""
    res = {"string": set(), "mipmap": set(), "style": set(), "xml": set(), "color": set()}
    for dirpath, _, filenames in os.walk(RES):
        kind = os.path.basename(dirpath).split("-")[0]
        for name in filenames:
            path = os.path.join(dirpath, name)
            if not name.endswith(".xml"):
                # 图片等二进制资源（mipmap/ic_launcher.png 等），文件名即资源名
                if kind in res:
                    res[kind].add(os.path.splitext(name)[0])
                continue
            try:
                root = ET.parse(path).getroot()
            except ET.ParseError:
                continue
            # values 目录：按标签名收集（string / color / style ...）
            if os.path.basename(dirpath).startswith("values"):
                for child in root:
                    if child.tag in res:
                        res[child.tag].add(child.attrib.get("name", ""))
            else:
                if kind in res:
                    res[kind].add(os.path.splitext(name)[0])
    return res


def check_kt_files():
    strings = collect_defined_strings()
    res = collect_res_names()

    project_symbols = set()
    kt_files = []
    for dirpath, _, filenames in os.walk(os.path.join(SRC, "java")):
        for name in filenames:
            if name.endswith(".kt"):
                kt_files.append(os.path.join(dirpath, name))
    # 先收集工程内定义的符号（同包可直接使用）
    for path in kt_files:
        code = read(path)
        body = strip_comments_and_strings(code)
        for m in re.finditer(r"\b(?:data class|class|object|interface|enum class|fun|val)\s+([A-Z]\w*)", body):
            project_symbols.add(m.group(1))
        # 枚举成员（Filter.ALL 这类写法）
        for m in re.finditer(r"\benum class\s+\w+\s*\{([^}]*)\}", body):
            for member in re.finditer(r"\b([A-Z][A-Z0-9_]*)\b", m.group(1)):
                project_symbols.add(member.group(1))

    for path in kt_files:
        code = read(path)
        rel = os.path.relpath(path, ROOT)
        check_brackets(path, strip_comments_and_strings(code))

        body = strip_comments_and_strings(code)
        body_no_imports = "\n".join(
            line for line in body.splitlines()
            if not line.strip().startswith("import ") and not line.strip().startswith("package ")
        )

        # stringResource(R.string.xxx) 校验
        for m in re.finditer(r"stringResource\(\s*R\.string\.(\w+)", body):
            if m.group(1) not in strings:
                add_problem(f"[字符串缺失] {rel}: R.string.{m.group(1)} 未在 strings.xml 中定义")

        # 检查 import 的符号是否真的用到（仅提示）
        imported = set()
        for m in re.finditer(r"^import\s+([\w.]+)(?:\s+as\s+(\w+))?", body, flags=re.M):
            full = m.group(1)
            alias = m.group(2) or full.rsplit(".", 1)[-1]
            imported.add(alias)
        for alias in sorted(imported):
            # getValue / setValue 由 "by" 委托语法隐式使用，文本中不会显式出现
            if alias in ("getValue", "setValue", "getValueAsState"):
                continue
            if not re.search(r"\b" + re.escape(alias) + r"\b", body_no_imports):
                add_note(f"[未使用的 import（不报错）] {rel}: {alias}")

        # 可能缺失 import 的类型：跳过前面紧跟 "." 的成员访问（如 Icons.Filled、Filter.ALL）
        for m in re.finditer(r"(?<![\w.])([A-Z]\w+)", body_no_imports):
            sym = m.group(1)
            if sym in BUILTIN or sym in imported or sym in project_symbols:
                continue
            if re.search(r"\b(?:class|object|enum class|interface|data class)\s+" + re.escape(sym) + r"\b", body):
                continue
            add_note(f"[可能缺失 import] {rel}: {sym}")

    # XML 交叉引用校验
    for dirpath, _, filenames in os.walk(RES):
        for name in filenames:
            if not name.endswith(".xml"):
                continue
            path = os.path.join(dirpath, name)
            content = read(path)
            for m in re.finditer(r"@(string|mipmap|style|xml|color)/([\w.]+)", content):
                kind, key = m.group(1), m.group(2)
                if kind in res and key not in res[kind]:
                    add_problem(
                        f"[资源引用缺失] {os.path.relpath(path, ROOT)}: "
                        f"@{kind}/{key} 未找到（已定义: {sorted(res[kind])[:8]}）"
                    )
    manifest = os.path.join(SRC, "AndroidManifest.xml")
    if os.path.exists(manifest):
        content = read(manifest)
        for m in re.finditer(r"@(string|mipmap|style|xml|color)/([\w.]+)", content):
            kind, key = m.group(1), m.group(2)
            if kind in res and key not in res[kind]:
                add_problem(f"[资源引用缺失] AndroidManifest.xml: @{kind}/{key} 未找到")

    # Activity 是否存在
    if os.path.exists(manifest):
        content = read(manifest)
        for m in re.finditer(r'android:name="\.([\w.]+)"', content):
            simple = m.group(1).split(".")[-1]
            found = any(os.path.basename(p) == f"{simple}.kt" for p in kt_files)
            if not found:
                add_problem(f"[Activity 缺失] AndroidManifest.xml: .{m.group(1)} 对应的 {simple}.kt 不存在")


def main():
    check_xml()
    check_kt_files()

    print("=" * 60)
    if problems:
        print(f"发现 {len(problems)} 个必须修复的问题：\n")
        for p in problems:
            print("  ✗", p)
    else:
        print("✓ 未发现会导致编译失败的问题")

    if notes:
        print(f"\n{len(notes)} 条提示（不影响编译）：")
        for n in notes[:40]:
            print("  ·", n)
        if len(notes) > 40:
            print(f"  … 另有 {len(notes) - 40} 条")
    print("=" * 60)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
