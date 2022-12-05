package dev.ragnarok.fenrir.fragment.audio.local.audioslocal

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.upload.Upload

interface IAudiosLocalView : IMvpView, IErrorView, IToastView {
    fun displayList(audios: List<Audio>)
    fun notifyItemChanged(index: Int)
    fun notifyItemRemoved(index: Int)
    fun notifyListChanged()
    fun notifyUploadItemsAdded(position: Int, count: Int)
    fun notifyUploadItemRemoved(position: Int)
    fun notifyUploadItemChanged(position: Int)
    fun notifyUploadProgressChanged(position: Int, progress: Int, smoothly: Boolean)
    fun setUploadDataVisible(visible: Boolean)
    fun displayUploads(data: List<Upload>)
    fun displayRefreshing(refreshing: Boolean)
    fun checkPermission()
}