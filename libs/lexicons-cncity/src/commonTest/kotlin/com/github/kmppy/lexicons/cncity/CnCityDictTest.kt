package com.github.kmppy.lexicons.cncity

import com.github.kmppy.Pinyin
import com.github.kmppy.PinyinCase
import com.github.kmppy.PinyinConfig
import com.github.kmppy.ToneStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class CnCityDictTest {

    @Test
    fun fixesPolyphoneInCityName() {
        val cfg = PinyinConfig(ToneStyle.NONE, PinyinCase.UPPERCASE, CnCityDict)
        assertEquals("CHONG QING", Pinyin.toPinyin("重庆", " ", cfg))
        assertEquals("WO ZAI SHANG HAI", Pinyin.toPinyin("我在上海", " ", cfg))
    }

    /** Lexicon hits follow the configured tone style, so a sentence never mixes styles. */
    @Test
    fun cityNamesKeepTones() {
        val mark = PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE, CnCityDict)
        val number = PinyinConfig(ToneStyle.TONE_NUMBER, PinyinCase.LOWERCASE, CnCityDict)
        val numberNoDict = PinyinConfig(ToneStyle.TONE_NUMBER, PinyinCase.LOWERCASE)

        assertEquals("chong2 qing4", Pinyin.toPinyin("重庆", " ", number))
        assertEquals("wǒ qù chóng qìng", Pinyin.toPinyin("我去重庆", " ", mark))
        assertEquals("wǒ zài shàng hǎi", Pinyin.toPinyin("我在上海", " ", mark))
        // 都 defaults to dōu in the table; 成都 needs dū.
        assertEquals("cheng2 dou1", Pinyin.toPinyin("成都", " ", numberNoDict))
        assertEquals("chéng dū", Pinyin.toPinyin("成都", " ", mark))
        // 单 defaults to dān in the table; 单县 needs shàn.
        assertEquals("dan1 xian4", Pinyin.toPinyin("单县", " ", numberNoDict))
        assertEquals("shàn xiàn", Pinyin.toPinyin("单县", " ", mark))
    }
}
