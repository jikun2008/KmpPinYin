# -*- coding: utf-8 -*-
"""Bootstrap the raw pinyin data (pinyin-dict.tsv) from the `pypinyin` dataset.

This is a one-off provenance helper. The authoritative build-time input for the
Kotlin generator is the produced TSV file, so this script (and the external
pypinyin package) is NOT needed to rebuild the library constants.

Output: <repo>/generator/src/main/resources/pinyin-dict.tsv
  Columns (tab separated): codepoint(decimal) <TAB> base_syllable(ascii) <TAB> tone(1..5)
  - base_syllable: tone-less ASCII syllable, using 'v' for ue after n/l
    (standard pinyin-data convention, e.g. nv3 / lv4 / ju1 / quan2).
  - tone: 1..4 for the four tones, 5 for neutral (qing sheng).
  - One row per CJK code point: only the FIRST (most common) reading is stored,
    matching TinyPinyin's single-default-reading design.
"""
import os
import sys

from pypinyin import pinyin_dict as _pinyin_dict_module
from pypinyin.contrib.tone_convert import to_tone3

CHAR_DICT = _pinyin_dict_module.pinyin_dict


def split_tone3(tone3: str):
    """'zhong1' -> ('zhong', 1); 'de' -> ('de', 5-neutral)."""
    if tone3 and tone3[-1].isdigit():
        return tone3[:-1], int(tone3[-1])
    return tone3, 5


def main():
    out_dir = os.path.abspath(
        os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
    )
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "pinyin-dict.tsv")

    rows = []
    for cp in sorted(CHAR_DICT.keys()):
        raw = CHAR_DICT[cp]
        first = raw.split(",")[0].strip()
        if not first:
            continue
        tone3 = to_tone3(first)
        base, tone = split_tone3(tone3)
        if not base or not base.isascii():
            # skip anything that did not normalize to pure ASCII
            continue
        rows.append((cp, base, tone))

    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# codepoint<TAB>base_syllable<TAB>tone(1..5)\n")
        for cp, base, tone in rows:
            f.write("%d\t%s\t%d\n" % (cp, base, tone))

    distinct = len({b for _, b, _ in rows})
    print("wrote %s" % out_path)
    print("rows=%d distinct_syllables=%d" % (len(rows), distinct))


if __name__ == "__main__":
    sys.exit(main())
