// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15h — v3 beat labels + lift projection past stroke end

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v6 {

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
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val raw = HeatmapSwipeSegmentInfer_v3.infer(keyboard, pointers) ?: return null
        val straight = HeatmapSwipeStraightLine_v1.analyze(pointers)
        val size = pointers.pointerSize
        if (size < 1) return null
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val liftEnd = HeatmapSwipeLiftProject_v1.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v2.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        val touchStart = HeatmapKeyLikelihood_v2.bestLabelAt(layout, xs[0], ys[0])

        val (pathLetters, beatCount, startLabel, endLabel) = when (straight.shape) {
            HeatmapSwipeStraightLine_v1.StrokeShape.NEAR_STRAIGHT_TWO_LETTER -> {
                lockStartEndPath(layout, pointers, size)
            }
            HeatmapSwipeStraightLine_v1.StrokeShape.NEAR_STRAIGHT_END_CURVE -> {
                val deduped = HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(raw.pathLetters)
                val capped = deduped.take(straight.maxWordLength.coerceAtLeast(2))
                Quad(capped, capped.size, touchStart ?: capped.firstOrNull(), liftEnd ?: capped.lastOrNull())
            }
            HeatmapSwipeStraightLine_v1.StrokeShape.GENERAL -> {
                val deduped = HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(raw.pathLetters)
                Quad(deduped, deduped.size, touchStart ?: deduped.firstOrNull(), liftEnd ?: deduped.lastOrNull())
            }
        }
        if (pathLetters.isEmpty()) return null
        return Result(
            startKeyLabel = startLabel,
            pathLetters = pathLetters,
            pathLettersRaw = raw.pathLetters,
            endKeyLabel = endLabel,
            beatCount = beatCount,
            beatCountRaw = raw.beatCount,
            classifiedBeats = raw.classifiedBeats,
            straightLine = straight,
            maxWordLength = straight.maxWordLength,
        )
    }

    private fun lockStartEndPath(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        size: Int,
    ): Quad {
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val start = HeatmapKeyLikelihood_v2.bestLabelAt(layout, xs[0], ys[0])
        val end = HeatmapSwipeLiftProject_v1.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v2.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        val letters = when {
            start == null && end == null -> emptyList()
            start == null -> listOfNotNull(end)
            end == null -> listOfNotNull(start)
            start == end -> listOf(start)
            else -> listOf(start, end)
        }
        return Quad(letters, letters.size, start, end)
    }

    private data class Quad(
        val letters: List<String>,
        val beatCount: Int,
        val start: String?,
        val end: String?,
    )
}
