// SPDX-License-Identifier: GPL-3.0-only



// ai-note: Block 3 step 13c+d+e — beats + likelihood labels + bridge filter



package helium314.keyboard.heatmap.swipe



import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

import helium314.keyboard.keyboard.Keyboard

import helium314.keyboard.latin.common.InputPointers



object HeatmapSwipeSegmentInfer_v2 {



    data class Result(

        val startKeyLabel: String?,

        val pathLetters: List<String>,

        val endKeyLabel: String?,

        val beatCount: Int,

        val classifiedBeats: List<HeatmapGeometryClassifier_v1.ClassifiedBeat>,

    )



    fun infer(keyboard: Keyboard, pointers: InputPointers): Result? {

        val layout = HeatmapCoordinateMap_v1.fromKeyboard(keyboard) ?: return null

        val rawBeats = HeatmapSwipeBeat_v1.detect(pointers)

        if (rawBeats.beatPoints.isEmpty()) return null

        val classified = HeatmapGeometryClassifier_v1.classify(layout, pointers, rawBeats)

        val labels = classified.mapNotNull { beat ->

            HeatmapKeyLikelihood_v1.bestLabelAt(layout, beat.x, beat.y)

        }

        if (labels.isEmpty()) return null

        return Result(

            startKeyLabel = labels.firstOrNull(),

            pathLetters = labels,

            endKeyLabel = labels.lastOrNull(),

            beatCount = classified.size,

            classifiedBeats = classified,

        )

    }

}


