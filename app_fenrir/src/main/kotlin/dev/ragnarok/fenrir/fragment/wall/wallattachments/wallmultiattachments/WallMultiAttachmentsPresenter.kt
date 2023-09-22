package dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.intValueIn
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class WallMultiAttachmentsPresenter(
    accountId: Long,
    private val owner_id: Long,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<IWallMultiAttachmentsView>(accountId, savedInstanceState) {
    private val mAudios: ArrayList<Post> = ArrayList()
    private val mDocs: ArrayList<Document> = ArrayList()
    private val mLinks: ArrayList<Link> = ArrayList()
    private val mPhotoAlbums: ArrayList<PhotoAlbum> = ArrayList()
    private val mPhotos: ArrayList<Photo> = ArrayList()
    private val mVideos: ArrayList<Video> = ArrayList()
    private val mPostsWithComments: ArrayList<Post> = ArrayList()

    private val fInteractor: IWallsRepository = walls
    private val actualDataDisposable = CompositeDisposable()
    private var loaded = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false

    @AttachmentWallType
    private var currentPage: Int = AttachmentWallType.PHOTO
    override fun onGuiCreated(viewHost: IWallMultiAttachmentsView) {
        super.onGuiCreated(viewHost)
        viewHost.onUpdateCurrentPage(currentPage)
        viewHost.displayAudioData(mAudios)
        viewHost.displayDocsData(mDocs)
        viewHost.displayLinksData(mLinks)
        viewHost.displayPhotoAlbumsData(mPhotoAlbums)
        viewHost.displayPhotoData(mPhotos)
        viewHost.displayVideoData(mVideos)
        viewHost.displayPostsWithCommentsData(mPostsWithComments)
        resolveToolbar()
        viewHost.resolveEmptyText(currentPage)
    }

    fun fireUpdateCurrentPage(@AttachmentWallType currentPage: Int) {
        this.currentPage = currentPage
        view?.onUpdateCurrentPage(currentPage)
        view?.resolveEmptyText(currentPage)
        resolveToolbar()
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(fInteractor.getWallNoCache(
            accountId,
            owner_id,
            offset,
            100,
            WallCriteria.MODE_ALL
        )
            .fromIOToMain()
            .subscribe({ data ->
                onActualDataReceived(
                    offset,
                    data
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun update(data: List<Post>, root: Boolean) {
        val origSizeAudios = mAudios.size
        val origSizeDocs = mDocs.size
        val origSizeLinks = mLinks.size
        val origSizePhotoAlbums = mPhotoAlbums.size
        val origSizePhotos = mPhotos.size
        val origSizeVideos = mVideos.size
        val origSizePostsWithComments = mPostsWithComments.size

        for (i in data) {
            i.attachments?.audios.nonNullNoEmpty {
                mAudios.add(i)
            }
            i.attachments?.docs.nonNullNoEmpty {
                mDocs.addAll(it)
            }
            i.attachments?.links.nonNullNoEmpty {
                mLinks.addAll(it)
            }
            i.attachments?.photoAlbums.nonNullNoEmpty {
                mPhotoAlbums.addAll(it)
            }
            i.attachments?.photos.nonNullNoEmpty {
                mPhotos.addAll(it)
            }
            i.attachments?.videos.nonNullNoEmpty {
                mVideos.addAll(it)
            }
            if (i.commentsCount > 0) {
                mPostsWithComments.add(i)
            }
            i.getCopyHierarchy()?.let {
                update(it, false)
            }
        }
        if (root) {
            if (mAudios.size - origSizeAudios > 0) {
                view?.notifyDataAdded(
                    AttachmentWallType.AUDIO,
                    origSizeAudios,
                    mAudios.size - origSizeAudios
                )
            }
            if (mDocs.size - origSizeDocs > 0) {
                view?.notifyDataAdded(
                    AttachmentWallType.DOC,
                    origSizeDocs,
                    mDocs.size - origSizeDocs
                )
            }
            if (mLinks.size - origSizeLinks > 0) {
                view?.notifyDataAdded(
                    AttachmentWallType.LINK,
                    origSizeLinks,
                    mLinks.size - origSizeLinks
                )
            }
            if (mPhotoAlbums.size - origSizePhotoAlbums > 0) {
                view?.notifyDataAdded(
                    AttachmentWallType.PHOTO_ALBUM,
                    origSizePhotoAlbums,
                    mPhotoAlbums.size - origSizePhotoAlbums
                )
            }
            if (mPhotos.size - origSizePhotos > 0) {
                view?.notifyDataAdded(
                    AttachmentWallType.PHOTO,
                    origSizePhotos,
                    mPhotos.size - origSizePhotos
                )
            }
            if (mVideos.size - origSizeVideos > 0) {
                view?.notifyDataAdded(
                    AttachmentWallType.VIDEO,
                    origSizeVideos,
                    mVideos.size - origSizeVideos
                )
            }
            if (mPostsWithComments.size - origSizePostsWithComments > 0) {
                view?.notifyDataAdded(
                    AttachmentWallType.POST_COMMENT,
                    origSizePostsWithComments,
                    mPostsWithComments.size - origSizePostsWithComments
                )
            }
            view?.resolveEmptyText(currentPage)
        }
    }

    fun goToPostComments() {
        val posts = ArrayList<Int>(mPostsWithComments.size)
        for (post in mPostsWithComments) {
            posts.add(post.vkid)
        }
        if (posts.size > 0) {
            view?.openAllComments(
                accountId,
                owner_id,
                posts
            )
        }
    }

    private fun clearAllData() {
        val origSizeAudios = mAudios.size
        val origSizeDocs = mDocs.size
        val origSizeLinks = mLinks.size
        val origSizePhotoAlbums = mPhotoAlbums.size
        val origSizePhotos = mPhotos.size
        val origSizeVideos = mVideos.size
        val origSizePostsWithComments = mPostsWithComments.size

        mAudios.clear()
        view?.notifyDataRemoved(AttachmentWallType.AUDIO, 0, origSizeAudios)
        mDocs.clear()
        view?.notifyDataRemoved(AttachmentWallType.DOC, 0, origSizeDocs)
        mLinks.clear()
        view?.notifyDataRemoved(AttachmentWallType.LINK, 0, origSizeLinks)
        mPhotoAlbums.clear()
        view?.notifyDataRemoved(AttachmentWallType.PHOTO_ALBUM, 0, origSizePhotoAlbums)
        mPhotos.clear()
        view?.notifyDataRemoved(AttachmentWallType.PHOTO, 0, origSizePhotos)
        mVideos.clear()
        view?.notifyDataRemoved(AttachmentWallType.VIDEO, 0, origSizeVideos)
        mPostsWithComments.clear()
        view?.notifyDataRemoved(AttachmentWallType.POST_COMMENT, 0, origSizePostsWithComments)

        view?.resolveEmptyText(currentPage)
    }

    private fun onActualDataReceived(offset: Int, data: List<Post>) {
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (endOfContent) resumedView?.onSetLoadingStatus(2)
        if (offset == 0) {
            clearAllData()
            loaded = data.size
            update(data, true)
            resolveToolbar()
        } else {
            loaded += data.size
            update(data, true)
            resolveToolbar()
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(actualDataLoading)
        if (!endOfContent) resumedView?.onSetLoadingStatus(
            if (actualDataLoading) 1 else 0
        )
    }

    private fun resolveToolbar() {
        view?.let {
            it.toolbarTitle(getString(R.string.attachments_in_wall))
            when (currentPage) {
                AttachmentWallType.AUDIO -> {
                    it.toolbarSubtitle(
                        getString(
                            R.string.audios_posts_count,
                            safeCountOf(mAudios)
                        ) + " " + getString(R.string.posts_analized, loaded)
                    )
                }

                AttachmentWallType.DOC -> {
                    it.toolbarSubtitle(
                        getString(
                            R.string.documents_count,
                            safeCountOf(mDocs)
                        ) + " " + getString(R.string.posts_analized, loaded)
                    )
                }

                AttachmentWallType.LINK -> {
                    it.toolbarSubtitle(
                        getString(
                            R.string.links_count,
                            safeCountOf(mLinks)
                        ) + " " + getString(R.string.posts_analized, loaded)
                    )
                }

                AttachmentWallType.PHOTO -> {
                    it.toolbarSubtitle(
                        getString(
                            R.string.photos_count,
                            safeCountOf(mPhotos)
                        ) + " " + getString(R.string.posts_analized, loaded)
                    )
                }

                AttachmentWallType.PHOTO_ALBUM -> {
                    it.toolbarSubtitle(
                        getString(
                            R.string.photo_albums_count,
                            safeCountOf(mPhotoAlbums)
                        ) + " " + getString(R.string.posts_analized, loaded)
                    )
                }

                AttachmentWallType.POST_COMMENT -> {
                    it.toolbarSubtitle(
                        getString(
                            R.string.posts_count,
                            safeCountOf(mPostsWithComments)
                        ) + " " + getString(R.string.posts_analized, loaded)
                    )
                }

                AttachmentWallType.VIDEO -> {
                    it.toolbarSubtitle(
                        getString(
                            R.string.videos_count,
                            safeCountOf(mVideos)
                        ) + " " + getString(R.string.posts_analized, loaded)
                    )
                }
            }
        }
    }

    fun firePhotoClick(position: Int) {
        if (FenrirNative.isNativeLoaded && Settings.get().main().isNative_parcel_photo) {
            view?.goToTempPhotosGallery(
                accountId,
                ParcelNative.createParcelableList(mPhotos, ParcelFlags.NULL_LIST),
                position
            )
        } else {
            val source = TmpSource(instanceId, 0)
            fireTempDataUsage()
            actualDataDisposable.add(
                Stores.instance
                    .tempStore()
                    .putTemporaryData(
                        source.ownerId,
                        source.sourceId,
                        mPhotos,
                        Serializers.PHOTOS_SERIALIZER
                    )
                    .fromIOToMain()
                    .subscribe({
                        onPhotosSavedToTmpStore(
                            position,
                            source
                        )
                    }) { obj -> obj.printStackTrace() })
        }
    }

    private fun onPhotosSavedToTmpStore(index: Int, source: TmpSource) {
        view?.goToTempPhotosGallery(
            accountId,
            source,
            index
        )
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd(button: Boolean): Boolean {
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData(loaded)
            return false
        }
        if (button && endOfContent) resumedView?.onSetLoadingStatus(2)
        return true
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    fun firePostBodyClick(post: Post) {
        if (intValueIn(post.postType, VKApiPost.Type.SUGGEST, VKApiPost.Type.POSTPONE)) {
            view?.openPostEditor(
                accountId,
                post
            )
            return
        }
        firePostClick(post)
    }

    fun firePostRestoreClick(post: Post) {
        appendDisposable(fInteractor.restore(accountId, post.ownerId, post.vkid)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    fun fireLikeLongClick(post: Post) {
        view?.goToLikes(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireShareLongClick(post: Post) {
        view?.goToReposts(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireLikeClick(post: Post) {
        if (Settings.get().main().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        appendDisposable(fInteractor.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain()
            .subscribe(ignore()) { t ->
                showError(t)
            })
    }

    init {
        loadActualData(0)
    }
}
