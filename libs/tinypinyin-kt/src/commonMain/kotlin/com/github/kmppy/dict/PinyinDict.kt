package com.github.kmppy.dict

/**
 * A user-supplied lexicon used to resolve polyphonic characters (多音字).
 *
 * Each key is a Chinese word/phrase; the value is the per-character pinyin for that
 * word, e.g. `"重庆" -> ["CHONG2", "QING4"]`. When the string converter matches a key it
 * uses these values instead of the single-character default table.
 *
 * A value may carry a trailing tone digit (`1`..`5`, `5` = neutral tone). It is then
 * rendered through the configured tone style: `CHONG2` becomes `CHONG` (no tone),
 * `chong2` (tone number) or `chóng` (tone mark). Digit-less values stay literal and are
 * emitted without any tone, which is what upstream TinyPinyin lexicons look like.
 */
interface PinyinDict {
    fun mapping(): Map<String, Array<String>>
}
