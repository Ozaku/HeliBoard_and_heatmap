// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.heatmap.learning

import android.content.Context
import helium314.keyboard.latin.utils.Log
import org.json.JSONObject
import java.io.File

object HeatmapUserProfile_v1 {
    private const val TAG = "HeatmapUserProfile"
    private const val FILE_NAME = "heatmap_user_profile_v1.json"

    // anchorStr -> (word -> count)
    private val shapeToWordCounts = HashMap<String, HashMap<String, Int>>()
    
    // word -> global boost count
    private val globalWordCounts = HashMap<String, Int>()

    @JvmStatic
    fun onChainResolved(context: Context, chain: HeatmapSwipeCorrectionChain_v2.ResolvedChain) {
        val finalWord = chain.finalWord.lowercase()
        var updated = false
        
        // Global boost
        globalWordCounts[finalWord] = (globalWordCounts[finalWord] ?: 0) + 1
        updated = true

        // Shape-specific boost
        for (attempt in chain.attempts) {
            if (attempt.outcome != WordSessionOutcome_v1.KEPT_IN_FIELD) {
                val geo = attempt.geometry
                if (geo != null && geo.cornerPoints.isNotEmpty()) {
                    val anchorStr = geo.cornerPoints.joinToString("") { it.label ?: "" }
                    if (anchorStr.isNotEmpty()) {
                        val wordMap = shapeToWordCounts.getOrPut(anchorStr) { HashMap() }
                        wordMap[finalWord] = (wordMap[finalWord] ?: 0) + 1
                    }
                }
            }
        }

        if (updated) {
            save(context)
        }
    }

    @JvmStatic
    fun getShapeBoost(anchorStr: String, word: String): Int {
        return shapeToWordCounts[anchorStr]?.get(word.lowercase()) ?: 0
    }

    @JvmStatic
    fun getGlobalBoost(word: String): Int {
        return globalWordCounts[word.lowercase()] ?: 0
    }

    @JvmStatic
    fun clear(context: Context) {
        shapeToWordCounts.clear()
        globalWordCounts.clear()
        save(context)
    }

    private fun save(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val root = JSONObject()
            val shapeObj = JSONObject()
            for ((shape, words) in shapeToWordCounts) {
                val wordsObj = JSONObject()
                for ((w, c) in words) wordsObj.put(w, c)
                shapeObj.put(shape, wordsObj)
            }
            root.put("shapeToWordCounts", shapeObj)
            
            val globalObj = JSONObject()
            for ((w, c) in globalWordCounts) globalObj.put(w, c)
            root.put("globalWordCounts", globalObj)
            
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save profile", e)
        }
    }

    private var profileLoaded = false

    @JvmStatic
    fun load(context: Context) {
        if (profileLoaded) return
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                profileLoaded = true
                return
            }
            val root = JSONObject(file.readText())
            
            shapeToWordCounts.clear()
            val shapeObj = root.optJSONObject("shapeToWordCounts")
            if (shapeObj != null) {
                for (shape in shapeObj.keys()) {
                    val wordsObj = shapeObj.getJSONObject(shape)
                    val wordsMap = HashMap<String, Int>()
                    for (w in wordsObj.keys()) wordsMap[w] = wordsObj.getInt(w)
                    shapeToWordCounts[shape] = wordsMap
                }
            }
            
            globalWordCounts.clear()
            val globalObj = root.optJSONObject("globalWordCounts")
            if (globalObj != null) {
                for (w in globalObj.keys()) {
                    globalWordCounts[w] = globalObj.getInt(w)
                }
            }
            profileLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load profile", e)
        }
    }
}
