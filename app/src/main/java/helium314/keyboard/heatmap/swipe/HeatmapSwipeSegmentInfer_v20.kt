// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v20 — v2 intent classify + v14 normalize; reuses v19 Result for downstream compat

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers

object HeatmapSwipeSegmentInfer_v20 {

    fun infer(keyboard: Keyboard, pointers: InputPointers): HeatmapSwipeSegmentInfer_v19.Result? {
        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null
        val raw = HeatmapSwipeRawBeatInfer_v3.infer(keyboard, pointers) ?: return null
        val graph = HeatmapKeyNeighborGraph_v2.fromLayout(layout)
        val kinematics = HeatmapSwipeStrokeKinematics_v1.analyze(layout, pointers)
        val touch = HeatmapSwipeStrokeTouchSet_v6.collect(layout, pointers, kinematics)
        val intent = HeatmapSwipeIntentClassifier_v2.classify(layout, pointers, kinematics, raw)
        val normalized = HeatmapPathLettersNormalize_v14.normalize(
            intentPath = intent.intentLetters,
            neighborGraph = graph,
            touch = touch,
            kinematics = kinematics,
            layout = layout,
            pointers = pointers,
            intent = intent,
        )
        val straight = HeatmapSwipeStraightLine_v1.analyze(pointers)
        val pathLetters = normalized.letters
        if (pathLetters.isEmpty()) return null
        val trace = HeatmapSwipeStrokeTrace_v1.build(layout, pointers, touch)
        val wiggleHints = HeatmapSwipeKeyWiggleDetector_v2.detect(layout, pointers, pathLetters)
        val doublePrefixIndices = HeatmapSwipeDoublePrefixHint_v1.hintIndices(
            pathLetters, raw.beatCountRaw, wiggleHints,
        )
        val startLabel = pickStart(touch.startLabel, touch.startDistribution, pathLetters, touch.touched)
        val endLabel = pickEnd(touch.liftLabel, pathLetters, touch.touched)
        val beatCountEffective = pathLetters.size.coerceAtLeast(raw.beatCount)
        val maxWordLength = HeatmapSwipeMaxWordLenPolicy_v1.maxDictLen(
            pathLetters.size,
            beatCountEffective,
            touch.touched.size,
        )
        return HeatmapSwipeSegmentInfer_v19.Result(
            startKeyLabel = startLabel,
            startDistribution = touch.startDistribution,
            pathLetters = pathLetters,
            intentPathLetters = normalized.intentPathLetters,
            pathLettersRaw = raw.pathLetters,
            endKeyLabel = endLabel,
            beatCount = beatCountEffective,
            beatCountRaw = raw.beatCountRaw,
            classifiedBeats = mapClassifiedBeats(raw.classifiedBeats),
            straightLine = straight,
            maxWordLength = maxWordLength,
            normalized = HeatmapPathLettersNormalize_v13.Normalized(
                letters = normalized.letters,
                intentPathLetters = normalized.intentPathLetters,
                dwellHints = normalized.dwellHints,
            ),
            touchedLetters = touch.touched,
            touchCounts = touch.counts,
            rejectedTouchLetters = touch.rejectedTouchLetters,
            strokeOrderLetters = touch.orderedLetters,
            transitKeys = intent.transitKeys,
            doublePrefixIndices = doublePrefixIndices,
            kinematics = kinematics,
            intent = mapIntent(intent),
            strokeTrace = trace,
        )
    }

    private fun mapIntent(intent: HeatmapSwipeIntentClassifier_v2.Result): HeatmapSwipeIntentClassifier_v1.Result =
        HeatmapSwipeIntentClassifier_v1.Result(
            segments = intent.segments.map { seg ->
                HeatmapSwipeIntentClassifier_v1.ClassifiedSegment(
                    kind = seg.kind,
                    startIndex = seg.startIndex,
                    endIndex = seg.endIndex,
                    promotedLabel = seg.promotedLabel,
                    transitLabels = seg.transitLabels,
                )
            },
            visitOrder = intent.visitOrder,
            transitKeys = intent.transitKeys,
            intentLetters = intent.intentLetters,
            startDistribution = intent.startDistribution,
            liftLabel = intent.liftLabel,
        )

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

    private fun pickStart(
        touch: String?,
        distribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        letters: List<String>,
        touched: Set<String>,
    ): String? =
        touch?.takeIf { it.isNotEmpty() }
            ?: HeatmapKeyLikelihood_v6.primaryStartLabel(distribution)
            ?: letters.firstOrNull { it in touched }

    private fun pickEnd(lift: String?, letters: List<String>, touched: Set<String>): String? =
        lift?.takeIf { it in touched } ?: letters.lastOrNull { it in touched }
}

