// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15m — always keep full touched path; never collapse to start/end only

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v11 {

    data class Result(
        val startKeyLabel: String?,
        val pathLetters: List<String>,
        val pathLettersRaw: List<String>,
        val endKeyLabel: String?,
        val beatCount: Int,
        val beatCountRaw: Int,
        val classifiedBeats: List<HeatmapGeometryClassifier_v1.ClassifiedBeat>,
        val straightLine: HeatmapSwipeStraightLine_v1.Analysis,
        val maxWordLength: Int,
        val normalized: HeatmapPathLettersNormalize_v2.Normalized,
        val touchedLetters: Set<String>,
        val touchCounts: Map<String, Int>,
        val rejectedTouchLetters: Set<String>,
        val strokeOrderLetters: List<String>,
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val raw = HeatmapSwipeSegmentInfer_v6.infer(keyboard, pointers) ?: return null
        val graph = HeatmapKeyNeighborGraph_v2.fromLayout(layout)
        val touch = HeatmapSwipeStrokeTouchSet_v2.collect(layout, pointers)
        val normalized = HeatmapPathLettersNormalize_v6.normalize(
            raw = raw.pathLetters,
            neighborGraph = graph,
            layout = layout,
            pointers = pointers,
            touch = touch,
        )
        val straight = HeatmapSwipeStraightLine_v1.analyze(pointers)
        val pathLetters = normalized.letters
        if (pathLetters.isEmpty()) return null
        val size = pointers.pointerSize
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val liftEnd = HeatmapSwipeLiftProject_v2.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        val touchStart = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[0], ys[0])
        val startLabel = pickStart(touchStart, pathLetters, touch.touched)
        val endLabel = pickEnd(liftEnd, pathLetters, touch.touched)
        val maxWordLength = HeatmapSwipeMaxWordLenPolicy_v1.maxDictLen(
            pathLetters.size,
            raw.beatCount,
            touch.touched.size,
        )
        return Result(
            startKeyLabel = startLabel,
            pathLetters = pathLetters,
            pathLettersRaw = raw.pathLetters,
            endKeyLabel = endLabel,
            beatCount = pathLetters.size.coerceAtLeast(raw.beatCount),
            beatCountRaw = raw.beatCount,
            classifiedBeats = raw.classifiedBeats,
            straightLine = straight,
            maxWordLength = maxWordLength,
            normalized = normalized,
            touchedLetters = touch.touched,
            touchCounts = touch.counts,
            rejectedTouchLetters = touch.rejectedLowCount,
            strokeOrderLetters = touch.orderedLetters,
        )
    }

    private fun pickStart(touch: String?, letters: List<String>, touched: Set<String>): String? =
        touch?.takeIf { it in touched } ?: letters.firstOrNull { it in touched }

    private fun pickEnd(lift: String?, letters: List<String>, touched: Set<String>): String? =
        lift?.takeIf { it in touched } ?: letters.lastOrNull { it in touched }
}
