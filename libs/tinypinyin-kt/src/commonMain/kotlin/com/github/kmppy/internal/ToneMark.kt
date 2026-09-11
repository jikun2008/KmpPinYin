package com.github.kmppy.internal

/**
 * Renders a tone-less ASCII syllable plus a tone number (1..5) into a syllable with a
 * Unicode tone diacritic (e.g. `zhong` + 1 -> `zhōng`).
 *
 * The stored syllables follow the pinyin-data convention where `v` represents `ü` after
 * n/l (`nv`, `lv`, `nve`, `lve`); after j/q/x/y the umlaut is written as plain `u`
 * (`ju`, `quan`, ...) and the mark simply goes on that `u`.
 *
 * Vowel-selection rule (standard pinyin):
 *  1. `a` or `e` always carries the mark;
 *  2. otherwise in `ou` the `o` carries it;
 *  3. otherwise the last vowel carries it (handles `iu`, `ui`, single vowels, `ü`).
 *
 * The lookup is case-insensitive: the built-in table only stores lowercase syllables,
 * but user-supplied lexicons often write them uppercase (`"CHONG2"`), and a
 * `PinyinCase.ORIGINAL` request should still get a marked syllable (`CHÓNG`).
 */
internal object ToneMark {

    private val MARKS = mapOf(
        'a' to charArrayOf('ā', 'á', 'ǎ', 'à'),
        'e' to charArrayOf('ē', 'é', 'ě', 'è'),
        'i' to charArrayOf('ī', 'í', 'ǐ', 'ì'),
        'o' to charArrayOf('ō', 'ó', 'ǒ', 'ò'),
        'u' to charArrayOf('ū', 'ú', 'ǔ', 'ù'),
        'ü' to charArrayOf('ǖ', 'ǘ', 'ǚ', 'ǜ'),
    )

    fun mark(syllable: String, tone: Int): String {
        // Neutral tone (5) or unknown: no diacritic, just materialize ü.
        val plain = syllable.replace('v', 'ü').replace('V', 'Ü')
        if (tone !in 1..4) return plain

        val chars = plain.toCharArray()
        val target = findMarkIndex(chars) ?: return plain
        val table = MARKS[chars[target].lowercaseChar()] ?: return plain
        var marked = table[tone - 1]
        if (chars[target].isUpperCase()) marked = marked.uppercaseChar()
        chars[target] = marked
        // concatToString() 而非 String(chars)：后者在 JS/Wasm 目标上已被标为错误级废弃。
        return chars.concatToString()
    }

    private fun isVowel(c: Char): Boolean = when (c.lowercaseChar()) {
        'a', 'e', 'i', 'o', 'u', 'ü' -> true
        else -> false
    }

    /** Works on a lowercase view of [chars]; indexes line up because both have the same length. */
    private fun findMarkIndex(chars: CharArray): Int? {
        val lower = chars.concatToString().lowercase()
        // Rare letters grow when lowercased; skip marking rather than mis-align the index.
        if (lower.length != chars.size) return null
        var idx = lower.indexOf('a')
        if (idx < 0) idx = lower.indexOf('e')
        if (idx < 0) idx = lower.indexOf("ou")
        if (idx >= 0) return idx
        var last = -1
        for (i in lower.indices) {
            if (isVowel(lower[i])) last = i
        }
        return if (last >= 0) last else null
    }
}
