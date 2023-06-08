package dev.ragnarok.fenrir.fragment.audio.local.localaudioalbums

import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.LocalImageAlbum

interface ILocalAudioAlbumsView : IMvpView {
    fun displayData(data: List<LocalImageAlbum>)
    fun setEmptyTextVisible(visible: Boolean)
    fun displayProgress(loading: Boolean)
    fun openAlbum(album: LocalImageAlbum)
    fun notifyDataChanged()
    fun requestReadExternalStoragePermission()

    fun updateCurrentId(currentId: Int)
}
