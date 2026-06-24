// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — in-memory gesture lexicon enumerated from the active tap dictionary.
// Backed by a lexicographically sorted Array<String> + parallel IntArray of frequencies
// (a compact implicit trie: ~11MB for English, no per-node objects). Candidate generation
// is a binary-search prefix-range DFS that only descends into letters reachable, in order,
// from the visited-key sequence (neighbor tolerant) — i.e. words that are a plausible
// subsequence of what the finger crossed. Built once per dictionary on a background thread
// and cached; callers get null until the build for the current dictionary completes.
package helium314.keyboard.heatmap.swipe

import com.android.inputmethod.latin.BinaryDictionary
import helium314.keyboard.latin.DictionaryFacilitator
import helium314.keyboard.latin.dictionary.ReadOnlyBinaryDictionary
import helium314.keyboard.latin.utils.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class HeatmapLexiconTrie_v1 private constructor(
    private val words: Array<String>,
    private val freqs: IntArray,
) {

    val size: Int get() = words.size

    data class Candidate(val word: String, val frequency: Int)

    /**
     * All lexicon words that are a neighbor-tolerant in-order subsequence of [visited], whose
     * first letter is in [startChars]. Bounded by [maxLen] and [maxCandidates].
     */
    fun collectSubsequenceWords(
        visited: List<Char>,
        neighbors: Map<Char, Set<Char>>,
        startChars: Set<Char>,
        maxLen: Int,
        maxCandidates: Int,
    ): List<Candidate> {
        val result = ArrayList<Candidate>(256)
        if (words.isEmpty() || visited.isEmpty() || startChars.isEmpty()) return result
        val ctx = SearchCtx(visited, neighbors, startChars, maxLen, maxCandidates, result)
        dfs(0, 0, words.size, 0, ctx)
        return result
    }

    private class SearchCtx(
        val visited: List<Char>,
        val neighbors: Map<Char, Set<Char>>,
        val startChars: Set<Char>,
        val maxLen: Int,
        val maxCandidates: Int,
        val result: ArrayList<Candidate>,
    )

    private fun dfs(depth: Int, lo: Int, hi: Int, fromIdx: Int, ctx: SearchCtx) {
        if (ctx.result.size >= ctx.maxCandidates) return
        var i = lo
        // Terminal: the word equal to the current prefix (if any) sorts first in [lo,hi).
        if (words[lo].length == depth) {
            ctx.result.add(Candidate(words[lo], freqs[lo]))
            i = lo + 1
        }
        if (depth >= ctx.maxLen) return
        while (i < hi) {
            if (ctx.result.size >= ctx.maxCandidates) return
            val w = words[i]
            if (w.length <= depth) {
                i++
                continue
            }
            val ch = w[depth]
            var j = i + 1
            while (j < hi && words[j].length > depth && words[j][depth] == ch) j++
            val allowed = if (depth == 0) ch in ctx.startChars else true
            if (allowed) {
                val nv = earliestMatch(ch, fromIdx, ctx)
                if (nv >= 0) {
                    dfs(depth + 1, i, j, nv + 1, ctx)
                }
            }
            i = j
        }
    }

    private fun earliestMatch(ch: Char, fromIdx: Int, ctx: SearchCtx): Int {
        var k = fromIdx
        val v = ctx.visited
        while (k < v.size) {
            val cur = v[k]
            if (cur == ch || ctx.neighbors[cur]?.contains(ch) == true) return k
            k++
        }
        return -1
    }

    // ai-note: STRICT anchor-aligned enumeration — the heart of the v2 anchor-driven decoder.
    // A word qualifies ONLY if its collapsed-run letters equal the detected anchor sequence,
    // in order, neighbor-tolerant. Each letter of a candidate word does exactly one of:
    //   (a) consume the current anchor (advance anchorIndex; arms a possible double of this letter)
    //   (b) repeat the just-matched anchor letter as a DOUBLE (no advance; disarms further doubling)
    // Terminal is valid only when every anchor has been consumed (end is hard). This rejects
    // "eras"(4 runs) for anchors e,g,s and accepts "eggs"(gg double), accepts "cheese"(ee double),
    // rejects "flying"/"fucking" for f,l,a,g, etc. [anchors] must be lowercase letter keys.
    fun collectAnchorAlignedWords(
        anchors: List<Char>,
        neighbors: Map<Char, Set<Char>>,
        maxCandidates: Int,
    ): List<Candidate> {
        val result = ArrayList<Candidate>(64)
        if (words.isEmpty() || anchors.isEmpty()) return result
        val ctx = AnchorCtx(anchors, neighbors, maxCandidates, result)
        adfs(0, 0, words.size, 0, '\u0000', false, ctx)
        return result
    }

    private class AnchorCtx(
        val anchors: List<Char>,
        val neighbors: Map<Char, Set<Char>>,
        val maxCandidates: Int,
        val result: ArrayList<Candidate>,
    )

    private fun anchorMatches(ch: Char, anchor: Char, ctx: AnchorCtx): Boolean {
        if (ch == anchor) return true
        return ctx.neighbors[anchor]?.contains(ch) == true
    }

    private fun adfs(
        depth: Int,
        lo: Int,
        hi: Int,
        anchorIndex: Int,
        lastChar: Char,
        canDouble: Boolean,
        ctx: AnchorCtx,
    ) {
        if (ctx.result.size >= ctx.maxCandidates) return
        var i = lo
        // Terminal: a word ending exactly here is valid only if ALL anchors were consumed.
        if (words[lo].length == depth) {
            if (anchorIndex == ctx.anchors.size) ctx.result.add(Candidate(words[lo], freqs[lo]))
            i = lo + 1
        }
        // No word may be longer than anchorCount + at most one double per anchor.
        if (depth >= ctx.anchors.size * 2) return
        while (i < hi) {
            if (ctx.result.size >= ctx.maxCandidates) return
            val w = words[i]
            if (w.length <= depth) {
                i++
                continue
            }
            val ch = w[depth]
            var j = i + 1
            while (j < hi && words[j].length > depth && words[j][depth] == ch) j++

            // Transition (a): consume the current anchor (advances index, arms doubling).
            if (anchorIndex < ctx.anchors.size && anchorMatches(ch, ctx.anchors[anchorIndex], ctx)) {
                adfs(depth + 1, i, j, anchorIndex + 1, ch, true, ctx)
            }
            // Transition (b): double the just-matched anchor letter (no advance, disarms doubling).
            if (canDouble && ch == lastChar) {
                adfs(depth + 1, i, j, anchorIndex, lastChar, false, ctx)
            }
            i = j
        }
    }

    companion object {
        private const val TAG = "HeatmapLexiconTrie"

        // ai-note: test-only factory so unit tests can build a lexicon from a fixed word list
        // without a live BinaryDictionary. Same module + package as the tests (internal).
        internal fun fromWordsForTest(words: Map<String, Int>): HeatmapLexiconTrie_v1 {
            val keys = words.keys.toTypedArray()
            keys.sort()
            val freqs = IntArray(keys.size) { words[keys[it]] ?: 0 }
            return HeatmapLexiconTrie_v1(keys, freqs)
        }

        @Volatile
        private var current: HeatmapLexiconTrie_v1? = null

        @Volatile
        private var currentSignature: String? = null

        private val building = AtomicBoolean(false)

        /**
         * Returns the lexicon for the facilitator's current main dictionary if it is built,
         * otherwise null and (once) kicks off a background build. Callers should fall back to
         * a lighter candidate source while this returns null.
         */
        fun getOrTriggerBuild(facilitator: DictionaryFacilitator): HeatmapLexiconTrie_v1? {
            val sig = signatureOf(facilitator) ?: return null
            if (sig == currentSignature) return current
            if (building.compareAndSet(false, true)) {
                Thread {
                    try {
                        val built = build(facilitator)
                        if (built != null) {
                            current = built
                            currentSignature = sig
                            Log.i(TAG, "lexicon built sig=$sig words=${built.size}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "lexicon build failed", e)
                    } finally {
                        building.set(false)
                    }
                }.apply {
                    name = "HeatmapLexiconBuild"
                    isDaemon = true
                }.start()
            }
            return null
        }

        private fun signatureOf(facilitator: DictionaryFacilitator): String? {
            val dict = facilitator.mainDictionary ?: return null
            val locale = dict.mLocale?.toString() ?: facilitator.currentLocale.toString()
            val hash = when (dict) {
                is ReadOnlyBinaryDictionary -> dict.hash
                is helium314.keyboard.latin.dictionary.DictionaryCollection -> {
                    dict.dictionaries.filterIsInstance<ReadOnlyBinaryDictionary>()
                        .joinToString(",") { it.hash }
                }
                else -> dict.hashCode().toString()
            }
            return "$locale|$hash"
        }

        private fun build(facilitator: DictionaryFacilitator): HeatmapLexiconTrie_v1? {
            val mainDict = facilitator.mainDictionary ?: return null
            val dicts = when (mainDict) {
                is ReadOnlyBinaryDictionary -> listOf(mainDict)
                is helium314.keyboard.latin.dictionary.DictionaryCollection -> {
                    mainDict.dictionaries.filterIsInstance<ReadOnlyBinaryDictionary>()
                }
                else -> return null
            }
            if (dicts.isEmpty()) return null

            val maxLen = HeatmapGestureTuningConstants_v2.MAX_WORD_LEN
            val minFreq = HeatmapGestureTuningConstants_v2.MIN_WORD_FREQUENCY
            val collected = HashMap<String, Int>(180_000)
            
            val consumer = object : BinaryDictionary.WordFrequencyConsumer {
                override fun accept(word: String, frequency: Int) {
                    if (word.length > maxLen) return
                    if (frequency < minFreq) return
                    var alpha = true
                    for (c in word) {
                        if (!c.isLetter()) {
                            alpha = false
                            break
                        }
                    }
                    if (!alpha) return
                    val key = word.lowercase(Locale.US)
                    val prev = collected[key]
                    if (prev == null || frequency > prev) collected[key] = frequency
                }
            }

            for (dict in dicts) {
                dict.forEachWord(consumer)
            }

            if (collected.isEmpty()) return null
            val keys = collected.keys.toTypedArray()
            keys.sort()
            val freqs = IntArray(keys.size)
            for (i in keys.indices) freqs[i] = collected[keys[i]] ?: 0
            return HeatmapLexiconTrie_v1(keys, freqs)
        }
    }
}
