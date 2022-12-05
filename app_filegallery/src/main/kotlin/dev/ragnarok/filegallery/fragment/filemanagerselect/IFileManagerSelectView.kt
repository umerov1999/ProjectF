package dev.ragnarok.filegallery.fragment.filemanagerselect

import android.os.Parcelable
import androidx.annotation.StringRes
import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.model.FileItemSelect

interface IFileManagerSelectView : IMvpView, IErrorView {
    fun displayData(items: ArrayList<FileItemSelect>)
    fun resolveEmptyText(visible: Boolean)
    fun resolveLoading(visible: Boolean)
    fun notifyAllChanged()
    fun updatePathString(file: String)
    fun restoreScroll(scroll: Parcelable)

    fun onScrollTo(pos: Int)
    fun notifyItemChanged(pos: Int)
    fun showMessage(@StringRes res: Int)

    fun updateSelectVisibility(visible: Boolean)
    fun updateHeader(ext: String?)
}
