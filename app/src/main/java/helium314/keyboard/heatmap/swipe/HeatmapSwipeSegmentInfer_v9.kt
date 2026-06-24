// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15k — v6 beats + v4 normalize + touch set; v5 lift; no far-key projection

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v9 {

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
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val raw = HeatmapSwipeSegmentInfer_v6.infer(keyboard, pointers) ?: return null
        val graph = HeatmapKeyNeighborGraph_v2.fromLayout(layout)
        val touched = HeatmapSwipeStrokeTouchSet_v1.collect(layout, pointers)
        val normalized = HeatmapPathLettersNormalize_v4.normalize(
            raw = raw.pathLetters,
            neighborGraph = graph,
            layout = layout,
            pointers = pointers,
            beatIndices = raw.beatIndices,
        )
        val straight = HeatmapSwipeStraightLine_v1.analyze(pointers)
        val size = pointers.pointerSize
        if (size < 1) return null
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val liftEnd = HeatmapSwipeLiftProject_v2.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        val touchStart = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[0], ys[0])

        val (pathLetters, beatCount, startLabel, endLabel) = when (straight.shape) {
            HeatmapSwipeStraightLine_v1.StrokeShape.NEAR_STRAIGHT_TWO_LETTER -> {
                lockStartEndPath(layout, pointers, size, touched)
            }
            HeatmapSwipeStraightLine_v1.StrokeShape.NEAR_STRAIGHT_END_CURVE -> {
                val capped = normalized.letters.take(straight.maxWordLength.coerceAtLeast(2))
                Quad(
                    capped,
                    capped.size,
                    pickStart(touchStart, capped, touched),
                    pickEnd(liftEnd, capped, touched),
                )
            }
            HeatmapSwipeStraightLine_v1.StrokeShape.GENERAL -> {
                Quad(
                    normalized.letters,
                    normalized.letters.size,
                    pickStart(touchStart, normalized.letters, touched),
                    pickEnd(liftEnd, normalized.letters, touched),
                )
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
            normalized = normalized,
            touchedLetters = touched,
        )
    }

    private fun lockStartEndPath(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        size: Int,
        touched: Set<String>,
    ): Quad {
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val start = HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[0], ys[0])
        val end = HeatmapSwipeLiftProject_v2.liftLabel(layout, pointers)
            ?: HeatmapKeyLikelihood_v5.bestLabelAt(layout, xs[size - 1], ys[size - 1])
        val startOk = start?.takeIf { it in touched }
        val endOk = end?.takeIf { it in touched }
        val letters = when {
            startOk == null && endOk == null -> emptyList()
            startOk == null -> listOfNotNull(endOk)
            endOk == null -> listOfNotNull(startOk)
            startOk == endOk -> listOf(startOk)
            else -> listOf(startOk, endOk)
        }
        return Quad(letters, letters.size, startOk, endOk)
    }

    private fun pickStart(touch: String?, letters: List<String>, touched: Set<String>): String? =
        touch?.takeIf { it in touched } ?: letters.firstOrNull { it in touched }

    private fun pickEnd(lift: String?, letters: List<String>, touched: Set<String>): String? =
        lift?.takeIf { it in touched } ?: letters.lastOrNull { it in touched }

    private data class Quad(
        val letters: List<String>,
        val beatCount: Int,
        val start: String?,
        val end: String?,
    )
}
