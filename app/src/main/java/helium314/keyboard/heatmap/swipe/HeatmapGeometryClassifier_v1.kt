// SPDX-License-Identifier: GPL-3.0-only



// ai-note: Block 3 step 13e — wide-arc bridge vs corner beat; filters non-letter gaps per 49_ B2



package helium314.keyboard.heatmap.swipe



import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

import helium314.keyboard.latin.common.InputPointers

import kotlin.math.abs

import kotlin.math.atan2



object HeatmapGeometryClassifier_v1 {



    enum class BeatKind {

        START,

        CORNER,

        END,

        /** Gentle arc across key gap — no letter at this beat. */

        BRIDGE,

    }



    data class ClassifiedBeat(

        val x: Int,

        val y: Int,

        val index: Int,

        val kind: BeatKind,

    )



    private const val ARC_ANGLE_THRESH_DEG = 12.0

    private const val MIN_BRIDGE_SPAN_PX = 28



    fun classify(

        layout: HeatmapCoordinateMap_v1.Snapshot,

        pointers: InputPointers,

        rawBeats: HeatmapSwipeBeat_v1.Result,

    ): List<ClassifiedBeat> {

        if (rawBeats.beatPoints.isEmpty()) return emptyList()

        val xs = pointers.xCoordinates

        val ys = pointers.yCoordinates

        val out = ArrayList<ClassifiedBeat>(rawBeats.beatPoints.size)

        rawBeats.beatPoints.forEachIndexed { i, beat ->

            val kind = when (i) {

                0 -> BeatKind.START

                rawBeats.beatPoints.lastIndex -> BeatKind.END

                else -> classifyMiddleBeat(layout, xs, ys, beat.index, rawBeats.beatPoints, i)

            }

            if (kind != BeatKind.BRIDGE) {

                out.add(ClassifiedBeat(beat.x, beat.y, beat.index, kind))

            }

        }

        if (out.isEmpty() || out.first().kind != BeatKind.START) {

            val first = rawBeats.beatPoints.first()

            out.add(0, ClassifiedBeat(first.x, first.y, first.index, BeatKind.START))

        }

        val lastRaw = rawBeats.beatPoints.last()

        if (out.lastOrNull()?.index != lastRaw.index) {

            out.add(ClassifiedBeat(lastRaw.x, lastRaw.y, lastRaw.index, BeatKind.END))

        }

        return out

    }



    private fun classifyMiddleBeat(

        layout: HeatmapCoordinateMap_v1.Snapshot,

        xs: IntArray,

        ys: IntArray,

        beatIndex: Int,

        allBeats: List<HeatmapSwipeBeat_v1.BeatPoint>,

        beatPos: Int,

    ): BeatKind {

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

        return sqrt((dx * dx + dy * dy).toDouble()).toInt()

    }



    private fun sqrt(v: Double) = kotlin.math.sqrt(v)

}


