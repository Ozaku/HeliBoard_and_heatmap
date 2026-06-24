// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.heatmap.learning



/** Snapshot of in-IME session state at flush time (step 1.11). */

data class CorrectionFlushContext_v1(

    val sessionGeneration: Long,

    val journalWordCount: Int,

    val registrySessionCount: Int,

    val journalUsedChars: Int,

)

