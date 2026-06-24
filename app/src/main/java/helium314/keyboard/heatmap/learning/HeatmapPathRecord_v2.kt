// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v2 — path capture v3 stash type — persist path bucket + alignment offsets on gated swipe commit

package helium314.keyboard.heatmap.learning

import android.database.sqlite.SQLiteDatabase
import helium314.keyboard.latin.utils.Log

object HeatmapPathRecord_v2 {

    private const val TAG = "HeatmapInstr"

    fun recordSwipeCommit(
        db: SQLiteDatabase,
        locale: String,
        layoutHash: String,
        inputMode: String,
        stash: HeatmapPathCapture_v3.Stash,
        layout: HeatmapCoordinateMap_v1.Snapshot,
    ) {
        val pointers = stash.pointers
        val size = pointers.pointerSize
        val signature = HeatmapPathSignature_v1.compute(
            xCoords = pointers.xCoordinates,
            yCoords = pointers.yCoordinates,
            pointerSize = size,
            keyboardWidth = stash.keyboardWidth,
            keyboardHeight = stash.keyboardHeight,
        ) ?: return
        HeatmapPathBucketStore_v1.bumpBucket(
            db = db,
            locale = locale,
            layoutHash = layoutHash,
            inputMode = inputMode,
            signatureHash = signature.signatureHash,
            polylineBlob = signature.polylineBlob,
        )
        HeatmapAlignmentOffsetStore_v1.bumpFromResampledPoints(
            db = db,
            locale = locale,
            layoutHash = layoutHash,
            inputMode = inputMode,
            layout = layout,
            points = signature.resampledPoints,
        )
        Log.i(TAG, "path bucket ${signature.signatureHash} pts=$size mode=$inputMode")
    }
}
