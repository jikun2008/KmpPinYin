package com.github.kmppy

import com.github.kmppy.internal.PinyinTable
import com.github.kmppy.internal.ToneMark

/**
 * Fast, low-memory Chinese-to-Pinyin converter for Kotlin Multiplatform (pure
 * commonMain, zero platform I/O), modeled after TinyPinyin.
 *
 * Defaults reproduce TinyPinyin: no tone, uppercase output. Extra features here:
 * tone rendering ([ToneStyle]) and traditional/Ext-A coverage via the built-in table.
 *
 * Global settings live in [init]/[config]; per-call overloads also accept an explicit
 * [PinyinConfig]. The global [current] config is a plain reference (not synchronized):
 * initialize it once at startup, or use the explicit-config overloads for concurrency.
 */
object Pinyin {

    private var current: PinyinConfig = PinyinConfig.DEFAULT

    /** Replace the global configuration (aligns with `Pinyin.init`). */
    fun init(config: PinyinConfig) {
        current = config
    }

    /** Reset the global configuration back to TinyPinyin-compatible defaults. */
    fun reset() {
        current = PinyinConfig.DEFAULT
    }

    /** Fluent builder for the global configuration. */
    fun config(block: PinyinConfig.Builder.() -> Unit) {
        current = PinyinConfig.Builder().apply(block).build()
    }

    fun newConfig(): PinyinConfig.Builder = PinyinConfig.Builder()

    /** `true` when [c] is a Chinese character covered by the built-in table. */
    fun isChinese(c: Char): Boolean = PinyinTable.codeOf(c) != 0

    /** Single character: pinyin for Chinese characters, otherwise `c.toString()`. */
    fun toPinyin(c: Char): String = toPinyin(c, current)

    fun toPinyin(c: Char, config: PinyinConfig): String {
        val entry = entryOf(c) ?: return c.toString()
        return formatEntry(entry.first, entry.second, config)
    }

    /** Convert [text], inserting [separator] between every character's pinyin. */
    fun toPinyin(text: String, separator: String = " "): String =
        toPinyin(text, separator, current)

    fun toPinyin(text: String, separator: String, config: PinyinConfig): String {
        if (text.isEmpty()) return text
        val chars = text.toCharArray()
        val matcher = config.matcher
        val tokens = ArrayList<String>(chars.size + 1)
        var i = 0
        while (i < chars.size) {
            val m = matcher?.match(chars, i)
            if (m != null) {
                for (k in 0 until m.length) {
                    val literal = m.values.getOrNull(k)
                    val token = if (literal != null) {
                        formatDictValue(literal, config)
                    } else {
                        formatSingle(chars[i + k], config)
                    }
                    tokens.add(token)
                }
                i += m.length
            } else {
                tokens.add(formatSingle(chars[i], config))
                i++
            }
        }
        return tokens.joinToString(separator)
    }

    // ---- internals ----

    private fun entryOf(c: Char): Pair<String, Int>? {
        val code = PinyinTable.codeOf(c)
        if (code == 0) return null
        val syllable = PinyinTable.syllables[(code ushr 3) - 1]
        val tone = code and 0x7
        return syllable to tone
    }

    private fun formatSingle(c: Char, config: PinyinConfig): String {
        val entry = entryOf(c) ?: return c.toString()
        return formatEntry(entry.first, entry.second, config)
    }

    private fun formatEntry(syllable: String, tone: Int, config: PinyinConfig): String {
        val shaped = when (config.toneStyle) {
            ToneStyle.NONE -> syllable
            ToneStyle.TONE_NUMBER -> if (tone >= 1) syllable + tone.toString() else syllable
            ToneStyle.TONE_MARK -> ToneMark.mark(syllable, tone)
        }
        return applyCase(shaped, config.case)
    }

    /**
     * Renders one dictionary value. A trailing tone digit (`"chong2"`, `"CHONG2"`, `"de5"`)
     * is parsed into syllable + tone so lexicon hits honour the configured [ToneStyle]
     * instead of leaking a raw digit into the output. Anything that is not a real syllable
     * before the digit (`"XD2"`) stays literal, as do digit-less values, which keeps
     * TinyPinyin-style lexicons byte-for-byte compatible.
     */
    private fun formatDictValue(literal: String, config: PinyinConfig): String {
        val last = literal.lastOrNull()
        val tone = if (last != null && last in '1'..'5') last - '0' else 0
        if (tone == 0) return applyCase(literal, config.case)
        val syllable = literal.substring(0, literal.length - 1)
        if (!isKnownSyllable(syllable)) return applyCase(literal, config.case)
        return formatEntry(syllable, tone, config)
    }

    /**
     * `true` when [s] names a syllable that actually exists in the built-in table, so that
     * a trailing digit can be trusted as a tone. Case-insensitive, and `ü` is accepted
     * where the table writes `v`. Only built once, and only if some lexicon value carries
     * a tone digit.
     */
    private fun isKnownSyllable(s: String): Boolean {
        if (s.isEmpty()) return false
        val key = s.lowercase().replace('ü', 'v')
        if (key.length != s.length) return false
        return key in knownSyllables
    }

    private val knownSyllables: Set<String> by lazy { PinyinTable.syllables.toSet() }

    private fun applyCase(s: String, case: PinyinCase): String = when (case) {
        PinyinCase.UPPERCASE -> s.uppercase()
        PinyinCase.LOWERCASE -> s.lowercase()
        PinyinCase.ORIGINAL -> s
    }
}
