package dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.model.Video

interface IWallMultiAttachmentsView : IMvpView, IErrorView,
    IAttachmentsPlacesView {
    fun notifyDataRemoved(@AttachmentWallType type: Int, position: Int, count: Int)
    fun notifyDataAdded(@AttachmentWallType type: Int, position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun toolbarTitle(title: String)
    fun toolbarSubtitle(subtitle: String)
    fun onSetLoadingStatus(isLoad: Int)
    fun openPostEditor(accountId: Long, post: Post)

    fun goToTempPhotosGallery(accountId: Long, source: TmpSource, index: Int)
    fun goToTempPhotosGallery(accountId: Long, ptr: Long, index: Int)

    fun onUpdateCurrentPage(@AttachmentWallType type: Int)
    fun resolveEmptyText(@AttachmentWallType type: Int)

    fun openAllComments(accountId: Long, ownerId: Long, posts: ArrayList<Int>)

    fun displayAudioData(posts: MutableList<Post>)
    fun displayDocsData(docs: MutableList<Document>)
    fun displayLinksData(links: MutableList<Link>)
    fun displayPhotoAlbumsData(photoAlbums: MutableList<PhotoAlbum>)
    fun displayPhotoData(photos: MutableList<Photo>)
    fun displayVideoData(videos: MutableList<Video>)
    fun displayPostsWithCommentsData(posts: MutableList<Post>)
}
