package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    LoadMoreState.LOADING,
    LoadMoreState.INVISIBLE,
    LoadMoreState.CAN_LOAD_MORE,
    LoadMoreState.END_OF_LIST
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class LoadMoreState {
    companion object {
        const val LOADING = 1
        const val INVISIBLE = 2
        const val CAN_LOAD_MORE = 3
        const val END_OF_LIST = 4
    }
}