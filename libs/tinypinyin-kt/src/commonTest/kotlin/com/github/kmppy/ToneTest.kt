package com.github.kmppy

import kotlin.test.Test
import kotlin.test.assertEquals

class ToneTest {

    private val numberCfg = PinyinConfig(ToneStyle.TONE_NUMBER, PinyinCase.UPPERCASE)
    private val markCfg = PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)

    @Test
    fun toneNumberAppendsDigit() {
        assertEquals("ZHONG1", Pinyin.toPinyin('中', numberCfg))
        assertEquals("WEN2", Pinyin.toPinyin('文', numberCfg))
        assertEquals("GUO2", Pinyin.toPinyin('国', numberCfg))
        assertEquals("DE5", Pinyin.toPinyin('的', numberCfg)) // neutral tone -> 5
        assertEquals("GUO2 ZHONG1", Pinyin.toPinyin("国中", " ", numberCfg))
    }

    @Test
    fun toneMarkPlacementFollowsRules() {
        assertEquals("zhōng", Pinyin.toPinyin('中', markCfg))
        assertEquals("wén", Pinyin.toPinyin('文', markCfg))
        assertEquals("yī", Pinyin.toPinyin('一', markCfg))
        assertEquals("quán", Pinyin.toPinyin('全', markCfg)) // u carries mark after q
        assertEquals("xué", Pinyin.toPinyin('学', markCfg)) // e carries the mark
    }

    @Test
    fun umlautRenderingForNVowels() {
        assertEquals("nǚ", Pinyin.toPinyin('女', markCfg)) // nv3 -> nǚ
        assertEquals("lǜ", Pinyin.toPinyin('绿', markCfg)) // lv4 -> lǜ
    }

    @Test
    fun neutralToneHasNoMark() {
        assertEquals("de", Pinyin.toPinyin('的', markCfg))
    }

    @Test
    fun caseOptions() {
        val lower = PinyinConfig(ToneStyle.NONE, PinyinCase.LOWERCASE)
        assertEquals("zhong", Pinyin.toPinyin('中', lower))
    }
}
