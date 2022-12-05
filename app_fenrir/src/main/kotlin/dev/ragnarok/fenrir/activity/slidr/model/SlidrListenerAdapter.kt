package dev.ragnarok.fenrir.activity.slidr.model

class SlidrListenerAdapter : SlidrListener {
    override fun onSlideStateChanged(state: Int) {}
    override fun onSlideChange(percent: Float) {}
    override fun onSlideOpened() {}
    override fun onSlideClosed(): Boolean {
        return false
    }
}