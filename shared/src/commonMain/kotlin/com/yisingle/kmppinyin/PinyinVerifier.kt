package com.yisingle.kmppinyin

import com.github.kmppy.Pinyin
import com.github.kmppy.PinyinCase
import com.github.kmppy.PinyinConfig
import com.github.kmppy.ToneStyle
import com.github.kmppy.dict.PinyinMapDict
import com.github.kmppy.lexicons.cncity.CnCityDict
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * `:libs:tinypinyin-kt` 的运行期验证与计时工具，供 [App] 上的按钮调用。
 *
 * - [goldens]：逐条比对期望值。期望值不是手写的，而是用库的数据源
 * `libs/generator/src/main/resources/pinyin-dict.tsv` 按同一套编码/声调规则算出来的，
 * 因此一旦 Kotlin 实现与数据不一致就会报 FAIL。
 * - [invariants]：对长文本做结构性检查（token 数、逐字结果、幂等、Ext-A 覆盖等）。
 * - [timing]：用 [TimeSource.Monotonic] 统计冷启动/平均/最快/最慢耗时与吞吐。
 */
object PinyinVerifier {

    /** 静夜思（含中文标点，标点应原样透传）。 */
    const val POEM_JINGYE = "床前明月光，疑是地上霜。举头望明月，低头思故乡。"

    /** 春晓。 */
    const val POEM_CHUNXIAO = "春眠不觉晓，处处闻啼鸟。夜来风雨声，花落知多少。"

    /** 性能测试用的长文本（288 个字符，含中文标点）。 */
    val LONG_SAMPLE: String = (POEM_JINGYE + POEM_CHUNXIAO).repeat(6)

    private val CFG_DEFAULT: PinyinConfig = PinyinConfig.DEFAULT
    private val CFG_NUMBER_LOWER: PinyinConfig = PinyinConfig(ToneStyle.TONE_NUMBER, PinyinCase.LOWERCASE)
    private val CFG_NUMBER_UPPER: PinyinConfig = PinyinConfig(ToneStyle.TONE_NUMBER, PinyinCase.UPPERCASE)
    private val CFG_MARK_LOWER: PinyinConfig = PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE)
    private val CFG_MARK_ORIGINAL: PinyinConfig = PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.ORIGINAL)
    private val CFG_CITY: PinyinConfig = PinyinConfig(ToneStyle.NONE, PinyinCase.UPPERCASE, CnCityDict)
    private val CFG_CITY_NUMBER: PinyinConfig = PinyinConfig(ToneStyle.TONE_NUMBER, PinyinCase.LOWERCASE, CnCityDict)
    private val CFG_CITY_MARK: PinyinConfig = PinyinConfig(ToneStyle.TONE_MARK, PinyinCase.LOWERCASE, CnCityDict)
    private val CFG_SHORT_VALUES: PinyinConfig = PinyinConfig(
        ToneStyle.NONE,
        PinyinCase.UPPERCASE,
        PinyinMapDict("中国人" to arrayOf("ZHONG1")),
    )

    /** 一次校验的完整结果。[timing] 为 null 表示只做了正确性比对（启动时自动跑的那一轮）。 */
    data class Report(
        val goldens: List<Golden>,
        val invariants: List<Check>,
        val timing: Timing? = null,
        val extras: List<Pair<String, String>> = emptyList(),
    ) {
        val total: Int get() = goldens.size + invariants.size
        val passed: Int get() = goldens.count { it.passed } + invariants.count { it.passed }
        val failed: Int get() = total - passed
        val allPassed: Boolean get() = failed == 0

        /**
         * [onlyFailed] 为 false 时逐条列出每一个期望值比对（带中文输入），这样“到底测了哪些
         * 中文”能直接在报告里看到；为 true 时只保留失败项。
         */
        fun render(onlyFailed: Boolean = false): String = buildString {
            appendLine("断言：$passed / $total 通过" + if (allPassed) "  PASS" else "  FAIL")
            appendLine()
            appendLine("【期望值比对（API 用例 + 内置样本）】${goldens.count { it.passed }}/${goldens.size}")
            goldens.forEach { g ->
                if (onlyFailed && g.passed) return@forEach
                appendLine("  ${if (g.passed) "✓" else "✗"} ${g.title}")
                appendLine("      中文：${g.input}")
                appendLine("      拼音：${g.actual}")
                if (!g.passed) appendLine("      期望：${g.expected}")
            }
            if (goldens.all { it.passed }) appendLine("  全部通过")
            appendLine()
            appendLine("【结构性不变量】${invariants.count { it.passed }}/${invariants.size}")
            invariants.forEach { c ->
                appendLine(if (c.passed) "  ✓ ${c.title}" else "  ✗ ${c.title} -> ${c.detail}")
            }
            appendLine()
            val t = timing
            if (t == null) {
                appendLine("【转换计时】本轮未计时；点「运行全量校验 + 计时」或「逐样本校验 + 计时」查看")
            } else {
                appendLine("【转换计时】${t.sampleChars} 字 / ${t.iterations} 次")
                appendLine("  首次调用   ${t.cold.millisText()}（含拼音表惰性解码，可能已被界面预热）")
                appendLine("  平均       ${t.average.millisText()}")
                appendLine("  最快/最慢  ${t.fastest.millisText()} / ${t.slowest.millisText()}")
                appendLine("  单字均耗   ${t.microsPerChar} µs/字")
                appendLine("  吞吐量     ${t.charsPerSecond} 字/秒")
                extras.forEach { (k, v) -> appendLine("  $k  $v") }
            }
        }
    }

    /** 一条期望值比对结果。 */
    data class Golden(val title: String, val input: String, val expected: String, val actual: String) {
        val passed: Boolean get() = expected == actual
    }

    /** 一条结构性检查结果。 */
    data class Check(val title: String, val passed: Boolean, val detail: String = "")

    /** 一组转换耗时统计。 */
    data class Timing(
        val sampleChars: Int,
        val iterations: Int,
        val cold: Duration,
        val average: Duration,
        val fastest: Duration,
        val slowest: Duration,
    ) {
        val microsPerChar: Double get() = round2(average.toDouble(DurationUnit.MICROSECONDS) / maxOf(sampleChars, 1))
        val charsPerSecond: Long get() {
            val seconds = average.toDouble(DurationUnit.SECONDS)
            return if (seconds <= 0.0) 0L else (sampleChars / seconds).roundToLong()
        }

        /** 单行摘要，供外部（如只对当前输入计时）直接展示。 */
        fun summary(): String =
            "$sampleChars 字 × $iterations 次：首次 ${cold.millisText()}，平均 ${average.millisText()}，" +
                "最快 ${fastest.millisText()}，最慢 ${slowest.millisText()}，$microsPerChar µs/字，$charsPerSecond 字/秒"
    }

    /** 针对任意文本的校验结果：逐字正确性 + 转换耗时。 */
    data class TextCheck(
        val text: String,
        val chineseChars: Int,
        val unmapped: List<Char>,
        val timing: Timing,
    ) {
        val passed: Boolean get() = unmapped.isEmpty()

        fun render(): String = buildString {
            appendLine("文本（${text.length} 字符，汉字 $chineseChars 个）：${text.take(24)}" + if (text.length > 24) "…" else "")
            appendLine(if (passed) "  ✓ 每个汉字都转出了拼音" else "  ✗ 未转出拼音：${unmapped.joinToString("")}")
            appendLine("  " + timing.summary())
        }
    }

    /** golden 用例：输入 + 分隔符 + 配置 + 期望值（均由 pinyin-dict.tsv 推得）。 */
    private data class Spec(
        val title: String,
        val input: String,
        val separator: String,
        val config: PinyinConfig,
        val expected: String,
    )

    private val SPECS: List<Spec> = listOf(
        Spec("单字 中", "中", " ", CFG_DEFAULT, "ZHONG"),
        Spec("词组 中文", "中文", " ", CFG_DEFAULT, "ZHONG WEN"),
        Spec("无分隔符", "中文", "", CFG_DEFAULT, "ZHONGWEN"),
        Spec("非中文逐字透传", "中2文", " ", CFG_DEFAULT, "ZHONG 2 WEN"),
        Spec("中英混排", "中文ABC你", "|", CFG_DEFAULT, "ZHONG|WEN|A|B|C|NI"),
        Spec(
            "静夜思 无调大写", POEM_JINGYE, " ", CFG_DEFAULT,
            "CHUANG QIAN MING YUE GUANG ， YI SHI DI SHANG SHUANG 。 JU TOU WANG MING YUE ， DI TOU SI GU XIANG 。",
        ),
        Spec(
            "静夜思 数字调小写", POEM_JINGYE, " ", CFG_NUMBER_LOWER,
            "chuang2 qian2 ming2 yue4 guang1 ， yi2 shi4 di4 shang4 shuang1 。 ju3 tou2 wang4 ming2 yue4 ， di1 tou2 si1 gu4 xiang1 。",
        ),
        Spec(
            "静夜思 符号调小写", POEM_JINGYE, " ", CFG_MARK_LOWER,
            "chuáng qián míng yuè guāng ， yí shì dì shàng shuāng 。 jǔ tóu wàng míng yuè ， dī tóu sī gù xiāng 。",
        ),
        Spec(
            "春晓 无调大写", POEM_CHUNXIAO, " ", CFG_DEFAULT,
            "CHUN MIAN BU JUE XIAO ， CHU CHU WEN TI NIAO 。 YE LAI FENG YU SHENG ， HUA LUO ZHI DUO SHAO 。",
        ),
        Spec(
            "春晓 符号调小写", POEM_CHUNXIAO, " ", CFG_MARK_LOWER,
            "chūn mián bù jué xiǎo ， chù chù wén tí niǎo 。 yè lái fēng yǔ shēng ， huā luò zhī duō shǎo 。",
        ),
        Spec("你好世界 符号调", "你好世界", " ", CFG_MARK_LOWER, "nǐ hǎo shì jiè"),
        Spec("你好世界 数字调", "你好世界", " ", CFG_NUMBER_UPPER, "NI3 HAO3 SHI4 JIE4"),
        Spec("符号调 ORIGINAL 不改大小写", "中文", " ", CFG_MARK_ORIGINAL, "zhōng wén"),
        Spec("多音字 重庆 无词典", "重庆", " ", CFG_DEFAULT, "ZHONG QING"),
        Spec("多音字 重庆 城市词典", "我去重庆", " ", CFG_CITY, "WO QU CHONG QING"),
        Spec("词典 上海", "我在上海", " ", CFG_CITY, "WO ZAI SHANG HAI"),
        Spec("词典 西安/南京", "从西安到南京", " ", CFG_CITY, "CONG XI AN DAO NAN JING"),
        // 词典值带声调数字，所以命中的词也要跟随 ToneStyle（见 Pinyin.formatDictValue）。
        // 下面每个期望值都先在 libs/generator/tools/validate_runtime_port.py 里独立算过一遍。
        Spec("词典+数字调 重庆", "我去重庆", " ", CFG_CITY_NUMBER, "wo3 qu4 chong2 qing4"),
        Spec("词典+符号调 重庆", "我去重庆", " ", CFG_CITY_MARK, "wǒ qù chóng qìng"),
        Spec("词典+符号调 上海", "我在上海", " ", CFG_CITY_MARK, "wǒ zài shàng hǎi"),
        Spec("词典+符号调 成都（都 dū）", "成都", " ", CFG_CITY_MARK, "chéng dū"),
        Spec("无词典+符号调 成都（默认 dōu）", "成都", " ", CFG_MARK_LOWER, "chéng dōu"),
        Spec("词典+符号调 单县（单 shàn）", "单县", " ", CFG_CITY_MARK, "shàn xiàn"),
        // 无调风格下尾部的声调数字会被消耗掉，不会漏进输出。
        Spec("词典值短于词回退", "中国人", " ", CFG_SHORT_VALUES, "ZHONG GUO REN"),
        Spec("Ext-A 生僻字", "㐀䶒", " ", CFG_DEFAULT, "QIU QI"),
    )

    fun goldens(): List<Golden> = SPECS.map { spec ->
        Golden(
            title = spec.title,
            input = spec.input,
            expected = spec.expected,
            actual = Pinyin.toPinyin(spec.input, spec.separator, spec.config),
        )
    }

    /**
     * 内置中文样本：文本 + 三种配置下的期望输出（无调大写 / 数字调小写 / 符号调小写）。
     *
     * 期望值由一份独立脚本直接从数据源
     * `libs/generator/src/main/resources/pinyin-dict.tsv` 算出，与 Kotlin 实现不共享代码，
     * 所以这里能反推出“实现与数据不一致”。覆盖：常用词 / 成语 / 古诗 / 人名 / 地名 /
     * 多音字 / ü 韵母 / 轻声（tone 5）/ 儿化 / 繁体 / 生僻字 / Ext-A / 中英数混排 / 中文标点。
     */
    data class Sample(
        val title: String,
        val text: String,
        val plain: String,
        val numbered: String,
        val marked: String,
    )

    val SAMPLES: List<Sample> = listOf(
        Sample("常用 你好", "你好",
            "NI HAO",
            "ni3 hao3",
            "nǐ hǎo"),
        Sample("常用 中华人民共和国", "中华人民共和国",
            "ZHONG HUA REN MIN GONG HE GUO",
            "zhong1 hua2 ren2 min2 gong4 he2 guo2",
            "zhōng huá rén mín gòng hé guó"),
        Sample("成语 掩耳盗铃", "掩耳盗铃",
            "YAN ER DAO LING",
            "yan3 er3 dao4 ling2",
            "yǎn ěr dào líng"),
        Sample("成语 春暖花开", "春暖花开",
            "CHUN NUAN HUA KAI",
            "chun1 nuan3 hua1 kai1",
            "chūn nuǎn huā kāi"),
        Sample("古诗 登鹳雀楼", "白日依山尽，黄河入海流。欲穷千里目，更上一层楼。",
            "BAI RI YI SHAN JIN ， HUANG HE RU HAI LIU 。 YU QIONG QIAN LI MU ， GENG SHANG YI CENG LOU 。",
            "bai2 ri4 yi1 shan1 jin3 ， huang2 he2 ru4 hai3 liu2 。 yu4 qiong2 qian1 li3 mu4 ， geng4 shang4 yi1 ceng2 lou2 。",
            "bái rì yī shān jǐn ， huáng hé rù hǎi liú 。 yù qióng qiān lǐ mù ， gèng shàng yī céng lóu 。"),
        Sample("古诗 念奴娇句", "大江东去，浪淘尽，千古风流人物。",
            "DA JIANG DONG QU ， LANG TAO JIN ， QIAN GU FENG LIU REN WU 。",
            "da4 jiang1 dong1 qu4 ， lang4 tao2 jin3 ， qian1 gu3 feng1 liu2 ren2 wu4 。",
            "dà jiāng dōng qù ， làng táo jǐn ， qiān gǔ fēng liú rén wù 。"),
        Sample("人名 张三丰武当", "张三丰在武当山练太极",
            "ZHANG SAN FENG ZAI WU DANG SHAN LIAN TAI JI",
            "zhang1 san1 feng1 zai4 wu3 dang1 shan1 lian4 tai4 ji2",
            "zhāng sān fēng zài wǔ dāng shān liàn tài jí"),
        Sample("地名 内蒙古呼和浩特", "内蒙古呼和浩特",
            "NEI MENG GU HU HE HAO TE",
            "nei4 meng2 gu3 hu1 he2 hao4 te4",
            "nèi méng gǔ hū hé hào tè"),
        Sample("地名 杭州西湖断桥", "杭州西湖断桥",
            "HANG ZHOU XI HU DUAN QIAO",
            "hang2 zhou1 xi1 hu2 duan4 qiao2",
            "háng zhōu xī hú duàn qiáo"),
        Sample("多音字 银行（表内默认音）", "银行",
            "YIN XING",
            "yin2 xing2",
            "yín xíng"),
        Sample("多音字 行人", "行人",
            "XING REN",
            "xing2 ren2",
            "xíng rén"),
        Sample("多音字 重量", "重量",
            "ZHONG LIANG",
            "zhong4 liang4",
            "zhòng liàng"),
        Sample("多音字 重庆（无词典）", "重庆",
            "ZHONG QING",
            "zhong4 qing4",
            "zhòng qìng"),
        Sample("ü 韵母 女人绿色", "女人绿色",
            "NV REN LV SE",
            "nv3 ren2 lv4 se4",
            "nǚ rén lǜ sè"),
        Sample("轻声 妈妈笑了（le 为 tone 5）", "妈妈笑了",
            "MA MA XIAO LE",
            "ma1 ma1 xiao4 le5",
            "mā mā xiào le"),
        Sample("儿化 花儿", "花儿",
            "HUA ER",
            "hua1 er2",
            "huā ér"),
        Sample("繁体 中國聲調", "中國聲調",
            "ZHONG GUO SHENG DIAO",
            "zhong1 guo2 sheng1 diao4",
            "zhōng guó shēng diào"),
        Sample("生僻 饕餮貔貅", "饕餮貔貅",
            "TAO TIE PI XIU",
            "tao1 tie4 pi2 xiu1",
            "tāo tiè pí xiū"),
        Sample("生僻 龘靐齉爨", "龘靐齉爨",
            "DA BING NANG CUAN",
            "da2 bing4 nang4 cuan4",
            "dá bìng nàng cuàn"),
        Sample("Ext-A 生僻字", "㐀䶒",
            "QIU QI",
            "qiu1 qi2",
            "qiū qí"),
        Sample("数字混排 日期", "2026年9月10日星期四",
            "2 0 2 6 NIAN 9 YUE 1 0 RI XING QI SI",
            "2 0 2 6 nian2 9 yue4 1 0 ri4 xing1 qi1 si4",
            "2 0 2 6 nián 9 yuè 1 0 rì xīng qī sì"),
        Sample("符号混排 价格", "价格：99.9元（原价199元）",
            "JIA GE ： 9 9 . 9 YUAN （ YUAN JIA 1 9 9 YUAN ）",
            "jia4 ge2 ： 9 9 . 9 yuan2 （ yuan2 jia4 1 9 9 yuan2 ）",
            "jià gé ： 9 9 . 9 yuán （ yuán jià 1 9 9 yuán ）"),
        Sample("书名号 红楼梦", "《红楼梦》曹雪芹著",
            "《 HONG LOU MENG 》 CAO XUE QIN ZHU",
            "《 hong2 lou2 meng4 》 cao2 xue3 qin2 zhu4",
            "《 hóng lóu mèng 》 cáo xuě qín zhù"),
        Sample("中英混排（大小写不改字母）", "中文ABC你",
            "ZHONG WEN A B C NI",
            "zhong1 wen2 A B C ni3",
            "zhōng wén A B C nǐ"),
        Sample("语气词 嗯呃哦唉", "嗯呃哦唉",
            "N E O AI",
            "n2 e4 o2 ai1",
            "n è ó āi"),
        Sample("标点重复", "重复测试！重复测试？",
            "ZHONG FU CE SHI ！ ZHONG FU CE SHI ？",
            "zhong4 fu4 ce4 shi4 ！ zhong4 fu4 ce4 shi4 ？",
            "zhòng fù cè shì ！ zhòng fù cè shì ？"),
        Sample("半角短横 + 空格透传", "北京-上海 高铁",
            "BEI JING - SHANG HAI   GAO TIE",
            "bei3 jing1 - shang4 hai3   gao1 tie3",
            "běi jīng - shàng hǎi   gāo tiě"),
    )

    /** 一条样本在三种配置下的期望值比对。 */
    private fun goldenOf(sample: Sample): List<Golden> = listOf(
        Golden("样本 ${sample.title} · 无调大写", sample.text, sample.plain,
            Pinyin.toPinyin(sample.text, " ", CFG_DEFAULT)),
        Golden("样本 ${sample.title} · 数字调小写", sample.text, sample.numbered,
            Pinyin.toPinyin(sample.text, " ", CFG_NUMBER_LOWER)),
        Golden("样本 ${sample.title} · 符号调小写", sample.text, sample.marked,
            Pinyin.toPinyin(sample.text, " ", CFG_MARK_LOWER)),
    )

    /** 内置样本展开全部期望值比对项（27 条样本 × 3 种配置）。 */
    fun sampleGoldens(): List<Golden> = SAMPLES.map { goldenOf(it) }.flatten()

    /** 一条内置样本的正确性 + 计时结果。 */
    data class SampleResult(val title: String, val goldens: List<Golden>, val timing: Timing) {
        val passed: Boolean get() = goldens.all { it.passed }

        fun render(): String = buildString {
            append(goldens.joinToString("") { if (it.passed) "✓" else "✗" })
            append(" ${goldens.first().input}【${title}】（${timing.sampleChars} 字）")
            append(" 平均 ${timing.average.millisText()}，")
            append("${timing.microsPerChar} µs/字，${timing.charsPerSecond} 字/秒")
            goldens.filterNot { it.passed }.forEach { g ->
                appendLine()
                append("    ${g.title}：期望=${g.expected} 实际=${g.actual}")
            }
        }
    }

    /** 逐条跑内置样本：三种配置比对 + 每条样本的转换计时（符号调小写）。 */
    fun runSamples(iterations: Int = 20): List<SampleResult> =
        SAMPLES.map { sample ->
            SampleResult(
                title = sample.title,
                goldens = goldenOf(sample),
                timing = benchmark(sample.text, CFG_MARK_LOWER, iterations),
            )
        }

    /** 只做正确性比对（API 用例 + 内置样本 + 不变量），不计时，可在界面启动时自动执行。 */
    fun checkOnly(): Report = Report(
        goldens = goldens() + sampleGoldens(),
        invariants = invariants() + globalConfigCheck(),
    )

    /** 结构性不变量：对长文本逐项检查，返回失败原因。 */
    fun invariants(): List<Check> {
        val result = ArrayList<Check>()

        val spaced = Pinyin.toPinyin(LONG_SAMPLE, " ", CFG_DEFAULT).split(" ")
        result += Check(
            "token 数 == 字符数",
            spaced.size == LONG_SAMPLE.length,
            "tokens=${spaced.size} chars=${LONG_SAMPLE.length}",
        )

        // 只有 token 数与字符数一致时，才能按位置一一比对。
        val pairs = if (spaced.size == LONG_SAMPLE.length) {
            LONG_SAMPLE.mapIndexed { index, char -> char to spaced[index] }
        } else {
            emptyList()
        }
        val badTokens = pairs.filter { (char, token) ->
            Pinyin.isChinese(char) && token.none { it in 'A'..'Z' }
        }
        result += Check(
            "每个汉字都转出 ASCII 拼音",
            badTokens.isEmpty(),
            badTokens.take(3).joinToString { "${it.first}->${it.second}" },
        )

        val stable = Pinyin.toPinyin(LONG_SAMPLE, " ", CFG_MARK_LOWER) ==
            Pinyin.toPinyin(LONG_SAMPLE, " ", CFG_MARK_LOWER)
        result += Check("同配置重复转换结果一致（lazy 解码稳定）", stable)

        val caseAligned = Pinyin.toPinyin(LONG_SAMPLE, " ", CFG_DEFAULT).lowercase() ==
            Pinyin.toPinyin(LONG_SAMPLE, " ", PinyinConfig(ToneStyle.NONE, PinyinCase.LOWERCASE))
        result += Check("UPPER/LOWER 仅大小写差异", caseAligned)

        val withDict = Pinyin.toPinyin("我去重庆看中文", " ", CFG_CITY)
        val withoutDict = Pinyin.toPinyin("我去重庆看中文", " ", CFG_DEFAULT)
        result += Check(
            "词典只影响命中的词",
            withDict.startsWith("WO QU CHONG QING") && withoutDict.startsWith("WO QU ZHONG QING"),
            "with=$withDict / without=$withoutDict",
        )

        // 词典值里的声调数字必须被消耗掉：符号调下不应残留裸数字，且整句风格统一。
        val cityMark = Pinyin.toPinyin("我去重庆看成都", " ", CFG_CITY_MARK)
        result += Check(
            "词典命中后符号调无裸数字残留",
            cityMark == "wǒ qù chóng qìng kàn chéng dū",
            "actual=$cityMark",
        )

        val ranges = listOf('一' to true, '龥' to true, '㐀' to true, '中' to true, 'A' to false, 'あ' to false, '가' to false, '。' to false)
            .filter { Pinyin.isChinese(it.first) != it.second }
        result += Check("isChinese 覆盖主区块/Ext-A 且不误判", ranges.isEmpty(), ranges.joinToString { "${it.first}" })

        return result
    }

    /** 全局配置（[Pinyin.config]）路径的检查；执行后恢复默认，避免影响界面其它区域。 */
    private fun globalConfigCheck(): Check {
        return try {
            Pinyin.config {
                toneStyle(ToneStyle.TONE_NUMBER)
                case(PinyinCase.LOWERCASE)
                with(PinyinMapDict("重庆" to arrayOf("chong2", "qing4")))
            }
            val actual = Pinyin.toPinyin("我去重庆")
            Check("全局 config + 自定义词典", actual == "wo3 qu4 chong2 qing4", "actual=$actual")
        } finally {
            Pinyin.reset()
        }
    }

    /** 跑一遍全部检查 + 计时。耗时量级为数十毫秒（Web 端更慢），点击时同步执行。 */
    fun verify(iterations: Int = 100): Report {
        val timing = benchmark(LONG_SAMPLE, CFG_DEFAULT, iterations)

        val charApi = measure {
            LONG_SAMPLE.forEach { Pinyin.toPinyin(it, CFG_MARK_LOWER) }
        }
        val dictRun = benchmark(LONG_SAMPLE, CFG_CITY, iterations / 2)
        val markRun = benchmark(LONG_SAMPLE, CFG_MARK_LOWER, iterations / 2)

        val extras = listOf(
            "逐字 API" to "${round2(charApi.toDouble(DurationUnit.MICROSECONDS) / LONG_SAMPLE.length)} µs/字",
            "TONE_MARK" to markRun.average.millisText() + " / 次",
            "带 CnCityDict" to dictRun.average.millisText() + " / 次（较无词典 ${diffText(timing.average, dictRun.average)}）",
        )

        return Report(
            goldens = goldens() + sampleGoldens(),
            invariants = invariants() + globalConfigCheck(),
            timing = timing,
            extras = extras,
        )
    }

    /**
     * 校验用户给定的一堆中文：[text] 中每个汉字都必须转出含 ASCII 字母的拼音（不能
     * 原样回显），并统计 [iterations] 次转换的耗时。
     */
    fun checkText(text: String, config: PinyinConfig = CFG_DEFAULT, iterations: Int = 50): TextCheck {
        val tokens = Pinyin.toPinyin(text, " ", config).split(" ")
        // 汉字转出的 token 应含拉丁字母（TONE_MARK 下也是拉丁字母加变音符号）。
        val unmapped = if (tokens.size == text.length) {
            text.mapIndexed { index, char -> char to tokens[index] }
                .filter { (char, token) ->
                    Pinyin.isChinese(char) && token.none { it in 'a'..'z' || it in 'A'..'Z' }
                }
                .map { it.first }
        } else {
            // 分隔符被输入文本包含时位置会错位，退回逐字校验。
            text.toList().filter { char ->
                Pinyin.isChinese(char) &&
                    Pinyin.toPinyin(char, config).none { it in 'a'..'z' || it in 'A'..'Z' }
            }
        }
        return TextCheck(
            text = text,
            chineseChars = text.count { Pinyin.isChinese(it) },
            unmapped = unmapped,
            timing = benchmark(text, config, iterations),
        )
    }

    /** 对 [text] 重复转换 [iterations] 次并统计耗时（前 3 次作为预热不计入）。 */
    fun benchmark(text: String, config: PinyinConfig = CFG_DEFAULT, iterations: Int = 100): Timing {
        val body: () -> Unit = { Pinyin.toPinyin(text, " ", config) }
        val cold = measure(body)
        repeat(3) { body() }
        val runs = ArrayList<Duration>(iterations)
        repeat(iterations) { runs += measure(body) }
        val total = runs.fold(Duration.ZERO) { acc, d -> acc + d }
        return Timing(
            sampleChars = text.length,
            iterations = iterations,
            cold = cold,
            average = if (runs.isEmpty()) Duration.ZERO else total / runs.size,
            fastest = runs.minOrNull() ?: Duration.ZERO,
            slowest = runs.maxOrNull() ?: Duration.ZERO,
        )
    }

    private fun diffText(base: Duration, other: Duration): String {
        val delta = (other - base).toDouble(DurationUnit.MILLISECONDS)
        val sign = if (delta >= 0) "+" else ""
        return "$sign${round2(delta)} ms"
    }

    private fun measure(block: () -> Unit): Duration {
        val start = TimeSource.Monotonic.markNow()
        block()
        return start.elapsedNow()
    }
}

/**
 * 下面两个辅助函数定在文件顶层（而非 object 成员），因为 [PinyinVerifier.Timing] /
 * [PinyinVerifier.Report] 是嵌套类，没有外层 receiver，无法调用 object 的 private 成员函数。
 */
/** 亚 0.01 ms 的量自动改用 µs 展示，否则短样本会一片“0.0 ms”。 */
private fun Duration.millisText(): String {
    val ms = toDouble(DurationUnit.MILLISECONDS)
    return if (ms < 0.01) "${round2(toDouble(DurationUnit.MICROSECONDS))} µs" else "${round2(ms)} ms"
}

private fun round2(value: Double): Double = (value * 100).roundToLong() / 100.0
