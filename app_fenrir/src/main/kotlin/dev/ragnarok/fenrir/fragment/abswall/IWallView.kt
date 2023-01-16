package dev.ragnarok.fenrir.fragment.abswall

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.ISnackbarView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.*

interface IWallView : IAttachmentsPlacesView, IMvpView, ISnackbarView,
    IErrorView, IToastView {
    fun displayWallData(data: MutableList<Post>)
    fun notifyWallDataSetChanged()
    fun updateStory(stories: MutableList<Story>?)
    fun notifyWallItemChanged(position: Int)
    fun notifyWallDataAdded(position: Int, count: Int)
    fun setupLoadMoreFooter(@LoadMoreState state: Int)
    fun showRefreshing(refreshing: Boolean)
    fun openPhotoAlbums(accountId: Long, ownerId: Long, owner: Owner?)
    fun openAudios(accountId: Long, ownerId: Long, owner: Owner?)
    fun openArticles(accountId: Long, ownerId: Long, owner: Owner?)
    fun openVideosLibrary(accountId: Long, ownerId: Long, owner: Owner?)
    fun goToPostCreation(accountId: Long, ownerId: Long, @EditingPostType postType: Int)
    fun copyToClipboard(label: String?, body: String?)
    fun openPhotoAlbum(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: ArrayList<Photo>,
        position: Int
    )

    fun goToWallSearch(accountId: Long, ownerId: Long)
    fun openPostEditor(accountId: Long, post: Post)
    fun notifyWallItemRemoved(index: Int)
    fun goToConversationAttachments(accountId: Long, ownerId: Long)
    fun goNarratives(accountId: Long, ownerId: Long)
    interface IOptionView {
        fun typeOwnerId(id: Long)
        fun setIsMy(my: Boolean)
        fun setIsBlacklistedByMe(blocked: Boolean)
        fun setIsFavorite(favorite: Boolean)
        fun setIsSubscribed(subscribed: Boolean)
    }

    fun onRequestSkipOffset(accountId: Long, ownerId: Long, wallFilter: Int, currentPos: Int)
}