// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v1 — pointer-index ordered corner/dwell path; transit keys excluded from lookup

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

object HeatmapSwipeOrderedCornerPath_v1 {

    private enum class Source { START, CORNER, END, DWELL, LIFT }

    private data class PromotedKey(
        val index: Int,
        val label: String,
        val source: Source,
    )

    @JvmStatic
    fun build(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        classifiedBeats: List<HeatmapGeometryClassifier_v2.ClassifiedBeat>,
        dwellSegments: List<HeatmapSwipeStrokeKinematics_v1.DwellSegment>,
        startDistribution: List<HeatmapKeyLikelihood_v6.LabelWeight>,
        liftLabel: String?,
    ): List<String> {
        val promoted = LinkedHashMap<Int, PromotedKey>()
        for (beat in classifiedBeats.sortedBy { it.index }) {
            if (beat.kind == HeatmapGeometryClassifier_v2.BeatKind.BRIDGE) continue
            val label = beat.keyLabel
                ?: HeatmapKeyLikelihood_v6.bestLabelAt(layout, beat.x, beat.y)
                ?: continue
            val source = when (beat.kind) {
                HeatmapGeometryClassifier_v2.BeatKind.START -> Source.START
                HeatmapGeometryClassifier_v2.BeatKind.END -> Source.END
                else -> Source.CORNER
            }
            mergePromoted(promoted, beat.index, label, source)
        }
        for (dwell in dwellSegments.sortedBy { it.startIndex }) {
            val label = dwell.dominantLabel ?: continue
            mergePromoted(promoted, dwell.startIndex, label, Source.DWELL)
        }
        val startPrimary = HeatmapKeyLikelihood_v6.primaryStartLabel(startDistribution)
        if (startPrimary != null) {
            val minIdx = promoted.keys.minOrNull() ?: 0
            if (promoted[minIdx]?.label != startPrimary) {
                mergePromoted(promoted, minIdx, startPrimary, Source.START)
            }
        }
        if (!liftLabel.isNullOrEmpty()) {
            val maxIdx = promoted.keys.maxOrNull() ?: 0
            if (promoted[maxIdx]?.label != liftLabel) {
                mergePromoted(promoted, maxIdx + 1, liftLabel, Source.LIFT)
            }
        }
        val labels = promoted.entries
            .sortedBy { it.key }
            .map { it.value.label }
        return HeatmapPathLettersNormalize_v1.collapseConsecutiveDuplicates(labels)
    }

    private fun mergePromoted(
        promoted: LinkedHashMap<Int, PromotedKey>,
        index: Int,
        label: String,
        source: Source,
    ) {
        val existing = promoted[index]
        if (existing == null) {
            promoted[index] = PromotedKey(index, label, source)
            return
        }
        val existingRank = sourceRank(existing.source)
        val newRank = sourceRank(source)
        if (newRank >= existingRank) {
            promoted[index] = PromotedKey(index, label, source)
        }
    }

    private fun sourceRank(source: Source): Int = when (source) {
        Source.START -> 4
        Source.LIFT -> 4
        Source.END -> 3
        Source.DWELL -> 2
        Source.CORNER -> 1
    }
}
