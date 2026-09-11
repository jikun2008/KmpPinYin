# -*- coding: utf-8 -*-
"""Functional port of the Kotlin runtime to validate all test assertions.

Mirrors PinyinTable.codeOf + ToneMark + DictMatcher + Pinyin.toPinyin exactly.
"""
import re

ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
src = open("tinypinyin-kt/src/commonMain/kotlin/com/github/kmppy/internal/PinyinTable.kt",
           encoding="utf-8").read()
SYL = re.search(r'SYLLABLES_STRING = "([^"]+)"', src).group(1).split(" ")


def grab(name):
    # PinyinTable emits one quoted chunk per line with a trailing " +" (Kotlin-legal
    # continuation), so collect every chunk up to the blank line that ends the declaration.
    m = re.search(name + r'\s*=\s*\n(.*?)\n[ \t]*\n', src, re.S)
    return "".join(re.findall(r'"([A-Za-z0-9\-_]+)"', m.group(1)))


MAIN = grab("MAIN_TABLE_STRING")
EXTA = grab("EXT_A_TABLE_STRING")

MARKS = {
    'a': 'āáǎà', 'e': 'ēéěè', 'i': 'īíǐì',
    'o': 'ōóǒò', 'u': 'ūúǔù', 'ü': 'ǖǘǚǜ',
}
VOWELS = set("aeiouü")


def code_of(ch):
    cp = ord(ch)
    if 0x4E00 <= cp <= 0x9FFF:
        arr, base = MAIN, 0x4E00
    elif 0x3400 <= cp <= 0x4DBF:
        arr, base = EXTA, 0x3400
    else:
        return 0
    i = cp - base
    return (ALPHA.index(arr[i * 2]) << 6) | ALPHA.index(arr[i * 2 + 1])


def entry_of(ch):
    code = code_of(ch)
    if code == 0:
        return None
    return SYL[(code >> 3) - 1], code & 7


def mark(syll, tone):
    plain = syll.replace('v', 'ü').replace('V', 'Ü')
    if tone not in (1, 2, 3, 4):
        return plain
    lower = plain.lower()
    idx = None
    if 'a' in lower:
        idx = lower.index('a')
    elif 'e' in lower:
        idx = lower.index('e')
    elif 'ou' in lower:
        idx = lower.index('ou')
    else:
        last = -1
        for j, c in enumerate(lower):
            if c in VOWELS:
                last = j
        idx = last
    if idx is None or idx < 0 or lower[idx] not in MARKS:
        return plain
    lst = list(plain)
    ch = MARKS[lower[idx]][tone - 1]
    if plain[idx].isupper():
        ch = ch.upper()
    lst[idx] = ch
    return "".join(lst)


def apply_case(s, case):
    if case == 'UPPER':
        return s.upper()
    if case == 'LOWER':
        return s.lower()
    return s


def format_entry(syll, tone, style, case):
    if style == 'NONE':
        shaped = syll
    elif style == 'NUMBER':
        shaped = syll + str(tone) if tone >= 1 else syll
    else:
        shaped = mark(syll, tone)
    return apply_case(shaped, case)


def format_dict_value(val, style, case):
    """Mirror Pinyin.formatDictValue: a trailing 1..5 is a tone, else the value is literal."""
    if val and val[-1] in '12345' and len(val) > 1 and is_known_syllable(val[:-1]):
        return format_entry(val[:-1], int(val[-1]), style, case)
    return apply_case(val, case)


SYL_SET = set(SYL)


def is_known_syllable(s):
    """Mirror Pinyin.isKnownSyllable - only real syllables may carry a tone digit."""
    if not s:
        return False
    key = s.lower().replace('ü', 'v')
    return len(key) == len(s) and key in SYL_SET


class Matcher:
    def __init__(self, mapping):
        self.root = {}
        for word, vals in mapping.items():
            node = self.root
            for c in word:
                node = node.setdefault(c, {})
            node['$'] = vals

    def match(self, chars, start):
        node = self.root
        best = None
        best_len = 0
        i = start
        while i < len(chars):
            nxt = node.get(chars[i])
            if nxt is None:
                break
            node = nxt
            if '$' in node and node['$']:
                best = node['$']
                best_len = i - start + 1
            i += 1
        return (best, best_len) if best else None


def to_pinyin_char(ch, style='NONE', case='UPPER'):
    e = entry_of(ch)
    if not e:
        return ch
    return format_entry(e[0], e[1], style, case)


def to_pinyin(text, sep=' ', style='NONE', case='UPPER', matcher=None):
    if not text:
        return text
    chars = list(text)
    tokens = []
    i = 0
    while i < len(chars):
        m = matcher.match(chars, i) if matcher else None
        if m:
            vals, ln = m
            for k in range(ln):
                if k < len(vals):
                    tokens.append(format_dict_value(vals[k], style, case))
                else:
                    tokens.append(to_pinyin_char(chars[i + k], style, case))
            i += ln
        else:
            tokens.append(to_pinyin_char(chars[i], style, case))
            i += 1
    return sep.join(tokens)


# ---- assertions ----
fails = []


def check(desc, got, exp):
    ok = got == exp
    print(("PASS" if ok else "FAIL"), desc, "=>", repr(got), "" if ok else ("expected " + repr(exp)))
    if not ok:
        fails.append(desc)


# PinyinTest
check("char 中", to_pinyin_char('中'), "ZHONG")
check("char 文", to_pinyin_char('文'), "WEN")
check("char 国", to_pinyin_char('国'), "GUO")
check("char A", to_pinyin_char('A'), "A")
check("char space", to_pinyin_char(' '), " ")
check("char kana", to_pinyin_char('あ'), "あ")
check("str 中文", to_pinyin("中文"), "ZHONG WEN")
check("str 中文-nosep", to_pinyin("中文", ""), "ZHONGWEN")
check("str 中文-dash", to_pinyin("中文", "-"), "ZHONG-WEN")
check("str 中2文", to_pinyin("中2文", " "), "ZHONG 2 WEN")
check("isChinese 一", code_of('一') != 0, True)
check("isChinese 龥", code_of('龥') != 0, True)
check("isChinese extA 㐀", code_of('\u3400') != 0, True)
check("isChinese A", code_of('A') != 0, False)
check("isChinese hangul", code_of('가') != 0, False)
check("empty", to_pinyin(""), "")

# ToneTest
check("num 中", to_pinyin_char('中', 'NUMBER', 'UPPER'), "ZHONG1")
check("num 文", to_pinyin_char('文', 'NUMBER', 'UPPER'), "WEN2")
check("num 国", to_pinyin_char('国', 'NUMBER', 'UPPER'), "GUO2")
check("num 的", to_pinyin_char('的', 'NUMBER', 'UPPER'), "DE5")
check("num 国中", to_pinyin("国中", " ", 'NUMBER', 'UPPER'), "GUO2 ZHONG1")
check("mark 中", to_pinyin_char('中', 'MARK', 'LOWER'), "zhōng")
check("mark 文", to_pinyin_char('文', 'MARK', 'LOWER'), "wén")
check("mark 一", to_pinyin_char('一', 'MARK', 'LOWER'), "yī")
check("mark 全", to_pinyin_char('全', 'MARK', 'LOWER'), "quán")
check("mark 学", to_pinyin_char('学', 'MARK', 'LOWER'), "xué")
check("mark 女", to_pinyin_char('女', 'MARK', 'LOWER'), "nǚ")
check("mark 绿", to_pinyin_char('绿', 'MARK', 'LOWER'), "lǜ")
check("mark 的-neutral", to_pinyin_char('的', 'MARK', 'LOWER'), "de")
check("lower 中", to_pinyin_char('中', 'NONE', 'LOWER'), "zhong")

# DictTest
d1 = Matcher({"重庆": ["CHONG", "QING"]})
check("dict 重庆", to_pinyin("重庆", " ", 'NONE', 'UPPER', d1), "CHONG QING")
check("nodict 重庆", to_pinyin("重庆", " ", 'NONE', 'UPPER', None), "ZHONG QING")
check("dict sentence", to_pinyin("我去重庆", " ", 'NONE', 'UPPER', d1), "WO QU CHONG QING")
d2 = Matcher({"中国人": ["ZHONG1"]})
check("dict short", to_pinyin("中国人", " ", 'NONE', 'UPPER', d2), "ZHONG GUO REN")

# Tone digit on a dictionary value (see Pinyin.formatDictValue)
d3 = Matcher({"重庆": ["CHONG2", "QING4"], "成都": ["CHENG2", "DU1"], "绿": ["LV4"]})
check("dict tone num", to_pinyin("重庆", " ", 'NUMBER', 'LOWER', d3), "chong2 qing4")
check("dict tone mark", to_pinyin("我去重庆", " ", 'MARK', 'LOWER', d3), "wǒ qù chóng qìng")
check("dict tone mark orig-case", to_pinyin("重庆", " ", 'MARK', 'ORIG', d3), "CHÓNG QÌNG")
check("dict tone none strips digit", to_pinyin("成都", " ", 'NONE', 'UPPER', d3), "CHENG DU")
check("dict v -> umlaut", to_pinyin("绿", " ", 'MARK', 'LOWER', d3), "lǜ")
check("nodict 成都", to_pinyin("成都", " ", 'NUMBER', 'LOWER', None), "cheng2 dou1")
# "XD2" is not a syllable, so the digit must stay part of the literal
d4 = Matcher({"中国人": ["XD2"]})
check("dict non-syllable + digit", to_pinyin("中国人", " ", 'MARK', 'ORIG', d4), "XD2 guó rén")

# CnCityDictTest
city = Matcher({"重庆": ["CHONG2", "QING4"], "上海": ["SHANG4", "HAI3"]})
check("city 重庆", to_pinyin("重庆", " ", 'NONE', 'UPPER', city), "CHONG QING")
check("city 我在上海", to_pinyin("我在上海", " ", 'NONE', 'UPPER', city), "WO ZAI SHANG HAI")
check("city 我在上海 mark", to_pinyin("我在上海", " ", 'MARK', 'LOWER', city), "wǒ zài shàng hǎi")

# PropertyTest
check("tokenCount", len(to_pinyin("中文ABC你", "|").split("|")), len("中文ABC你"))

print()
print("TOTAL FAILURES:", len(fails))
if fails:
    raise SystemExit(1)
