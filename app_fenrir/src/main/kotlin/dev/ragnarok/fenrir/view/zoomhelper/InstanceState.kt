package dev.ragnarok.fenrir.view.zoomhelper

internal object InstanceState {
    val zoomHelper: ZoomHelper? by lazy {
        ZoomHelper()
    }
}