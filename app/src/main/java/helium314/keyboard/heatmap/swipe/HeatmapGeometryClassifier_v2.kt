// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 3 step 15o — same-key wiggle = CORNER; lower bridge threshold

package helium314.keyboard.heatmap.swipe

import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1
import helium314.keyboard.latin.common.InputPointers
import kotlin.math.abs
import kotlin.math.atan2

object HeatmapGeometryClassifier_v2 {

    enum class BeatKind {
        START,
        CORNER,
        END,
        BRIDGE,
    }

    data class ClassifiedBeat(
        val x: Int,
        val y: Int,
        val index: Int,
        val kind: BeatKind,
        val keyLabel: String?,
    )

    private const val ARC_ANGLE_THRESH_DEG = 8.0
    private const val MIN_BRIDGE_SPAN_PX = 36

    fun classify(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        rawBeats: HeatmapSwipeBeat_v2.Result,
    ): List<ClassifiedBeat> = classifyPoints(layout, pointers, rawBeats.beatPoints)

    fun classify(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        rawBeats: HeatmapSwipeBeat_v3.Result,
    ): List<ClassifiedBeat> = classifyPoints(layout, pointers, rawBeats.beatPoints.map {
        HeatmapSwipeBeat_v2.BeatPoint(it.x, it.y, it.index)
    })

    private fun classifyPoints(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        pointers: InputPointers,
        beatPoints: List<HeatmapSwipeBeat_v2.BeatPoint>,
    ): List<ClassifiedBeat> {
        if (beatPoints.isEmpty()) return emptyList()
        val rawBeats = HeatmapSwipeBeat_v2.Result(beatPoints.size, beatPoints)
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val labels = rawBeats.beatPoints.map { beat ->
            HeatmapKeyLikelihood_v5.bestLabelAt(layout, beat.x, beat.y)
        }
        val out = ArrayList<ClassifiedBeat>(rawBeats.beatPoints.size)
        rawBeats.beatPoints.forEachIndexed { i, beat ->
            val kind = when (i) {
                0 -> BeatKind.START
                rawBeats.beatPoints.lastIndex -> BeatKind.END
                else -> classifyMiddleBeat(
                    layout, xs, ys, beat.index, rawBeats.beatPoints, i, labels,
                )
            }
            if (kind != BeatKind.BRIDGE) {
                out.add(ClassifiedBeat(beat.x, beat.y, beat.index, kind, labels[i]))
            }
        }
        if (out.isEmpty() || out.first().kind != BeatKind.START) {
            val first = rawBeats.beatPoints.first()
            out.add(0, ClassifiedBeat(first.x, first.y, first.index, BeatKind.START, labels.firstOrNull()))
        }
        val lastRaw = rawBeats.beatPoints.last()
        if (out.lastOrNull()?.index != lastRaw.index) {
            out.add(
                ClassifiedBeat(
                    lastRaw.x, lastRaw.y, lastRaw.index, BeatKind.END,
                    labels.lastOrNull(),
                ),
            )
        }
        return out
    }

    private fun classifyMiddleBeat(
        layout: HeatmapCoordinateMap_v1.Snapshot,
        xs: IntArray,
        ys: IntArray,
        beatIndex: Int,
        allBeats: List<HeatmapSwipeBeat_v2.BeatPoint>,
        beatPos: Int,
        labels: List<String?>,
    ): BeatKind {
        val prevLabel = labels.getOrNull(beatPos - 1)
        val curLabel = labels.getOrNull(beatPos)
        if (!prevLabel.isNullOrEmpty() && prevLabel == curLabel) {
            return BeatKind.CORNER
        }
        val prev = allBeats[beatPos - 1]
        val next = allBeats.getOrNull(beatPos + 1) ?: return BeatKind.CORNER
        val span = distance(prev.x, prev.y, next.x, next.y)
        if (span < MIN_BRIDGE_SPAN_PX) return BeatKind.CORNER
        val maxAngle = maxAngleChangeDeg(xs, ys, prev.index, beatIndex, next.index)
        if (beatIndex < 0 || beatIndex >= xs.size || beatIndex >= ys.size) return BeatKind.CORNER
        val onKey = layout.keyAt(xs[beatIndex], ys[beatIndex]) != null
        if (maxAngle < ARC_ANGLE_THRESH_DEG && !onKey) return BeatKind.BRIDGE
        return BeatKind.CORNER
    }

    private fun maxAngleChangeDeg(xs: IntArray, ys: IntArray, i0: Int, i1: Int, i2: Int): Double {
        if (i0 >= i1 || i1 >= i2) return 180.0
        val d0 = directionDeg(xs[i0], ys[i0], xs[i1], ys[i1])
        val d1 = directionDeg(xs[i1], ys[i1], xs[i2], ys[i2])
        var delta = abs(d1 - d0) % 360.0
        if (delta > 180.0) delta = 360.0 - delta
        return delta
    }

    private fun directionDeg(x0: Int, y0: Int, x1: Int, y1: Int): Double =
        Math.toDegrees(atan2((y1 - y0).toDouble(), (x1 - x0).toDouble()))

    private fun distance(x0: Int, y0: Int, x1: Int, y1: Int): Int {
        val dx = x1 - x0
        val dy = y1 - y0
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
    }
}
