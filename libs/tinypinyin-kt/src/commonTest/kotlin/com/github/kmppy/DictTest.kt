package com.github.kmppy

import com.github.kmppy.dict.PinyinMapDict
import kotlin.test.Test
import kotlin.test.assertEquals

class DictTest {

    @Test
    fun dictResolvesPolyphonicCharacter() {
        val cfg = PinyinConfig(
            toneStyle = ToneStyle.NONE,
            case = PinyinCase.UPPERCASE,
            dict = PinyinMapDict(mapOf("重庆" to arrayOf("CHONG", "QING"))),
        )
        // With dictionary: 重 uses the CHONG reading from the phrase.
        assertEquals("CHONG QING", Pinyin.toPinyin("重庆", " ", cfg))
        // Without dictionary: 重 falls back to its most common single-char reading.
        assertEquals("ZHONG QING", Pinyin.toPinyin("重庆", " ", PinyinConfig.DEFAULT))
    }

    @Test
    fun longestMatchInsideSentence() {
        val cfg = PinyinConfig(
            toneStyle = ToneStyle.NONE,
            case = PinyinCase.UPPERCASE,
            dict = PinyinMapDict(mapOf("重庆" to arrayOf("CHONG", "QING"))),
        )
        assertEquals("WO QU CHONG QING", Pinyin.toPinyin("我去重庆", " ", cfg))
    }

    @Test
    fun globalConfigWithDict() {
        Pinyin.config {
            with(PinyinMapDict(mapOf("重庆" to arrayOf("CHONG", "QING"))))
        }
        assertEquals("CHONG QING", Pinyin.toPinyin("重庆"))
        Pinyin.reset()
    }

    @Test
    fun dictValueShorterThanWordFallsBackPerChar() {
        val cfg = PinyinConfig(
            toneStyle = ToneStyle.NONE,
            case = PinyinCase.UPPERCASE,
            dict = PinyinMapDict(mapOf("中国人" to arrayOf("ZHONG1"))), // intentionally short
        )
        // First char uses the dictionary value (its tone digit is consumed, not printed),
        // the rest fall back to the table.
        assertEquals("ZHONG GUO REN", Pinyin.toPinyin("中国人", " ", cfg))
    }

    /** A tone digit on a dictionary value makes the hit follow the configured tone style. */
    @Test
    fun dictToneDigitFollowsToneStyle() {
        val dict = PinyinMapDict(mapOf("重庆" to arrayOf("CHONG2", "QING4")))
        assertEquals(
            "CHONG QING",
            Pinyin.toPinyin("重庆", " ", PinyinConfig(ToneStyle.NONE, PinyinCase.UPPERCASE, dict)),
        )
        assertEquals(
            "chong2 qing4",
            Pinyin.toPinyin("重庆", " ", PinyinConfig(ToneStyle.TONE_NUMBER, PinyinCase.LOWERCASE, dict)),
        )
        assertEquals(
            "chóng qìng",
            Pinyin.toPinyin("重庆", " ", PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE, dict)),
        )
        // ORIGINAL case keeps the author's casing and still gets a mark.
        assertEquals(
            "CHÓNG QÌNG",
            Pinyin.toPinyin("重庆", " ", PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.ORIGINAL, dict)),
        )
    }

    /** Digit-less values stay verbatim: upstream lexicons must not change behaviour. */
    @Test
    fun dictValueWithoutToneDigitStaysLiteral() {
        val dict = PinyinMapDict(mapOf("重庆" to arrayOf("CHONG", "QING")))
        assertEquals(
            "chong qing",
            Pinyin.toPinyin("重庆", " ", PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE, dict)),
        )
        // A toneless hit sits next to marked neighbours, which is exactly why the digit exists.
        assertEquals(
            "wǒ qù chong qing",
            Pinyin.toPinyin("我去重庆", " ", PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE, dict)),
        )
    }

    /** The non-pinyin-looking literal "XD2" must not be mistaken for a tone digit. */
    @Test
    fun dictLiteralEndingInDigitButNotPinyinStaysVerbatim() {
        val dict = PinyinMapDict(mapOf("中国人" to arrayOf("XD2")))
        assertEquals(
            "XD2 guó rén",
            Pinyin.toPinyin("中国人", " ", PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.ORIGINAL, dict)),
        )
    }

    /** ü readings can be written with `v` in a lexicon and still get marked. */
    @Test
    fun dictValueWithVMaterializesUmlaut() {
        val dict = PinyinMapDict(mapOf("绿" to arrayOf("LV4")))
        assertEquals(
            "lǜ",
            Pinyin.toPinyin("绿", " ", PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE, dict)),
        )
    }
}
