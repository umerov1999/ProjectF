package dev.ragnarok.fenrir.dialog.audioduplicate

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Audio

interface IAudioDuplicateView : IMvpView, IErrorView {
    fun displayData(new_audio: Audio, old_audio: Audio)
    fun setOldBitrate(bitrate: Int?)
    fun setNewBitrate(bitrate: Int?)
    fun updateShowBitrate(needShow: Boolean)
}