package com.github.kmppy.lexicons.cncity

import com.github.kmppy.dict.PinyinDict

/**
 * A small Chinese place-name lexicon (对标 `tinypinyin-lexicons-*-cncity`).
 *
 * Its main purpose is to fix polyphonic characters in proper nouns, e.g. 重庆 where
 * 重 is read "chóng" rather than the default "zhòng", and 成都 where 都 is read "dū"
 * rather than the default "dōu".
 *
 * Every value carries an explicit tone digit, so matched words follow the configured
 * [com.github.kmppy.ToneStyle]: 重庆 renders as `CHONG QING` / `chong2 qing4` / `chóng qìng`.
 * Digits and bases were read off the library's own data source
 * (`libs/generator/src/main/resources/pinyin-dict.tsv`), not typed from memory. Only
 * 重庆 / 成都 / 单县 actually override a wrong single-character default; the other words
 * match the table already and are kept because upstream ships them.
 */
object CnCityDict : PinyinDict {

    private val mapping: Map<String, Array<String>> = mapOf(
        "重庆" to arrayOf("CHONG2", "QING4"), // 重 is chóng here, table default is zhòng
        "上海" to arrayOf("SHANG4", "HAI3"),
        "北京" to arrayOf("BEI3", "JING1"),
        "广州" to arrayOf("GUANG3", "ZHOU1"),
        "深圳" to arrayOf("SHEN1", "ZHEN4"),
        "成都" to arrayOf("CHENG2", "DU1"), // 都 is dū here, table default is dōu
        "武汉" to arrayOf("WU3", "HAN4"),
        "西安" to arrayOf("XI1", "AN1"),
        "杭州" to arrayOf("HANG2", "ZHOU1"),
        "南京" to arrayOf("NAN2", "JING1"),
        "单县" to arrayOf("SHAN4", "XIAN4"), // 单 is shàn here, table default is dàn
    )

    override fun mapping(): Map<String, Array<String>> = mapping
}
