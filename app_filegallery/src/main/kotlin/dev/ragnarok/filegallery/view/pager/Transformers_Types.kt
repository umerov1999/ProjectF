package dev.ragnarok.filegallery.view.pager

import androidx.annotation.IntDef

@IntDef(
    Transformers_Types.OFF,
    Transformers_Types.DEPTH_TRANSFORMER,
    Transformers_Types.ZOOM_OUT_TRANSFORMER,
    Transformers_Types.CLOCK_SPIN_TRANSFORMER,
    Transformers_Types.BACKGROUND_TO_FOREGROUND_TRANSFORMER,
    Transformers_Types.CUBE_IN_DEPTH_TRANSFORMER,
    Transformers_Types.FAN_TRANSFORMER,
    Transformers_Types.GATE_TRANSFORMER,
    Transformers_Types.SLIDER_TRANSFORMER
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class Transformers_Types {
    companion object {
        const val OFF = 0
        const val DEPTH_TRANSFORMER = 1
        const val ZOOM_OUT_TRANSFORMER = 2
        const val CLOCK_SPIN_TRANSFORMER = 3
        const val BACKGROUND_TO_FOREGROUND_TRANSFORMER = 4
        const val CUBE_IN_DEPTH_TRANSFORMER = 5
        const val FAN_TRANSFORMER = 6
        const val GATE_TRANSFORMER = 7
        const val SLIDER_TRANSFORMER = 8
    }
}