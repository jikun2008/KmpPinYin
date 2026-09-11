package com.github.kmppy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinyinTest {

    @Test
    fun singleCharDefaultsToUppercaseNoTone() {
        Pinyin.reset()
        assertEquals("ZHONG", Pinyin.toPinyin('中'))
        assertEquals("WEN", Pinyin.toPinyin('文'))
        assertEquals("GUO", Pinyin.toPinyin('国'))
    }

    @Test
    fun nonChineseCharReturnedAsIs() {
        assertEquals("A", Pinyin.toPinyin('A'))
        assertEquals("1", Pinyin.toPinyin('1'))
        assertEquals(" ", Pinyin.toPinyin(' '))
        assertEquals("あ", Pinyin.toPinyin('あ')) // hiragana is not Chinese
    }

    @Test
    fun stringConversionInsertsSeparator() {
        Pinyin.reset()
        assertEquals("ZHONG WEN", Pinyin.toPinyin("中文"))
        assertEquals("ZHONGWEN", Pinyin.toPinyin("中文", ""))
        assertEquals("ZHONG-WEN", Pinyin.toPinyin("中文", "-"))
        // mixed: non-Chinese passes through per character, one token per character
        assertEquals("ZHONG 2 WEN", Pinyin.toPinyin("中2文", " "))
    }

    @Test
    fun isChineseCoversRangesAndBoundaries() {
        assertTrue(Pinyin.isChinese('一')) // U+4E00
        assertTrue(Pinyin.isChinese('龥')) // U+9FA5 area, within main block
        assertTrue(Pinyin.isChinese('㐀')) // U+3400 Ext-A
        assertFalse(Pinyin.isChinese('A'))
        assertFalse(Pinyin.isChinese('あ'))
        assertFalse(Pinyin.isChinese('가')) // Hangul
    }

    @Test
    fun emptyStringStaysEmpty() {
        assertEquals("", Pinyin.toPinyin(""))
    }
}
