package com.github.kmppy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyTest {

    @Test
    fun neverThrowsAndOutputNonEmptyAcrossRanges() {
        val cps = intArrayOf(0x0, 0x41, 0x3042, 0x3400, 0x4E00, 0x4E2D, 0x6587, 0x9FA5, 0x9FFF, 0xAC00, 0xFFFD)
        for (cp in cps) {
            val c = cp.toChar()
            val single = Pinyin.toPinyin(c, PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.ORIGINAL))
            assertTrue(single.isNotEmpty(), "empty output for U+$cp")
        }
    }

    @Test
    fun tokenCountMatchesSeparators() {
        val text = "中文ABC你"
        val out = Pinyin.toPinyin(text, "|")
        // Every character (Chinese or not) yields exactly one token.
        assertEquals(text.length, out.split("|").size)
    }

    @Test
    fun chineseTableIsDenseInMainBlock() {
        var hit = 0
        for (cp in 0x4E00..0x9FA5) {
            if (Pinyin.isChinese(cp.toChar())) hit++
        }
        // The common block should be almost fully covered.
        assertTrue(hit > 20000, "expected dense coverage, got $hit")
    }
}
