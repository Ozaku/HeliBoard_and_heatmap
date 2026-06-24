// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15q — visit order v4 + normalize v9; multi-corner FLYING not FIG

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v15 {

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
        val multiCorner: Boolean,
    )

    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val raw = HeatmapSwipeRawBeatInfer_v2.infer(keyboard, pointers) ?: return null
        val graph = HeatmapKeyNeighborGraph_v2.fromLayout(layout)
        val touch = HeatmapSwipeStrokeTouchSet_v4.collect(layout, pointers)
        val normalized = HeatmapPathLettersNormalize_v9.normalize(
            rawBeatPath = raw.pathLetters,
            rawBeats = raw.rawBeats,
            neighborGraph = graph,
            layout = layout,
            pointers = pointers,
            touch = touch,
        )
        val straight = HeatmapSwipeStraightLine_v1.analyze(pointers)
        val pathLetters = normalized.letters
        if (pathLetters.isEmpty()) return null
        val multiCorner = HeatmapPathLettersNormalize_v9.isMultiCornerStroke(
            raw.beatCountRaw,
            raw.pathLetters,
            touch.orderedLetters,
        )
        val startLabel = pickStart(touch.startLabel, pathLetters, touch.touched)
        val endLabel = pickEnd(touch.liftLabel, pathLetters, touch.touched)
        val beatCountEffective = pathLetters.size.coerceAtLeast(raw.beatCount)
        val maxWordLength = HeatmapSwipeMaxWordLenPolicy_v1.maxDictLen(
            pathLetters.size,
            beatCountEffective,
            touch.orderedLetters.size.coerceAtLeast(touch.touched.size),
        )
        return Result(
            startKeyLabel = startLabel,
            pathLetters = pathLetters,
            pathLettersRaw = raw.pathLetters,
            endKeyLabel = endLabel,
            beatCount = beatCountEffective,
            beatCountRaw = raw.beatCountRaw,
            classifiedBeats = mapClassifiedBeats(raw.classifiedBeats),
            straightLine = straight,
            maxWordLength = maxWordLength,
            normalized = normalized,
            touchedLetters = touch.touched,
            touchCounts = touch.counts,
            rejectedTouchLetters = touch.rejectedTouchLetters,
            strokeOrderLetters = touch.orderedLetters,
            multiCorner = multiCorner,
        )
    }

    private fun mapClassifiedBeats(
        beats: List<HeatmapGeometryClassifier_v2.ClassifiedBeat>,
    ): List<HeatmapGeometryClassifier_v1.ClassifiedBeat> =
        beats.map { beat ->
            HeatmapGeometryClassifier_v1.ClassifiedBeat(
                x = beat.x,
                y = beat.y,
                index = beat.index,
                kind = when (beat.kind) {
                    HeatmapGeometryClassifier_v2.BeatKind.START ->
                        HeatmapGeometryClassifier_v1.BeatKind.START
                    HeatmapGeometryClassifier_v2.BeatKind.CORNER ->
                        HeatmapGeometryClassifier_v1.BeatKind.CORNER
                    HeatmapGeometryClassifier_v2.BeatKind.END ->
                        HeatmapGeometryClassifier_v1.BeatKind.END
                    HeatmapGeometryClassifier_v2.BeatKind.BRIDGE ->
                        HeatmapGeometryClassifier_v1.BeatKind.BRIDGE
                },
            )
        }

    private fun pickStart(touch: String?, letters: List<String>, touched: Set<String>): String? =
        touch?.takeIf { it in touched } ?: letters.firstOrNull { it in touched }

    private fun pickEnd(lift: String?, letters: List<String>, touched: Set<String>): String? =
        lift?.takeIf { it in touched } ?: letters.lastOrNull { it in touched }
}
