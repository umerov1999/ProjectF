package dev.ragnarok.fenrir.fragment.videos.videopreview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

class VideoPreviewPresenter(
    accountId: Long,
    private val videoId: Int,
    private val ownerId: Long,
    aKey: String?,
    video: Video?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IVideoPreviewView>(accountId, savedInstanceState) {
    private val accessKey: String? = if (video != null) video.accessKey else aKey
    private val interactor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val ownerInteractor: IOwnersRepository = owners
    private var video: Video? = null
    private var owner: Owner? = null
    private var refreshingNow = false
    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable("video", video)
    }

    private fun setRefreshingNow(refreshingNow: Boolean) {
        this.refreshingNow = refreshingNow
    }

    private fun onVideoAddedToBookmarks() {
        video?.let {
            it.setFavorite(!it.isFavorite)
        }
        view?.showSuccessToast()
    }

    fun fireFaveVideo() {
        val pVideo = video ?: return
        if (!pVideo.isFavorite) {
            appendJob(
                faveInteractor.addVideo(
                    accountId,
                    pVideo.ownerId,
                    pVideo.id,
                    pVideo.accessKey
                )
                    .fromIOToMain({ onVideoAddedToBookmarks() }) { t ->
                        showError(
                            getCauseIfRuntime(t)
                        )
                    })
        } else {
            appendJob(
                faveInteractor.removeVideo(
                    accountId,
                    pVideo.ownerId,
                    pVideo.id
                )
                    .fromIOToMain({ onVideoAddedToBookmarks() }) { t ->
                        showError(
                            getCauseIfRuntime(t)
                        )
                    })
        }
    }

    fun fireEditVideo(context: Context) {
        video?.let { vd ->
            val root = View.inflate(context, R.layout.entry_video_info, null)
            root.findViewById<TextInputEditText>(R.id.edit_title).setText(video?.title)
            root.findViewById<TextInputEditText>(R.id.edit_description).setText(
                video?.description
            )
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.edit)
                .setCancelable(true)
                .setView(root)
                .setPositiveButton(R.string.button_ok) { _, _ ->
                    appendJob(
                        interactor.edit(
                            accountId, vd.ownerId, vd.id,
                            root.findViewById<TextInputEditText>(R.id.edit_title).text.toString(),
                            root.findViewById<TextInputEditText>(R.id.edit_description).text.toString()
                        ).fromIOToMain({ refreshVideoInfo() }) { t ->
                            showError(getCauseIfRuntime(t))
                        })
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
        }
    }

    private fun resolveSubtitle() {
        view?.showSubtitle(if (video != null) video?.title else null)
    }

    override fun onGuiCreated(viewHost: IVideoPreviewView) {
        super.onGuiCreated(viewHost)
        when {
            video != null -> {
                displayFullVideoInfo(viewHost, video ?: return)
            }

            refreshingNow -> {
                viewHost.displayLoading()
            }

            else -> {
                viewHost.displayLoadingError()
            }
        }
        resolveSubtitle()
    }

    private fun displayFullVideoInfo(view: IVideoPreviewView, video: Video) {
        view.displayVideoInfo(video)
        view.displayCommentCount(video.commentsCount)
        view.setCommentButtonVisible(video.isCanComment || video.commentsCount > 0 || isMy)
        view.displayLikes(video.likesCount, video.isUserLikes)
        if (owner == null) {
            appendJob(
                ownerInteractor.getBaseOwnerInfo(
                    accountId,
                    ownerId,
                    IOwnersRepository.MODE_ANY
                )
                    .fromIOToMain({ info -> onOwnerReceived(info) }) { e ->
                        showError(e)
                    })
        } else {
            owner?.let { view.displayOwner(it) }
        }
    }

    private fun onOwnerReceived(info: Owner) {
        owner = info
        owner?.let {
            view?.displayOwner(it)
        }
    }

    fun fireOpenOwnerClicked() {
        view?.showOwnerWall(
            accountId,
            ownerId
        )
    }

    private fun onVideoInfoGetError(throwable: Throwable) {
        setRefreshingNow(false)
        showError(throwable)
        if (video == null) {
            view?.displayLoadingError()
        }
    }

    private fun onActualInfoReceived(video: Video) {
        setRefreshingNow(false)
        if (this.video != null && video.date == 0L && (this.video ?: return).date != 0L) {
            video.setDate((this.video ?: return).date)
        }
        if (this.video != null && video.addingDate == 0L && (this.video
                ?: return).addingDate != 0L
        ) {
            video.setAddingDate((this.video ?: return).addingDate)
        }
        this.video = video
        resolveSubtitle()
        view?.let {
            displayFullVideoInfo(
                it,
                video
            )
        }
    }

    private fun refreshVideoInfo() {
        setRefreshingNow(true)
        if (video == null) {
            view?.displayLoading()
        }
        appendJob(
            interactor.getById(accountId, ownerId, videoId, accessKey, false)
                .fromIOToMain({ video -> onActualInfoReceived(video) }) { throwable ->
                    onVideoInfoGetError(
                        getCauseIfRuntime(throwable)
                    )
                })
    }

    private val isMy: Boolean
        get() = accountId == ownerId

    fun fireOptionViewCreated(view: IVideoPreviewView.IOptionView) {
        view.setCanAdd(video != null && !isMy && video?.isCanAdd == true)
        view.setIsMy(video != null && isMy)
    }

    private fun onAddComplete() {
        view?.showSuccessToast()
    }

    private fun onAddError(throwable: Throwable) {
        showError(throwable)
    }

    fun fireAddToMyClick() {
        appendJob(
            interactor.addToMy(accountId, accountId, ownerId, videoId)
                .fromIOToMain({ onAddComplete() }) { throwable ->
                    onAddError(
                        getCauseIfRuntime(throwable)
                    )
                })
    }

    fun fireDeleteMyClick() {
        appendJob(
            interactor.delete(accountId, videoId, ownerId, accountId)
                .fromIOToMain({ onAddComplete() }) { throwable ->
                    onAddError(
                        getCauseIfRuntime(throwable)
                    )
                })
    }

    fun fireCopyUrlClick(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText(
            getString(R.string.link),
            "https://vk.com/video" + video?.ownerId + "_" + video?.id
        )
        clipboard?.setPrimaryClip(clip)
        createCustomToast(context).showToast(R.string.copied_url)
    }

    fun fireOwnerClick(ownerId: Long) {
        view?.showOwnerWall(
            accountId,
            ownerId
        )
    }

    fun fireShareClick() {
        video?.let {
            view?.displayShareDialog(
                accountId,
                it,
                !isMy
            )
        }
    }

    fun fireCommentsClick() {
        video?.let {
            val commented = Commented.from(it)
            view?.showComments(
                accountId,
                commented
            )
        }
    }

    private fun onLikesResponse(count: Int, userLikes: Boolean) {
        video?.let {
            it.setLikesCount(count)
            it.setUserLikes(userLikes)
            view?.displayLikes(
                count,
                userLikes
            )
        }
    }

    private fun onLikeError(throwable: Throwable) {
        showError(throwable)
    }

    fun fireLikeClick() {
        if (Settings.get().main().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        video?.let {
            val add = !it.isUserLikes
            appendJob(
                interactor.likeOrDislike(accountId, ownerId, videoId, accessKey, add)
                    .fromIOToMain(
                        { pair: Pair<Int, Boolean> -> onLikesResponse(pair.first, pair.second) }
                    ) { throwable -> onLikeError(getCauseIfRuntime(throwable)) })
        }
    }

    fun fireLikeLongClick() {
        video?.let {
            view?.goToLikes(
                accountId,
                "video",
                it.ownerId,
                it.id
            )
        }
    }

    fun firePlayClick() {
        video?.let {
            view?.showVideoPlayMenu(
                accountId,
                it
            )
        }
    }

    fun fireAutoPlayClick() {
        video?.let {
            view?.doAutoPlayVideo(
                accountId,
                it
            )
        }
    }

    fun fireTryAgainClick() {
        refreshVideoInfo()
    }

    init {
        if (savedInstanceState == null) {
            this.video = video
        } else {
            this.video = savedInstanceState.getParcelableCompat("video")
        }
        refreshVideoInfo()
    }
}