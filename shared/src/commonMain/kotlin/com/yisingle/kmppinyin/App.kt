package com.yisingle.kmppinyin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kmppy.Pinyin
import com.github.kmppy.PinyinCase
import com.github.kmppy.PinyinConfig
import com.github.kmppy.ToneStyle
import com.github.kmppy.lexicons.cncity.CnCityDict

/**
 * 拼音库多端验证台：覆盖 [Pinyin] 的字符串转换、逐字转换、[ToneStyle]/[PinyinCase]
 * 配置以及 [CnCityDict] 多音字词典行为。
 *
 * 打开界面就会自动跑一轮 [PinyinVerifier.checkOnly]（内置中文样本 + API 用例），
 * 不需要手动输入任何中文；输入框只用于额外的交互式试错。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
        var input by remember { mutableStateOf("我去重庆看中文") }
        var separator by remember { mutableStateOf(" ") }
        var toneStyle by remember { mutableStateOf(ToneStyle.TONE_MARK) }
        var pinyinCase by remember { mutableStateOf(PinyinCase.LOWERCASE) }
        var useDict by remember { mutableStateOf(true) }

        val config = remember(toneStyle, pinyinCase, useDict) {
            PinyinConfig(
                toneStyle = toneStyle,
                case = pinyinCase,
                dict = if (useDict) CnCityDict else null,
            )
        }
        val sep = separator.ifEmpty { " " }
        val output = remember(input, config, sep) {
            if (input.isEmpty()) "" else Pinyin.toPinyin(input, sep, config)
        }
        val chineseCount = remember(input) { input.count { Pinyin.isChinese(it) } }

        // 自动化校验结果：启动时自动跑一轮正确性比对，点击按钮时额外计时
        var report by remember { mutableStateOf<PinyinVerifier.Report?>(null) }
        var textCheck by remember { mutableStateOf<PinyinVerifier.TextCheck?>(null) }
        var samples by remember { mutableStateOf<List<PinyinVerifier.SampleResult>?>(null) }

        // 默认逐条列出每一个期望值比对（带中文），只出错时才需要收敛成失败项
        var onlyFailed by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            report = PinyinVerifier.checkOnly()
        }

        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("TinyPinyin-KT 测试台", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入中文") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = separator,
                onValueChange = { separator = it },
                label = { Text("分隔符") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "中文字符数：$chineseCount / ${input.length}",
                style = MaterialTheme.typography.bodySmall,
            )

            OptionsRow(
                title = "声调",
                options = ToneStyle.entries.toList(),
                selected = toneStyle,
                labelOf = {
                    when (it) {
                        ToneStyle.NONE -> "无"
                        ToneStyle.TONE_NUMBER -> "数字"
                        ToneStyle.TONE_MARK -> "符号"
                    }
                },
                onSelect = { toneStyle = it },
            )
            OptionsRow(
                title = "大小写",
                options = PinyinCase.entries.toList(),
                selected = pinyinCase,
                labelOf = {
                    when (it) {
                        PinyinCase.UPPERCASE -> "UPPER"
                        PinyinCase.LOWERCASE -> "lower"
                        PinyinCase.ORIGINAL -> "原样"
                    }
                },
                onSelect = { pinyinCase = it },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useDict, onCheckedChange = { useDict = it })
                Text("启用地名词典 CnCityDict（重庆 -> chóng qìng、成都 -> chéng dū，修正多音字且保留声调）")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("转换结果", style = MaterialTheme.typography.labelLarge)
                    Text(
                        output.ifEmpty { "请输入内容" },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            Text("自动化校验", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { report = PinyinVerifier.verify() }) {
                    Text("全量校验 + 长文本计时")
                }
                OutlinedButton(
                    onClick = {
                        samples = PinyinVerifier.runSamples(iterations = 100)
                    },
                ) {
                    Text("逐样本校验 + 计时")
                }
                OutlinedButton(
                    onClick = {
                        // 输入框为空时用长文本（静夜思 + 春晓 × 6）作为待测文本
                        val text = input.ifEmpty { PinyinVerifier.LONG_SAMPLE }
                        textCheck = PinyinVerifier.checkText(text, config, iterations = 200)
                    },
                ) {
                    Text("校验并计时当前输入")
                }
            }
            samples?.let { results ->
                val okCount = results.count { it.passed }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "内置样本：$okCount / ${results.size} 通过（每条比对无调/数字调/符号调三种配置）",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (okCount == results.size) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        Text(
                            text = results.joinToString("\n") { it.render() },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
            textCheck?.let { result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (result.passed) {
                                "当前输入校验通过"
                            } else {
                                "当前输入有 ${result.unmapped.size} 个汉字未转出拼音"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (result.passed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        Text(
                            text = result.render(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
            report?.let { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (r.allPassed) "全部通过：${r.passed} / ${r.total}" else "失败 ${r.failed} 项，通过 ${r.passed} / ${r.total}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (r.allPassed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = onlyFailed, onCheckedChange = { onlyFailed = it })
                            Text(
                                "只看失败（不勾则逐条列出测过的中文：${r.goldens.size} 条期望比对）",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        SelectionContainer {
                            Text(
                                text = r.render(onlyFailed),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }

            Text("逐字对照", style = MaterialTheme.typography.labelLarge)
            Text(
                "（逐字对照走单字 API，词典只在整句转换时生效，所以这里仍是每个字的表内默认音）",
                style = MaterialTheme.typography.bodySmall,
            )
            input.take(40).chunked(4).forEach { line ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    line.forEach { c ->
                        Box(modifier = Modifier.width(120.dp)) {
                            Text("$c -> ${Pinyin.toPinyin(c, config)}")
                        }
                    }
                }
            }
            if (input.length > 40) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("（仅显示前 40 字）", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun <T> OptionsRow(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
        options.forEach { option ->
            if (option == selected) {
                Button(onClick = { onSelect(option) }) {
                    Text(labelOf(option))
                }
            } else {
                TextButton(onClick = { onSelect(option) }) {
                    Text(labelOf(option), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
