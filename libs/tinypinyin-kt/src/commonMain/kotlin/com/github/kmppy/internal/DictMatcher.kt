package com.github.kmppy.internal

/**
 * Longest-prefix trie matcher over a user dictionary.
 *
 * Mirrors TinyPinyin's strategy: while converting a string, greedily match the longest
 * dictionary word at each position; if none matches, fall back to the single-character
 * default table.
 */
internal class DictMatcher(mapping: Map<String, Array<String>>) {

    private class Node {
        val children = HashMap<Char, Node>()
        var values: Array<String>? = null
    }

    private val root = Node()
    val isEmpty: Boolean get() = root.children.isEmpty()

    /** Matched result: the per-character pinyin and how many chars were consumed. */
    class Match(val values: Array<String>, val length: Int)

    init {
        for ((word, values) in mapping) {
            if (word.isEmpty()) continue
            var node = root
            for (c in word) {
                node = node.children.getOrPut(c) { Node() }
            }
            node.values = values
        }
    }

    /** Returns the longest match starting at [chars] at [start], or null. */
    fun match(chars: CharArray, start: Int): Match? {
        var node = root
        var bestValues: Array<String>? = null
        var bestLen = 0
        var i = start
        while (i < chars.size) {
            val next = node.children[chars[i]] ?: break
            node = next
            val values = node.values
            if (values != null && values.isNotEmpty()) {
                bestValues = values
                bestLen = i - start + 1
            }
            i++
        }
        return bestValues?.let { Match(it, bestLen) }
    }
}
