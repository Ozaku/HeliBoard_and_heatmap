// SPDX-License-Identifier: GPL-3.0-only

// ai-note: why a correction-detector flush ran (step 1.11)

package helium314.keyboard.heatmap.learning



enum class CorrectionFlushReason_v1 {

    FIELD_BLUR,

    HOST_CHANGED,

    IME_ACTION,

    WINDOW_SLIDE,

    SESSION_RESET,

    MANUAL_DEBUG,

}

