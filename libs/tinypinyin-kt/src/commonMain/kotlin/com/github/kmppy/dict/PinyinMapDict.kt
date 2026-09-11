package com.github.kmppy.dict

/**
 * Convenience [PinyinDict] backed by a [Map]. Mirrors TinyPinyin's anonymous
 * `PinyinMapDict` usage; append a tone digit to keep tones on matched words:
 *
 * ```
 * Pinyin.config {
 *     with(PinyinMapDict(mapOf("重庆" to arrayOf("CHONG2", "QING4"))))
 * }
 * ```
 */
class PinyinMapDict(
    private val map: Map<String, Array<String>>,
) : PinyinDict {
    constructor(vararg entries: Pair<String, Array<String>>) : this(mapOf(*entries))

    override fun mapping(): Map<String, Array<String>> = map
}
