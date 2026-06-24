// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.swipe



import helium314.keyboard.heatmap.learning.HeatmapCoordinateMap_v1

import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue

import org.junit.Test



class HeatmapKeyLikelihood_v1Test {



    private fun layout(): HeatmapCoordinateMap_v1.Snapshot {

        val keys = listOf(

            key("a", 0, 0, 100, 100),

            key("b", 200, 0, 300, 100),

            key("z", 0, 300, 100, 400),

        )

        return HeatmapCoordinateMap_v1.Snapshot(

            localeTag = "en",

            layoutSetExtra = "qwerty",

            mainLayoutName = "main",

            elementId = 1,

            keyboardWidth = 400,

            keyboardHeight = 500,

            layoutHash = "test",

            keys = keys,

        )

    }



    private fun key(label: String, l: Int, t: Int, r: Int, b: Int) =

        HeatmapCoordinateMap_v1.KeyBoundsEntry(label, label[0].code, l, t, r, b)



    @Test

    fun insideKeyIsCertain() {

        val like = HeatmapKeyLikelihood_v1.likelihoodAt(layout(), 50, 50, key("a", 0, 0, 100, 100))

        assertEquals(1.0, like, 0.001)

    }



    @Test

    fun farRowKeyIsZero() {

        val like = HeatmapKeyLikelihood_v1.likelihoodAt(layout(), 50, 50, key("z", 0, 300, 100, 400))

        assertEquals(0.0, like, 0.001)

    }



    @Test

    fun neighborHasPartialLikelihood() {

        val like = HeatmapKeyLikelihood_v1.likelihoodAt(layout(), 110, 50, key("b", 200, 0, 300, 100))

        assertTrue(like in 0.01..0.99)

    }

}


