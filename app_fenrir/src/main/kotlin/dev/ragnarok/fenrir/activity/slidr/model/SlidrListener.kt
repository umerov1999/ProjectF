package dev.ragnarok.fenrir.activity.slidr.model

interface SlidrListener {

    fun onSlideStateChanged(state: Int)
    fun onSlideChange(percent: Float)
    fun onSlideOpened()

    fun onSlideClosed(): Boolean
}