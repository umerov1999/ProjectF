package dev.ragnarok.fenrir.activity.shortvideopager

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.media.story.IStoryPlayer
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Video

interface IShortVideoPagerView : IMvpView, IErrorView, IToastView {
    fun displayData(pageCount: Int, selectedIndex: Int)
    fun setAspectRatioAt(position: Int, w: Int, h: Int)
    fun setPreparingProgressVisible(position: Int, preparing: Boolean)
    fun attachDisplayToPlayer(adapterPosition: Int, storyPlayer: IStoryPlayer?)
    fun setToolbarTitle(@StringRes titleRes: Int, vararg params: Any?)
    fun setToolbarSubtitle(shortVideo: Video, account_id: Long, isPlaySpeed: Boolean)
    fun onShare(shortVideo: Video, account_id: Long)
    fun configHolder(adapterPosition: Int, progress: Boolean, aspectRatioW: Int, aspectRatioH: Int)
    fun onNext()
    fun requestWriteExternalStoragePermission()
    fun showMessage(@StringRes message: Int, error: Boolean)
    fun showMessage(message: String, error: Boolean)
    fun updateCount(count: Int)
    fun notifyDataSetChanged()
    fun notifyDataAdded(pos: Int, count: Int)
    fun displayListLoading(loading: Boolean)

    fun displayLikes(count: Int, userLikes: Boolean)
    fun displayCommentCount(count: Int)
    fun showComments(accountId: Long, commented: Commented)
    fun goToLikes(accountId: Long, type: String, ownerId: Long, id: Int)
}
