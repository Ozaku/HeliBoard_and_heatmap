// SPDX-License-Identifier: GPL-3.0-only

// ai-note: v3 — global synchronized stash so decode thread sees same pointers as IME commit

package helium314.keyboard.heatmap.learning

import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathCapture_v3 {

    private const val COPY_CAPACITY = 256

    data class Stash(
        val pointers: InputPointers,
        val keyboardWidth: Int,
        val keyboardHeight: Int,
    )

    private val lock = Any()

    @Volatile
    private var holder: Stash? = null

    @JvmStatic
    fun stashFromIme(source: InputPointers, batchMode: Boolean) {
        synchronized(lock) {
            if (!batchMode) {
                holder = null
                return
            }
            val keyboard = KeyboardSwitcher.getInstance().keyboard
            if (keyboard == null) {
                holder = null
                return
            }
            val copy = InputPointers(COPY_CAPACITY.coerceAtLeast(source.pointerSize))
            copy.copy(source)
            holder = Stash(
                pointers = copy,
                keyboardWidth = keyboard.mOccupiedWidth,
                keyboardHeight = keyboard.mOccupiedHeight,
            )
        }
    }

    @JvmStatic
    fun peek(): Stash? = holder

    @JvmStatic
    fun peekPointerSize(): Int = holder?.pointers?.pointerSize ?: 0

    @JvmStatic
    fun consume(): Stash? = synchronized(lock) {
        val stash = holder
        holder = null
        stash
    }
}
