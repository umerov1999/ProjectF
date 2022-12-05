package dev.ragnarok.fenrir.fragment.videopreview

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
    fun showOwnerWall(accountId: Int, ownerId: Int)
    fun showSubtitle(subtitle: String?)
    fun showComments(accountId: Int, commented: Commented)
    fun displayShareDialog(accountId: Int, video: Video, canPostToMyWall: Boolean)
    fun showVideoPlayMenu(accountId: Int, video: Video)
    fun doAutoPlayVideo(accountId: Int, video: Video)
    fun goToLikes(accountId: Int, type: String, ownerId: Int, id: Int)
    fun displayOwner(owner: Owner)
    interface IOptionView {
        fun setCanAdd(can: Boolean)
        fun setIsMy(my: Boolean)
    }
}