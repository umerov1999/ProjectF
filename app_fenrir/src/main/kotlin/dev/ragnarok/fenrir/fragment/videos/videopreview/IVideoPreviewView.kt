package dev.ragnarok.fenrir.fragment.videos.videopreview

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Video

interface IVideoPreviewView : IMvpView, IErrorView {
    fun displayLoading()
    fun displayLoadingError()
    fun displayVideoInfo(video: Video)
    fun displayLikes(count: Int, userLikes: Boolean)
    fun setCommentButtonVisible(visible: Boolean)
    fun displayCommentCount(count: Int)
    fun showSuccessToast()
    fun showOwnerWall(accountId: Long, ownerId: Long)
    fun showSubtitle(subtitle: String?)
    fun showComments(accountId: Long, commented: Commented)
    fun displayShareDialog(accountId: Long, video: Video, canPostToMyWall: Boolean)
    fun showVideoPlayMenu(accountId: Long, video: Video)
    fun doAutoPlayVideo(accountId: Long, video: Video)
    fun goToLikes(accountId: Long, type: String, ownerId: Long, id: Int)
    fun displayOwner(owner: Owner)
    interface IOptionView {
        fun setCanAdd(can: Boolean)
        fun setIsMy(my: Boolean)
    }
}