// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15n — prefer lift-pointer end; tail samples weighted for T after E wiggle

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v12 {

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

    private const val LIFT_TAIL_FRACTION = 0.18
    private const val LIFT_TAIL_MIN_SAMPLES = 3

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
        val liftEnd = resolveLiftEnd(layout, pointers, touch.touched)
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

    /** ai-note: lift label from last point; tail majority vote if lift point ambiguous */
    private fun resolveLiftEnd(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        touched: Set<String>,
    ): String? {
        val size = pointers.pointerSize
        if (size < 1) return null
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val direct = HeatmapSwipeLiftProject_v2.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        val tailFrom = (size * (1.0 - LIFT_TAIL_FRACTION)).toInt().coerceIn(0, size - 1)
        val counts = HashMap<String, Int>()
        for (i in tailFrom until size) {
            val label = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[i], ys[i]) ?: continue
            if (label !in touched) continue
            counts[label] = counts.getOrDefault(label, 0) + 1
        }
        val tailWinner = counts.maxByOrNull { it.value }
        if (tailWinner != null && tailWinner.value >= LIFT_TAIL_MIN_SAMPLES) {
            return tailWinner.key
        }
        return direct?.takeIf { it in touched }
    }

    private fun pickStart(touch: String?, letters: List<String>, touched: Set<String>): String? =
        touch?.takeIf { it in touched } ?: letters.firstOrNull { it in touched }

    private fun pickEnd(lift: String?, letters: List<String>, touched: Set<String>): String? =
        lift?.takeIf { it in touched } ?: letters.lastOrNull { it in touched }
}
