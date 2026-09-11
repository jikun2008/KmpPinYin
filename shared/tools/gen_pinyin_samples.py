# -*- coding: utf-8 -*-
"""为 shared/PinyinVerifier.SAMPLES 生成期望拼音。

期望值不手打：本脚本只读库的数据源 libs/generator/src/main/resources/pinyin-dict.tsv，
按 :libs:tinypinyin-kt 的三条渲染规则独立算一遍，与 Kotlin 实现互为对照——
两边不一致就说明「实现」或「数据」有一方改动过。

三种输出：
  plain   -> ToneStyle.NONE       + PinyinCase.UPPERCASE
  number  -> ToneStyle.TONE_NUMBER + PinyinCase.LOWERCASE
  mark    -> ToneStyle.TONE_MARK   + PinyinCase.LOWERCASE

用法：
    python shared/tools/gen_pinyin_samples.py
结果写入同目录 samples-out.txt（Windows 控制台是 GBK，直接 print 中文/生僻字会报错），
把内容替换进 PinyinVerifier.SAMPLES 即可。
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
TSV = os.path.normpath(os.path.join(
    HERE, "..", "..", "libs", "generator", "src", "main", "resources", "pinyin-dict.tsv"))
OUT = os.path.join(HERE, "samples-out.txt")

MAIN = (0x4E00, 0x9FFF)
EXT_A = (0x3400, 0x4DBF)

UMLAUT = "\u00fc"
MARKS = {
    "a": "\u0101\u00e1\u01ce\u00e0",
    "e": "\u0113\u00e9\u011b\u00e8",
    "i": "\u012b\u00ed\u01d0\u00ec",
    "o": "\u014d\u00f3\u01d2\u00f2",
    "u": "\u016b\u00fa\u01d4\u00f9",
    UMLAUT: "\u01d6\u01d8\u01da\u01dc",
}
VOWELS = set("aeiou") | {UMLAUT}


def load():
    """codepoint -> (base syllable, tone 1..5)。"""
    d = {}
    with open(TSV, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            p = line.split("\t")
            if len(p) < 3:
                continue
            cp, base, tone = int(p[0].strip()), p[1].strip(), int(p[2].strip())
            if base:
                d[cp] = (base, tone)
    return d


DICT = load()


def entry(c):
    cp = ord(c)
    if MAIN[0] <= cp <= MAIN[1] or EXT_A[0] <= cp <= EXT_A[1]:
        return DICT.get(cp)
    return None


def mark_index(chars):
    """声调符号落点：a > e > ou 的 o > 最后一个元音（同 ToneMark.findMarkIndex）。"""
    for i, c in enumerate(chars):
        if c == "a":
            return i
    for i, c in enumerate(chars):
        if c == "e":
            return i
    ou = "".join(chars).find("ou")
    if ou >= 0:
        return ou
    last = -1
    for i, c in enumerate(chars):
        if c in VOWELS:
            last = i
    return last if last >= 0 else None


def marked(base, tone):
    plain = base.replace("v", UMLAUT)
    if not (1 <= tone <= 4):
        return plain
    chars = list(plain)
    idx = mark_index(chars)
    if idx is None:
        return plain
    table = MARKS.get(chars[idx])
    if table is None:
        return plain
    chars[idx] = table[tone - 1]
    return "".join(chars)


def render(text, style):
    out = []
    for c in text:
        e = entry(c)
        if e is None:
            out.append(c)  # 表外字符原样透传
            continue
        base, tone = e
        if style == "plain":
            out.append(base.upper())
        elif style == "number":
            out.append((base + str(tone)) if tone >= 1 else base)
        else:
            out.append(marked(base, tone))
    return " ".join(out)


# 需要新增/维护的样本：标题 + 待测文本。长文本（LONG_SAMPLE）由 Kotlin 侧引用，不在此列。
SAMPLES = [
    ("常用 你好", "你好"),
    ("常用 中华人民共和国", "中华人民共和国"),
    ("成语 掩耳盗铃", "掩耳盗铃"),
    ("成语 春暖花开", "春暖花开"),
    ("古诗 登鹳雀楼", "白日依山尽，黄河入海流。欲穷千里目，更上一层楼。"),
    ("古诗 念奴娇句", "大江东去，浪淘尽，千古风流人物。"),
    ("人名 张三丰武当", "张三丰在武当山练太极"),
    ("地名 内蒙古呼和浩特", "内蒙古呼和浩特"),
    ("地名 杭州西湖断桥", "杭州西湖断桥"),
    ("多音字 银行（表内默认音）", "银行"),
    ("多音字 行人", "行人"),
    ("多音字 重量", "重量"),
    ("多音字 重庆（无词典）", "重庆"),
    ("ü 韵母 女人绿色", "女人绿色"),
    ("轻声 妈妈笑了（le 为 tone 5）", "妈妈笑了"),
    ("儿化 花儿", "花儿"),
    ("繁体 中國聲調", "中國聲調"),
    ("生僻 饕餮貔貅", "饕餮貔貅"),
    ("生僻 龘靐齉爨", "龘靐齉爨"),
    ("Ext-A 生僻字", "㐀䶒"),
    ("数字混排 日期", "2026年9月10日星期四"),
    ("符号混排 价格", "价格：99.9元（原价199元）"),
    ("书名号 红楼梦", "《红楼梦》曹雪芹著"),
    ("中英混排（大小写不改字母）", "中文ABC你"),
    ("语气词 嗯呃哦唉", "嗯呃哦唉"),
    ("标点重复", "重复测试！重复测试？"),
    ("半角短横 + 空格透传", "北京-上海 高铁"),
]

PUNCT = set("，。、；：？！《》（）—…")


def main():
    missing = []
    for title, text in SAMPLES:
        for c in text:
            if ord(c) > 0x2000 and entry(c) is None and c not in PUNCT:
                missing.append("%s:%s(U+%04X)" % (title, c, ord(c)))

    out = ["MISSING = " + (", ".join(missing) if missing else "none"), ""]
    for title, text in SAMPLES:
        out.append('        Sample("%s", "%s",' % (title, text))
        out.append('            "%s",' % render(text, "plain"))
        out.append('            "%s",' % render(text, "number"))
        out.append('            "%s"),' % render(text, "mark"))

    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")
    print("wrote %s (%d samples)" % (OUT, len(SAMPLES)))


if __name__ == "__main__":
    main()
