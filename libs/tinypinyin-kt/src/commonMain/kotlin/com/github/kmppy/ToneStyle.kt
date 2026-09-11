package com.github.kmppy

/**
 * How tone information is rendered in the output.
 *
 * The tone value for each character comes from the built-in table; user dictionaries
 * provide literal strings and are only affected by [PinyinCase].
 */
enum class ToneStyle {
    /** No tone, e.g. `ZHONG`. Matches TinyPinyin's default output. */
    NONE,

    /** Trailing tone number, e.g. `ZHONG1` (tone 5 = neutral). */
    TONE_NUMBER,

    /** Unicode tone diacritic on the main vowel, e.g. `ZHŌNG`. */
    TONE_MARK,
}

/** Letter case applied to generated pinyin. */
enum class PinyinCase {
    /** `ZHONG` (default, aligns with TinyPinyin). */
    UPPERCASE,

    /** `zhong`. */
    LOWERCASE,

    /** Keep the stored form (lowercase, tone marks preserved). */
    ORIGINAL,
}
