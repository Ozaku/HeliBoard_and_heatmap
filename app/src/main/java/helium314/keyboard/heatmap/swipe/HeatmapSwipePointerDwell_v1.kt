// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15j — dwell from pointer sample density on key (wiggle on E without duplicate beat labels)

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipePointerDwell_v1 {

    private const val MIN_SAMPLES_IN_KEY = 8
    private const val MIN_LIKELIHOOD = 0.52
    private const val MIN_SEGMENT_SHARE = 0.22

    @JvmStatic
    fun detect(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        deduped: List<String>,
        beatIndices: List<Int>,
    ): List<HeatmapPathLettersNormalize_v2.DwellHint> {
        if (deduped.isEmpty() || pointers.pointerSize < 4) return emptyList()
        val hints = ArrayList<HeatmapPathLettersNormalize_v2.DwellHint>()
        val segments = buildSegments(pointers.pointerSize, beatIndices)
        for ((segIdx, range) in segments.withIndex()) {
            val letterCounts = HashMap<String, Int>()
            var total = 0
            for (i in range) {
                val x = pointers.xCoordinates[i]
                val y = pointers.yCoordinates[i]
                val top = HeatmapKeyLikelihood_v4.topKeysAt(
                    layout, x, y, maxKeys = 1, minLikelihood = MIN_LIKELIHOOD,
                )
                val label = top.firstOrNull()?.key?.storageLabel ?: continue
                if (label !in deduped) continue
                letterCounts[label] = letterCounts.getOrDefault(label, 0) + 1
                total++
            }
            if (total < MIN_SAMPLES_IN_KEY) continue
            val dominant = letterCounts.maxByOrNull { it.value } ?: continue
            if (dominant.value < MIN_SAMPLES_IN_KEY) continue
            if (dominant.value.toDouble() / total < MIN_SEGMENT_SHARE) continue
            val dedupIdx = mapSegmentToDedupedIndex(segIdx, segments.size, deduped)
            if (dedupIdx < 0) continue
            hints.add(
                HeatmapPathLettersNormalize_v2.DwellHint(
                    dedupedIndex = dedupIdx,
                    letter = dominant.key,
                    rawRunLength = dominant.value,
                ),
            )
        }
        return mergeHints(hints)
    }

    private fun buildSegments(size: Int, beatIndices: List<Int>): List<IntRange> {
        val anchors = beatIndices.distinct().sorted().filter { it in 0 until size }
        if (anchors.size < 2) return listOf(0 until size)
        val out = ArrayList<IntRange>()
        for (i in 0 until anchors.lastIndex) {
            out.add(anchors[i]..anchors[i + 1])
        }
        return out
    }

    private fun mapSegmentToDedupedIndex(
        segIdx: Int,
        segCount: Int,
        deduped: List<String>,
    ): Int {
        if (deduped.size == 1) return 0
        if (segCount <= 1) return deduped.size / 2
        val mapped = (segIdx.toDouble() * (deduped.size - 1) / (segCount - 1)).toInt()
        return mapped.coerceIn(0, deduped.lastIndex)
    }

    private fun mergeHints(
        hints: List<HeatmapPathLettersNormalize_v2.DwellHint>,
    ): List<HeatmapPathLettersNormalize_v2.DwellHint> {
        val byIndex = LinkedHashMap<Int, HeatmapPathLettersNormalize_v2.DwellHint>()
        for (hint in hints) {
            val prev = byIndex[hint.dedupedIndex]
            if (prev == null || hint.rawRunLength > prev.rawRunLength) {
                byIndex[hint.dedupedIndex] = hint
            }
        }
        return byIndex.values.toList()
    }
}
