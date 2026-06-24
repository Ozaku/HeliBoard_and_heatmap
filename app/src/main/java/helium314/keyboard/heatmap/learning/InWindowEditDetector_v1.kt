// SPDX-License-Identifier: GPL-3.0-only

// ai-note: step 1.12 — selection + field probe stub; does not assign WordSession.finalText yet

package helium314.keyboard.heatmap.learning



import android.content.Context

import android.content.SharedPreferences

import helium314.keyboard.latin.utils.Log

import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.atomic.AtomicReference



object InWindowEditDetector_v1 {

    private const val TAG = "HeatmapInstr"

    private const val PREF_SELECTION_EVENTS = "heatmap_selection_edit_events"

    private const val PREF_LAST_PROBE = "heatmap_last_field_probe"

    private const val PREF_LAST_FINGERPRINT = "heatmap_last_field_fingerprint"

    private const val PREF_PENDING_SYNC = "heatmap_pending_finaltext_sync"



    private val lastFingerprint = AtomicReference<Int?>(null)

    private val lastSelectionSignature = AtomicInteger(0)



    @JvmStatic

    fun onSelectionChanged(

        context: Context,

        oldSelStart: Int,

        oldSelEnd: Int,

        newSelStart: Int,

        newSelEnd: Int,

    ) {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return

        if (oldSelStart == newSelStart && oldSelEnd == newSelEnd) return

        val sig = (oldSelStart xor newSelStart) + (oldSelEnd shl 4 xor newSelEnd)

        if (lastSelectionSignature.getAndSet(sig) == sig) return

        val total = HeatmapCrossProcessPrefs_v2.readPrefs(context).getInt(PREF_SELECTION_EVENTS, 0) + 1

        HeatmapCrossProcessPrefs_v2.editCommit(context) {

            putInt(PREF_SELECTION_EVENTS, total)

        }

        HeatmapWordSlotSession_v7.markEditSuspected()

        Log.i(TAG, "selection sel=$newSelStart..$newSelEnd (was $oldSelStart..$oldSelEnd)")

    }



    @JvmStatic

    fun onFlushFieldProbe(

        context: Context,

        snapshot: FieldTextSnapshot_v1?,

        pendingSyncSlots: Int,

    ) {

        if (!HeatmapLearningSettings_v2.isLearningEnabled(context)) return

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val probeLine = snapshot?.debugToken() ?: "skip=no_connection"

        val pending = pendingSyncSlots.coerceAtLeast(0)

        HeatmapCrossProcessPrefs_v2.editCommit(context) {

            putString(PREF_LAST_PROBE, probeLine)

            putInt(PREF_PENDING_SYNC, pending)

            snapshot?.takeIf { it.probed }?.let { putInt(PREF_LAST_FINGERPRINT, it.fingerprint) }

        }

        if (snapshot != null && snapshot.probed) {

            val prev = lastFingerprint.getAndSet(snapshot.fingerprint)

            if (prev != null && prev != snapshot.fingerprint) {

                Log.i(

                    TAG,

                    "fieldDiff stub changed fp=${snapshot.fingerprint} pendingFinalText=$pending " +

                        "before=${snapshot.beforeChars} after=${snapshot.afterChars}",

                )

            } else {

                Log.i(TAG, "fieldDiff stub unchanged fp=${snapshot.fingerprint} pendingFinalText=$pending")

            }

        } else {

            Log.i(TAG, "fieldDiff stub $probeLine pendingFinalText=$pending")

        }

    }



    fun formatStatusBlock(context: Context): String {

        val prefs = HeatmapCrossProcessPrefs_v2.readPrefs(context)

        val sel = prefs.getInt(PREF_SELECTION_EVENTS, 0)

        val probe = prefs.getString(PREF_LAST_PROBE, null) ?: "none yet"

        val pending = prefs.getInt(PREF_PENDING_SYNC, 0)

        val fp = prefs.getInt(PREF_LAST_FINGERPRINT, 0)

        return buildString {

            append("\n\n— In-window edit (beta 1.12) —")

            append("\nSelection moves: ").append(sel)

            append("\nPending finalText sync: ").append(pending)

            append("\nLast field probe: ").append(probe)

            if (fp != 0) append("\nLast fingerprint: ").append(fp)

            append("\n(finalText per slot not updated yet — stub only)")

        }

    }



    internal fun readSelectionEvents(prefs: SharedPreferences): Int =

        prefs.getInt(PREF_SELECTION_EVENTS, 0)

}

