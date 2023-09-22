package dev.ragnarok.fenrir.fragment.wall.wallpost

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPostSource
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore

class WallPostPresenter(
    accountId: Long, private val postId: Int, private val ownerId: Long, post: Post?,
    owner: Owner?, savedInstanceState: Bundle?
) : PlaceSupportPresenter<IWallPostView>(accountId, savedInstanceState) {
    private val wallInteractor: IWallsRepository = walls
    private val ownersRepository: IOwnersRepository = owners
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private var post: Post? = null
    private var owner: Owner? = null
    private var loadingPostNow = false
    private fun onPostChanged(post: Post) {
        this.post = post
        resolveCommentsView()
        resolveLikesView()
        resolveToolbarView()
        resolveCommentsView()
        resolveRepostsView()
    }

    private fun onPostUpdate(update: PostUpdate) {
        val pPost = post ?: return
        update.likeUpdate.requireNonNull {
            pPost.setLikesCount(it.count)
            if (update.accountId == accountId) {
                pPost.setUserLikes(it.isLiked)
            }
            resolveLikesView()
        }
        update.pinUpdate.requireNonNull {
            pPost.setPinned(it.isPinned)
        }
        update.deleteUpdate.requireNonNull {
            pPost.setDeleted(it.isDeleted)
            resolveContentRootView()
        }
    }

    private fun loadOwnerInfoIfNeed() {
        if (owner == null) {
            appendDisposable(
                ownersRepository.getBaseOwnerInfo(
                    accountId,
                    ownerId,
                    IOwnersRepository.MODE_NET
                )
                    .fromIOToMain()
                    .subscribe({ owner -> onOwnerInfoReceived(owner) }, ignore())
            )
        }
    }

    private fun onOwnerInfoReceived(owner: Owner) {
        this.owner = owner
    }

    private fun setLoadingPostNow(loadingPostNow: Boolean) {
        this.loadingPostNow = loadingPostNow
        resolveContentRootView()
    }

    private fun loadActualPostInfo() {
        if (loadingPostNow) {
            return
        }
        setLoadingPostNow(true)
        appendDisposable(wallInteractor.getById(accountId, ownerId, postId)
            .fromIOToMain()
            .subscribe({ post -> onActualPostReceived(post) }) { t ->
                onLoadPostInfoError(
                    t
                )
            })
    }

    private fun onLoadPostInfoError(t: Throwable) {
        setLoadingPostNow(false)
        showError(t)
    }

    private fun onActualPostReceived(post: Post) {
        this.post = post
        setLoadingPostNow(false)
        resolveToolbarView()
        resolveCommentsView()
        resolveLikesView()
        resolveRepostsView()
    }

    override fun onGuiCreated(viewHost: IWallPostView) {
        super.onGuiCreated(viewHost)
        resolveRepostsView()
        resolveLikesView()
        resolveCommentsView()
        resolveContentRootView()
        resolveToolbarView()
    }

    private fun resolveRepostsView() {
        val pPost = post ?: return
        view?.displayReposts(
            pPost.repostCount,
            pPost.isUserReposted
        )
    }

    private fun resolveLikesView() {
        val pPost = post ?: return
        view?.displayLikes(
            pPost.likesCount,
            pPost.isUserLikes
        )
    }

    private fun resolveCommentsView() {
        val pPost = post ?: return
        view?.let {
            it.displayCommentCount(pPost.commentsCount)
            it.setCommentButtonVisible(pPost.isCanPostComment || pPost.commentsCount > 0)
        }
    }

    private fun resolveContentRootView() {
        when {
            post != null -> {
                view?.displayPostInfo(post ?: return)
            }

            loadingPostNow -> {
                view?.displayLoading()
            }

            else -> {
                view?.displayLoadingFail()
            }
        }
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_POST, post)
        outState.putParcelable(SAVE_OWNER, ParcelableOwnerWrapper(owner))
    }

    fun fireOptionViewCreated(view: IWallPostView.IOptionView) {
        val pPost = post ?: run {
            view.setCanPin(false)
            view.setCanUnpin(false)
            view.setCanDelete(false)
            view.setCanRestore(false)
            view.setCanEdit(false)
            view.setInFave(false)
            return
        }
        view.setCanPin(!pPost.isPinned && pPost.isCanPin && !pPost.isDeleted)
        view.setCanUnpin(pPost.isPinned && pPost.isCanPin && !pPost.isDeleted)
        view.setCanDelete(canDelete())
        view.setCanRestore(pPost.isDeleted)
        view.setCanEdit(pPost.isCanEdit)
        view.setInFave(pPost.isFavorite)
    }

    private fun canDelete(): Boolean {
        val pPost = post ?: return false
        if (pPost.isDeleted) {
            return false
        }
        val canDeleteAsAdmin = owner is Community && (owner as Community).isAdmin
        val canDeleteAsOwner = ownerId == accountId || pPost.authorId == accountId
        return canDeleteAsAdmin || canDeleteAsOwner
    }

    fun fireGoToOwnerClick() {
        fireOwnerClick(ownerId)
    }

    fun firePostEditClick() {
        val pPost = post ?: run {
            view?.showPostNotReadyToast()
            return
        }
        view?.goToPostEditing(accountId, pPost)
    }

    fun fireCommentClick() {
        val commented = Commented(postId, ownerId, CommentedType.POST, null)
        view?.openComments(
            accountId,
            commented,
            null
        )
    }

    fun fireRepostLongClick() {
        view?.goToReposts(
            accountId,
            "post",
            ownerId,
            postId
        )
    }

    fun fireLikeLongClick() {
        view?.goToLikes(
            accountId,
            "post",
            ownerId,
            postId
        )
    }

    fun fireTryLoadAgainClick() {
        loadActualPostInfo()
    }

    fun fireShareClick() {
        if (post != null) {
            view?.repostPost(
                accountId,
                post ?: return
            )
        } else {
            view?.showPostNotReadyToast()
        }
    }

    fun fireLikeClick() {
        if (Settings.get().main().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        if (post != null) {
            appendDisposable(wallInteractor.like(
                accountId,
                ownerId,
                postId,
                !(post ?: return).isUserLikes
            )
                .fromIOToMain()
                .subscribe(ignore()) { t ->
                    showError(t)
                })
        } else {
            view?.showPostNotReadyToast()
        }
    }

    fun fireBookmark() {
        post?.let { pPost ->
            if (!pPost.isFavorite) {
                appendDisposable(faveInteractor.addPost(accountId, ownerId, postId, null)
                    .fromIOToMain()
                    .subscribe({ onPostAddedToBookmarks() }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            } else {
                appendDisposable(faveInteractor.removePost(accountId, ownerId, postId)
                    .fromIOToMain()
                    .subscribe({ onPostAddedToBookmarks() }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }
        }
    }

    private fun onPostAddedToBookmarks() {
        post?.let {
            it.setFavorite((!it.isFavorite))
        }
        view?.invalidateMenu()
        view?.showSuccessToast()
    }

    fun fireDeleteClick() {
        deleteOrRestore(true)
    }

    fun fireRestoreClick() {
        deleteOrRestore(false)
    }

    private fun deleteOrRestore(delete: Boolean) {
        val completable = if (delete) wallInteractor.delete(
            accountId,
            ownerId,
            postId
        ) else wallInteractor.restore(accountId, ownerId, postId)
        appendDisposable(completable
            .fromIOToMain()
            .subscribe({ onDeleteOrRestoreComplete(delete) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    private fun onDeleteOrRestoreComplete(deleted: Boolean) {
        view?.displayDeleteOrRestoreComplete(
            deleted
        )
    }

    fun firePinClick() {
        pinOrUnpin(true)
    }

    fun fireUnpinClick() {
        pinOrUnpin(false)
    }

    private fun pinOrUnpin(pin: Boolean) {
        appendDisposable(wallInteractor.pinUnpin(accountId, ownerId, postId, pin)
            .fromIOToMain()
            .subscribe({ onPinOrUnpinComplete(pin) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    private fun onPinOrUnpinComplete(pinned: Boolean) {
        view?.displayPinComplete(pinned)
    }

    fun fireRefresh() {
        loadActualPostInfo()
    }

    fun fireCopyLinkClick() {
        val link = String.format("vk.com/wall%s_%s", ownerId, postId)
        view?.copyLinkToClipboard(link)
    }

    fun fireExportClick() {
        post?.let { view?.doPostExport(accountId, it) }
    }

    fun fireReport(context: Context) {
        val items = arrayOf<CharSequence>(
            "Спам",
            "Детская порнография",
            "Экстремизм",
            "Насилие",
            "Пропаганда наркотиков",
            "Материал для взрослых",
            "Оскорбление",
            "Призывы к суициду"
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.report)
            .setItems(items) { dialog: DialogInterface, item: Int ->
                appendDisposable(wallInteractor.reportPost(accountId, ownerId, postId, item)
                    .fromIOToMain()
                    .subscribe({ p ->
                        if (p == 1) view?.customToast?.showToast(
                            R.string.success
                        ) else view?.customToast?.showToast(
                            R.string.error
                        )
                    }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
                dialog.dismiss()
            }
            .show()
    }

    private fun resolveToolbarView() {
        if (post != null) {
            var type = IWallPostView.SUBTITLE_NORMAL
            if (post?.source != null) {
                when ((post ?: return).source?.getData()) {
                    VKApiPostSource.Data.PROFILE_ACTIVITY -> type =
                        IWallPostView.SUBTITLE_STATUS_UPDATE

                    VKApiPostSource.Data.PROFILE_PHOTO -> type = IWallPostView.SUBTITLE_PHOTO_UPDATE
                }
            }
            val finalType = type
            view?.let {
                it.displayToolbarTitle(post?.authorName)
                it.displayToolbarSubtitle(finalType, post?.date ?: 0)
            }
        } else {
            view?.let {
                it.displayDefaultToolbarTitle()
                it.displayDefaultToolbarSubtitle()
            }
        }
    }

    fun fireCopyTextClick() {
        if (post == null) {
            view?.showPostNotReadyToast()
            return
        }

        // Append post text
        val builder = StringBuilder()

        post?.text.nonNullNoEmpty {
            builder.append(it).append("\n")
        }

        // Append copies text if exists
        post?.getCopyHierarchy().nonNullNoEmpty {
            for (copy in it) {
                if (copy.text.nonNullNoEmpty()) {
                    builder.append(copy.text).append("\n")
                }
            }
        }
        view?.copyTextToClipboard(builder.toString())
    }

    fun fireHasgTagClick(hashTag: String) {
        view?.goToNewsSearch(
            accountId,
            hashTag
        )
    }

    companion object {
        private const val SAVE_POST = "save-post"
        private const val SAVE_OWNER = "save-owner"
    }

    init {
        if (savedInstanceState?.getParcelableCompat<ParcelableOwnerWrapper>(SAVE_OWNER) != null) {
            val wrapper: ParcelableOwnerWrapper =
                savedInstanceState.getParcelableCompat(SAVE_OWNER)!!
            this.post = savedInstanceState.getParcelableCompat(SAVE_POST)
            this.owner = wrapper.get()
        } else {
            this.post = post
            this.owner = owner
            loadActualPostInfo()
        }
        loadOwnerInfoIfNeed()
        appendDisposable(wallInteractor.observeMinorChanges()
            .filter { it.ownerId == ownerId && it.postId == postId }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onPostUpdate(it) })
        appendDisposable(wallInteractor.observeChanges()
            .filter { postId == it.vkid && it.ownerId == ownerId }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onPostChanged(it) })
    }
}