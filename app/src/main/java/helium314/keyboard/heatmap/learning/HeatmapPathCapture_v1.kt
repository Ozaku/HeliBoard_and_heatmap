// SPDX-License-Identifier: GPL-3.0-only

// ai-note: Block 2 step 10 — stash InputPointers before WordComposer.commitWord clears batch state

package helium314.keyboard.heatmap.learning

import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.common.InputPointers

object HeatmapPathCapture_v1 {

    private const val COPY_CAPACITY = 256

    data class Stash(
        val pointers: InputPointers,
        val keyboardWidth: Int,
        val keyboardHeight: Int,
    )

    private val holder = ThreadLocal<Stash?>()

    @JvmStatic
    fun stashFromIme(source: InputPointers, batchMode: Boolean) {
        if (!batchMode) {
            holder.set(null)
            return
        }
        val keyboard = KeyboardSwitcher.getInstance().keyboard
        if (keyboard == null) {
            holder.set(null)
            return
        }
        val copy = InputPointers(COPY_CAPACITY.coerceAtLeast(source.pointerSize))
        copy.copy(source)
        holder.set(
            Stash(
                pointers = copy,
                keyboardWidth = keyboard.mOccupiedWidth,
                keyboardHeight = keyboard.mOccupiedHeight,
            ),
        )
    }

    @JvmStatic
    fun peekPointerSize(): Int = holder.get()?.pointers?.pointerSize ?: 0

    @JvmStatic
    fun consume(): Stash? {
        val stash = holder.get()
        holder.set(null)
        return stash
    }
}
