// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.14 — commit-path latency, heap on flush, prefix-branch stub (Block 3+)

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.content.SharedPreferences

import android.os.SystemClock

import helium314.keyboard.latin.utils.Log

import java.util.Arrays

import kotlin.math.roundToInt



object HeatmapMetricsRecorder_v1 {

    private const val TAG = "HeatmapInstr"

    private const val RING_SIZE = 32

    private const val PREF_COMMIT_COUNT = "heatmap_metrics_commit_count"

    private const val PREF_COMMIT_SUM_MS = "heatmap_metrics_commit_sum_ms"

    private const val PREF_COMMIT_MAX_MS = "heatmap_metrics_commit_max_ms"

    private const val PREF_COMMIT_LAST_MS = "heatmap_metrics_commit_last_ms"

    private const val PREF_HEAP_USED_MB = "heatmap_metrics_heap_used_mb"

    private const val PREF_HEAP_MAX_MB = "heatmap_metrics_heap_max_mb"

    private const val PREF_HEAP_AT_MS = "heatmap_metrics_heap_at_ms"

    private const val PREF_PREFIX_BRANCHES = "heatmap_metrics_prefix_branches"

    private const val PREF_RING = "heatmap_metrics_commit_ring"



    private val ringLock = Any()

    private val ringMs = LongArray(RING_SIZE)

    private var ringCount = 0

    private var ringIndex = 0



    /** Call at end of heatmap onWordCommitted work (after gate passed). */

    @JvmStatic

    fun onCommitPathFinished(context: Context, durationMs: Long) {

        val ms = durationMs.coerceAtLeast(0L)

        synchronized(ringLock) {

            ringMs[ringIndex] = ms

            ringIndex = (ringIndex + 1) % RING_SIZE

            ringCount = (ringCount + 1).coerceAtMost(RING_SIZE)

        }

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val count = prefs.getInt(PREF_COMMIT_COUNT, 0) + 1

        val sum = prefs.getLong(PREF_COMMIT_SUM_MS, 0L) + ms

        val max = maxOf(prefs.getLong(PREF_COMMIT_MAX_MS, 0L), ms)

        HeatmapCrossProcessPrefs_v2.editCommit(context) {

            putInt(PREF_COMMIT_COUNT, count)

            putLong(PREF_COMMIT_SUM_MS, sum)

            putLong(PREF_COMMIT_MAX_MS, max)

            putLong(PREF_COMMIT_LAST_MS, ms)

            putString(PREF_RING, encodeRing())

        }

        Log.i(TAG, "metrics commit path ${ms}ms avg=${averageMs(count, sum)} p95~${percentileMs(95)}")

    }



    /** Sample JVM heap when a flush runs (IME process). */

    @JvmStatic

    fun onFlush(context: Context) {

        val runtime = Runtime.getRuntime()

        val usedMb = bytesToMb(runtime.totalMemory() - runtime.freeMemory())

        val maxMb = bytesToMb(runtime.maxMemory())

        HeatmapCrossProcessPrefs_v2.editCommit(context) {

            putInt(PREF_HEAP_USED_MB, usedMb)

            putInt(PREF_HEAP_MAX_MB, maxMb)

            putLong(PREF_HEAP_AT_MS, System.currentTimeMillis())

        }

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        Log.i(

            TAG,

            "metrics flush heap=${usedMb}MB/${maxMb}MB commits=${prefs.getInt(PREF_COMMIT_COUNT, 0)} " +

                "lastCommit=${prefs.getLong(PREF_COMMIT_LAST_MS, 0L)}ms p95~${percentileMs(95)} " +

                "prefixBranches=${prefs.getInt(PREF_PREFIX_BRANCHES, 0)} (stub)",

        )

    }



    /** Block 3 swipe engine will call when narrowing dictionary prefix trie. */

    @JvmStatic

    fun addPrefixBranchStub(context: Context, count: Int = 1) {

        if (count <= 0) return

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val total = prefs.getInt(PREF_PREFIX_BRANCHES, 0) + count

        HeatmapCrossProcessPrefs_v2.editCommit(context) {

            putInt(PREF_PREFIX_BRANCHES, total)

        }

    }



    fun formatStatusBlock(context: Context): String {

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val count = prefs.getInt(PREF_COMMIT_COUNT, 0)

        val sum = prefs.getLong(PREF_COMMIT_SUM_MS, 0L)

        val last = prefs.getLong(PREF_COMMIT_LAST_MS, 0L)

        val max = prefs.getLong(PREF_COMMIT_MAX_MS, 0L)

        val heapUsed = prefs.getInt(PREF_HEAP_USED_MB, 0)

        val heapMax = prefs.getInt(PREF_HEAP_MAX_MB, 0)

        val branches = prefs.getInt(PREF_PREFIX_BRANCHES, 0)

        val p95 = percentileFromEncodedRing(prefs.getString(PREF_RING, null), 95)

        return buildString {

            append("\n\n— Metrics (beta 1.14) —")

            append("\nCommit path samples: ").append(count)

            if (count > 0) {

                append("\nLast / avg / max: ").append(last).append(" / ")

                    .append(averageMs(count, sum)).append(" / ").append(max).append(" ms")

                append("\nApprox p95: ").append(p95).append(" ms")

            } else {

                append("\n(No commits recorded yet.)")

            }

            if (heapMax > 0) {

                append("\nHeap at last flush: ").append(heapUsed).append(" / ").append(heapMax).append(" MB")

            }

            append("\nPrefix branches (stub): ").append(branches)

        }

    }



    fun readSnapshot(prefs: SharedPreferences): MetricsSnapshot = MetricsSnapshot(

        commitCount = prefs.getInt(PREF_COMMIT_COUNT, 0),

        commitLastMs = prefs.getLong(PREF_COMMIT_LAST_MS, 0L),

        commitAvgMs = averageMs(prefs.getInt(PREF_COMMIT_COUNT, 0), prefs.getLong(PREF_COMMIT_SUM_MS, 0L)),

        commitMaxMs = prefs.getLong(PREF_COMMIT_MAX_MS, 0L),

        commitP95Ms = percentileFromEncodedRing(prefs.getString(PREF_RING, null), 95),

        heapUsedMb = prefs.getInt(PREF_HEAP_USED_MB, 0),

        heapMaxMb = prefs.getInt(PREF_HEAP_MAX_MB, 0),

        prefixBranchStubCount = prefs.getInt(PREF_PREFIX_BRANCHES, 0),

    )



    data class MetricsSnapshot(

        val commitCount: Int,

        val commitLastMs: Long,

        val commitAvgMs: Long,

        val commitMaxMs: Long,

        val commitP95Ms: Long,

        val heapUsedMb: Int,

        val heapMaxMb: Int,

        val prefixBranchStubCount: Int,

    )



    private fun averageMs(count: Int, sum: Long): Long =

        if (count <= 0) 0L else (sum / count)



    private fun percentileMs(p: Int): Long = synchronized(ringLock) {

        if (ringCount <= 0) return 0L

        val copy = LongArray(ringCount)

        for (i in 0 until ringCount) {

            val idx = (ringIndex - ringCount + i + RING_SIZE) % RING_SIZE

            copy[i] = ringMs[idx]

        }

        Arrays.sort(copy)

        val rank = ((p / 100.0) * (ringCount - 1)).roundToInt().coerceIn(0, ringCount - 1)

        copy[rank]

    }



    private fun encodeRing(): String = synchronized(ringLock) {

        if (ringCount <= 0) return ""

        buildString {

            for (i in 0 until ringCount) {

                if (i > 0) append(',')

                val idx = (ringIndex - ringCount + i + RING_SIZE) % RING_SIZE

                append(ringMs[idx])

            }

        }

    }



    private fun percentileFromEncodedRing(encoded: String?, p: Int): Long {

        if (encoded.isNullOrBlank()) return 0L

        val values = encoded.split(',').mapNotNull { it.toLongOrNull() }.toLongArray()

        if (values.isEmpty()) return 0L

        Arrays.sort(values)

        val rank = ((p / 100.0) * (values.size - 1)).roundToInt().coerceIn(0, values.size - 1)

        return values[rank]

    }



    private fun bytesToMb(bytes: Long): Int =

        (bytes / (1024 * 1024)).toInt().coerceAtLeast(0)

}

