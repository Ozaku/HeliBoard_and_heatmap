// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 10 — resample swipe polyline, 32-byte blob, ps_ signature hash

package helium314.keyboard.heatmap.learning

import java.security.MessageDigest

object HeatmapPathSignature_v1 {

    const val SAMPLE_COUNT = 16

    const val POLYLINE_BYTES = SAMPLE_COUNT * 2

    data class Result(
        val signatureHash: String,
        val polylineBlob: ByteArray,
        val resampledPoints: List<Point>,
    ) {
        data class Point(val x: Int, val y: Int)
    }

    fun compute(
        xCoords: IntArray,
        yCoords: IntArray,
        pointerSize: Int,
        keyboardWidth: Int,
        keyboardHeight: Int,
    ): Result? {
        if (pointerSize < 2) return null
        val w = keyboardWidth.coerceAtLeast(1)
        val h = keyboardHeight.coerceAtLeast(1)
        val resampled = resampleEvenly(xCoords, yCoords, pointerSize, SAMPLE_COUNT)
        val blob = ByteArray(POLYLINE_BYTES)
        for (i in 0 until SAMPLE_COUNT) {
            val p = resampled[i]
            blob[i] = normalizeAxis(p.x, w)
            blob[SAMPLE_COUNT + i] = normalizeAxis(p.y, h)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(blob)
        val hex16 = digest.take(8).joinToString("") { b -> "%02x".format(b) }
        return Result(
            signatureHash = "ps_$hex16",
            polylineBlob = blob,
            resampledPoints = resampled,
        )
    }

    private fun normalizeAxis(value: Int, span: Int): Byte =
        ((value.coerceIn(0, span) * 255L / span).toInt().coerceIn(0, 255)).toByte()

    private fun resampleEvenly(
        xs: IntArray,
        ys: IntArray,
        size: Int,
        count: Int,
    ): List<Result.Point> {
        if (size <= 1) {
            val x = xs[0]
            val y = ys[0]
            return List(count) { Result.Point(x, y) }
        }
        return (0 until count).map { i ->
            val t = if (count == 1) 0f else i.toFloat() / (count - 1)
            val idx = (t * (size - 1)).toInt().coerceIn(0, size - 1)
            Result.Point(xs[idx], ys[idx])
        }
    }
}
