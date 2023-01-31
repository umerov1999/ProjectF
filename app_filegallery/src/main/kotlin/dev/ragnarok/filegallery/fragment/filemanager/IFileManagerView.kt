package dev.ragnarok.filegallery.fragment.filemanager

import android.os.Parcelable
import androidx.annotation.StringRes
import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.Video

interface IFileManagerView : IMvpView, IErrorView {
    fun displayData(items: ArrayList<FileItem>)
    fun resolveEmptyText(visible: Boolean)
    fun resolveLoading(visible: Boolean)
    fun notifyAllChanged()
    fun updatePathString(file: String)
    fun restoreScroll(scroll: Parcelable)

    fun displayGalleryUnSafe(parcelNativePointer: Long, position: Int, reversed: Boolean)
    fun displayVideo(video: Video)
    fun startPlayAudios(audios: ArrayList<Audio>, position: Int)

    fun onScrollTo(pos: Int)
    fun notifyItemChanged(pos: Int)
    fun showMessage(@StringRes res: Int)

    fun updateSelectedMode(show: Boolean)
    fun onBusy(path: String)
}
