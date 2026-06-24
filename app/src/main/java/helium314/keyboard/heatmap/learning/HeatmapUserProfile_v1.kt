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

    data class SpatialOffset(var xOffset: Float = 0f, var yOffset: Float = 0f, var count: Int = 0)
    
    // char -> spatial offset
    private val keyOffsets = HashMap<Char, SpatialOffset>()

    @JvmStatic
    fun onChainResolved(context: Context, chain: HeatmapSwipeCorrectionChain_v2.ResolvedChain) {
        val finalWord = chain.finalWord.lowercase()
        var updated = false
        
        // Global boost
        globalWordCounts[finalWord] = (globalWordCounts[finalWord] ?: 0) + 1
        updated = true

        // Shape-specific boost & Spatial Offsets
        for (attempt in chain.attempts) {
            val geo = attempt.geometry
            if (geo != null && geo.cornerPoints.isNotEmpty()) {
                if (attempt.outcome != WordSessionOutcome_v1.KEPT_IN_FIELD) {
                    val anchorStr = geo.cornerPoints.joinToString("") { it.label ?: "" }
                    if (anchorStr.isNotEmpty()) {
                        val wordMap = shapeToWordCounts.getOrPut(anchorStr) { HashMap() }
                        wordMap[finalWord] = (wordMap[finalWord] ?: 0) + 1
                    }
                }
                
                // Spatial offsets from successful or corrected swipes
                // We only learn offsets if the number of anchors matches the collapsed word length,
                // meaning we have high confidence in the 1:1 mapping of anchor to intended letter.
                val collapsedFinal = collapseAdjacent(finalWord.filter { it.isLetter() }.map { it.lowercaseChar() })
                if (collapsedFinal.size == geo.cornerPoints.size) {
                    for (i in geo.cornerPoints.indices) {
                        val intendedChar = collapsedFinal[i]
                        val anchor = geo.cornerPoints[i]
                        if (anchor.keyCenterX >= 0 && anchor.keyCenterY >= 0) {
                            val dx = anchor.x - anchor.keyCenterX.toFloat()
                            val dy = anchor.y - anchor.keyCenterY.toFloat()
                            val offset = keyOffsets.getOrPut(intendedChar) { SpatialOffset() }
                            offset.xOffset = (offset.xOffset * offset.count + dx) / (offset.count + 1)
                            offset.yOffset = (offset.yOffset * offset.count + dy) / (offset.count + 1)
                            offset.count++
                        }
                    }
                }
            }
        }

        if (updated) {
            save(context)
        }
    }

    private fun collapseAdjacent(keys: List<Char>): List<Char> {
        if (keys.size < 2) return keys
        val out = ArrayList<Char>(keys.size)
        var last: Char? = null
        for (k in keys) {
            if (k != last) out.add(k)
            last = k
        }
        return out
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
    fun getOffsetX(ch: Char): Float = keyOffsets[ch.lowercaseChar()]?.xOffset ?: 0f

    @JvmStatic
    fun getOffsetY(ch: Char): Float = keyOffsets[ch.lowercaseChar()]?.yOffset ?: 0f

    @JvmStatic
    fun clear(context: Context) {
        shapeToWordCounts.clear()
        globalWordCounts.clear()
        keyOffsets.clear()
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
            
            val offsetsObj = JSONObject()
            for ((ch, offset) in keyOffsets) {
                val oObj = JSONObject()
                oObj.put("x", offset.xOffset.toDouble())
                oObj.put("y", offset.yOffset.toDouble())
                oObj.put("count", offset.count)
                offsetsObj.put(ch.toString(), oObj)
            }
            root.put("keyOffsets", offsetsObj)
            
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

            keyOffsets.clear()
            val offsetsObj = root.optJSONObject("keyOffsets")
            if (offsetsObj != null) {
                for (chStr in offsetsObj.keys()) {
                    if (chStr.isNotEmpty()) {
                        val oObj = offsetsObj.getJSONObject(chStr)
                        keyOffsets[chStr[0]] = SpatialOffset(
                            xOffset = oObj.optDouble("x", 0.0).toFloat(),
                            yOffset = oObj.optDouble("y", 0.0).toFloat(),
                            count = oObj.optInt("count", 0)
                        )
                    }
                }
            }

            profileLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load profile", e)
        }
    }
}
