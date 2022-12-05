package dev.ragnarok.fenrir.fragment.filemanagerselect

import android.os.Parcelable
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.FileItem

interface IFileManagerSelectView : IMvpView, IErrorView {
    fun displayData(items: ArrayList<FileItem>)
    fun resolveEmptyText(visible: Boolean)
    fun resolveLoading(visible: Boolean)
    fun onError(throwable: Throwable)
    fun notifyAllChanged()
    fun updatePathString(file: String)
    fun restoreScroll(scroll: Parcelable)

    fun onScrollTo(pos: Int)
    fun notifyItemChanged(pos: Int)
    fun showMessage(@StringRes res: Int)

    fun updateSelectVisibility(visible: Boolean)
    fun updateHeader(ext: String?)
}
