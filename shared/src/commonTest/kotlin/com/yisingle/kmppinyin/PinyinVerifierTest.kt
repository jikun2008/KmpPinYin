package com.yisingle.kmppinyin

import com.github.kmppy.Pinyin
import com.github.kmppy.PinyinCase
import com.github.kmppy.PinyinConfig
import com.github.kmppy.ToneStyle
import com.github.kmppy.lexicons.cncity.CnCityDict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 把 [PinyinVerifier] 的用例在命令行侧也跑一遍：
 * 界面上的按钮是人工验证，这里是可回归的自动化验证。
 */
class PinyinVerifierTest {

    @Test
    fun goldensMatchLibraryOutput() {
        val failed = PinyinVerifier.goldens().filterNot { it.passed }
        assertTrue(
            failed.isEmpty(),
            "golden 用例失败：\n" + failed.joinToString("\n") {
                "  ${it.title}\n    输入=${it.input}\n    期望=${it.expected}\n    实际=${it.actual}"
            },
        )
    }

    @Test
    fun invariantsHold() {
        val failed = PinyinVerifier.invariants().filterNot { it.passed }
        assertTrue(
            failed.isEmpty(),
            "不变量失败：" + failed.joinToString("; ") { "${it.title} -> ${it.detail}" },
        )
    }

    @Test
    fun fullVerifyPassesAndProducesTiming() {
        val report = PinyinVerifier.verify(iterations = 20)
        assertTrue(report.allPassed, "校验未全部通过：\n" + report.render())
        val timing = assertNotNull(report.timing, "verify() 应携带计时结果")
        assertTrue(timing.average.inWholeMicroseconds > 0L, "平均耗时应大于 0")
        assertTrue(timing.charsPerSecond > 0, "每秒字数应大于 0")
    }

    /** 启动时自动跑的那一轮：只有正确性，不做计时。 */
    @Test
    fun checkOnlySkipsTiming() {
        val report = PinyinVerifier.checkOnly()
        assertTrue(report.allPassed, "自动校验未全部通过：\n" + report.render())
        assertEquals(null, report.timing, "checkOnly() 不应计时")
        assertEquals(PinyinVerifier.goldens().size + PinyinVerifier.sampleGoldens().size, report.goldens.size)
    }

    /** 内置中文样本：每条比对无调大写 / 数字调小写 / 符号调小写三种配置。 */
    @Test
    fun builtInSamplesMatchExpected() {
        assertEquals(27, PinyinVerifier.SAMPLES.size, "样本数量变了，请同步修正断言")
        val goldens = PinyinVerifier.sampleGoldens()
        assertEquals(PinyinVerifier.SAMPLES.size * 3, goldens.size)
        val failed = goldens.filterNot { it.passed }
        assertTrue(
            failed.isEmpty(),
            "内置样本失败（${failed.size}/${goldens.size}）：\n" + failed.joinToString("\n") {
                "  ${it.title}\n    输入=${it.input}\n    期望=${it.expected}\n    实际=${it.actual}"
            },
        )
    }

    /** 样本的期望值必须是真转换出来的，不能是“查不到而原样透传”凑成的。 */
    @Test
    fun everySampleChineseCharIsActuallyConverted() {
        val suspicious = PinyinVerifier.SAMPLES.filterNot {
            PinyinVerifier.checkText(it.text, iterations = 1).passed
        }
        assertTrue(
            suspicious.isEmpty(),
            "这些样本里有汉字未转出拼音：" + suspicious.joinToString { it.title },
        )
    }

    @Test
    fun runSamplesPassesAndTimesEverySample() {
        val results = PinyinVerifier.runSamples(iterations = 5)
        assertEquals(PinyinVerifier.SAMPLES.size, results.size)
        val bad = results.filterNot { it.passed }
        assertTrue(bad.isEmpty(), "样本计时运行失败：\n" + bad.joinToString("\n") { it.render() })
        assertTrue(results.all { it.timing.average.inWholeMicroseconds >= 0L })
    }

    /** 打印逐样本的耗时表，便于直接看不同长度文本的性能数量级。 */
    @Test
    fun printSampleTimings() {
        println(PinyinVerifier.runSamples(iterations = 20).joinToString("\n") { it.render() })
    }

    /** 按钮“校验并计时当前输入”背后的逻辑。 */
    @Test
    fun checkTextOnArbitraryInput() {
        val long = PinyinVerifier.checkText(PinyinVerifier.LONG_SAMPLE, iterations = 20)
        assertTrue(long.passed, "长文本应全部转出：" + long.render())
        assertTrue(long.timing.average.inWholeMicroseconds > 0L)
        assertEquals(288, long.text.length)
        assertEquals(240, long.chineseChars)

        val mixed = PinyinVerifier.checkText("中文123abc你好", iterations = 10)
        assertTrue(mixed.passed, mixed.render())
        assertEquals(4, mixed.chineseChars)
    }

    /** 报告必须把测过的中文逐条列出来，否则人工无法复核。 */
    @Test
    fun reportListsEveryTestedChinese() {
        val report = PinyinVerifier.checkOnly()
        val full = report.render()
        val missing = PinyinVerifier.SAMPLES.filter { !full.contains("中文：${it.text}") }
        assertTrue(missing.isEmpty(), "报告里没列出这些中文：" + missing.joinToString { it.title })
        assertTrue(full.contains("中文：我去重庆"), "API 用例的中文也要列出")
        if (report.allPassed) {
            assertTrue(
                !report.render(onlyFailed = true).contains("中文："),
                "只看失败时不应列出通过项",
            )
        }
    }

    /** 词典命中后仍要跟随声调风格：重庆 = chóng qìng，不是无调的 chong qing。 */
    @Test
    fun cityDictKeepsToneStyle() {
        fun cfg(style: ToneStyle, case: PinyinCase) = PinyinConfig(style, case, CnCityDict)

        assertEquals("WO QU CHONG QING", Pinyin.toPinyin("我去重庆", " ", cfg(ToneStyle.NONE, PinyinCase.UPPERCASE)))
        assertEquals("wo3 qu4 chong2 qing4", Pinyin.toPinyin("我去重庆", " ", cfg(ToneStyle.TONE_NUMBER, PinyinCase.LOWERCASE)))
        assertEquals("wǒ qù chóng qìng", Pinyin.toPinyin("我去重庆", " ", cfg(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)))
        assertEquals("wǒ zài shàng hǎi", Pinyin.toPinyin("我在上海", " ", cfg(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)))
        // 成都 / 单县 是多音字修正，无词典时分别是 chéng dōu / dān xiàn。
        assertEquals("chéng dū", Pinyin.toPinyin("成都", " ", cfg(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)))
        assertEquals("shàn xiàn", Pinyin.toPinyin("单县", " ", cfg(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)))
        // 整句里不应出现“有的字带调、有的字裸数字”的混风格输出。
        val sentence = Pinyin.toPinyin("我去重庆看成都", " ", cfg(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE))
        assertEquals("wǒ qù chóng qìng kàn chéng dū", sentence)
        assertTrue(
            sentence.none { it in '1'..'5' },
            "符号调输出里残留了声调数字：$sentence",
        )
    }

    /** 打印一轮真实数据（耗时/吞吐），便于在终端里直接看数量级。 */
    @Test
    fun printReport() {
        println(PinyinVerifier.verify(iterations = 50).render())
    }
}
