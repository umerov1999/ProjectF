package dev.ragnarok.fenrir.fragment.photoallcomment

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Comment

interface IPhotoAllCommentView : IMvpView, IErrorView, IToastView,
    IAttachmentsPlacesView {
    fun displayData(comments: MutableList<Comment>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun dismissDeepLookingCommentProgress()
    fun displayDeepLookingCommentProgress()
    fun moveFocusTo(index: Int, smooth: Boolean)
    fun notifyDataAddedToTop(count: Int)
    fun notifyItemChanged(index: Int)
}