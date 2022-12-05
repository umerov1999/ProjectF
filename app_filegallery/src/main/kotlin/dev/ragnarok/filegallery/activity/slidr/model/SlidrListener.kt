package dev.ragnarok.filegallery.activity.slidr.model

interface SlidrListener {

    fun onSlideStateChanged(state: Int)
    fun onSlideChange(percent: Float)
    fun onSlideOpened()

    fun onSlideClosed(): Boolean
}