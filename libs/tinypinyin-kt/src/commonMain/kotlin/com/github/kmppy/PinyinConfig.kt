package com.github.kmppy

import com.github.kmppy.dict.PinyinDict
import com.github.kmppy.internal.DictMatcher

/**
 * Immutable conversion settings. Build one via [Builder] (or [Pinyin.config]) and pass
 * it to [Pinyin.init].
 *
 * Defaults reproduce TinyPinyin: no tone, uppercase, no dictionary.
 */
class PinyinConfig(
    val toneStyle: ToneStyle = ToneStyle.NONE,
    val case: PinyinCase = PinyinCase.UPPERCASE,
    val dict: PinyinDict? = null,
) {
    internal val matcher: DictMatcher? by lazy { dict?.let { DictMatcher(it.mapping()) } }

    class Builder {
        private var toneStyle: ToneStyle = ToneStyle.NONE
        private var case: PinyinCase = PinyinCase.UPPERCASE
        private var dict: PinyinDict? = null

        fun toneStyle(style: ToneStyle) = apply { this.toneStyle = style }
        fun case(pinyinCase: PinyinCase) = apply { this.case = pinyinCase }
        fun upperCase(enabled: Boolean) = apply {
            this.case = if (enabled) PinyinCase.UPPERCASE else PinyinCase.ORIGINAL
        }
        fun with(dict: PinyinDict) = apply { this.dict = dict }

        fun build(): PinyinConfig = PinyinConfig(toneStyle, case, dict)
    }

    companion object {
        val DEFAULT: PinyinConfig = Builder().build()
    }
}
