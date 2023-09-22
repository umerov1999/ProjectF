package dev.ragnarok.fenrir.activity.shortvideopager

import android.os.Bundle
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.Includes.storyPlayerFactory
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.media.story.IStoryPlayer
import dev.ragnarok.fenrir.media.story.IStoryPlayer.IStatusChangeListener
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.VideoSize
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString

class ShortVideoPagerPresenter(
    accountId: Long,
    private val ownerId: Long?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IShortVideoPagerView>(accountId, savedInstanceState),
    IStatusChangeListener, IStoryPlayer.IVideoSizeChangeListener {
    private var mShortVideoPlayer: IStoryPlayer? = null
    private val mShortVideos: ArrayList<Video> = ArrayList()
    private val shortVideosInteractor = InteractorFactory.createStoriesInteractor()
    private val interactor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    private var mCurrentIndex = -1
    private var nextFrom: String? = null
    private var isEndContent: Boolean = false
    private var loadingNow = false
    private var isPlayBackSpeed = false

    override fun onGuiCreated(viewHost: IShortVideoPagerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mShortVideos.size, mCurrentIndex)
        viewHost.displayListLoading(loadingNow)
        resolveToolbarTitle()
        resolvePlayerDisplay()
        resolveAspectRatio()
        resolvePreparingProgress()
        resolveToolbarSubtitle()
    }

    fun togglePlaybackSpeed(): Boolean {
        isPlayBackSpeed = !isPlayBackSpeed
        mShortVideoPlayer?.setPlaybackSpeed(isPlayBackSpeed)
        return isPlayBackSpeed
    }

    private fun receiveShortVideos() {
        loadingNow = true
        view?.displayListLoading(loadingNow)
        appendDisposable(
            shortVideosInteractor.getShortVideos(accountId, ownerId, nextFrom, 25).fromIOToMain()
                .subscribe(
                    { onActualShortVideosReceived(nextFrom, it.first, it.second) },
                    {
                        loadingNow = false
                        view?.displayListLoading(loadingNow)
                        view?.showThrowable(it)
                    })
        )
    }

    private fun onActualShortVideosReceived(
        startFrom: String?,
        shortVideos: List<Video>,
        nextFrom: String?
    ) {
        loadingNow = false
        view?.displayListLoading(loadingNow)
        this.nextFrom = nextFrom
        isEndContent = nextFrom.isNullOrEmpty()
        if (startFrom.isNullOrEmpty()) {
            if (shortVideos.isEmpty()) {
                return
            }
            mCurrentIndex = 0
            mShortVideos.clear()
            mShortVideos.addAll(shortVideos)
            view?.updateCount(mShortVideos.size)
            view?.notifyDataSetChanged()
            initStoryPlayer()
            resolveToolbarTitle()
            resolveToolbarSubtitle()
        } else {
            val startSize = mShortVideos.size
            mShortVideos.addAll(shortVideos)
            view?.updateCount(mShortVideos.size)
            view?.notifyDataAdded(
                startSize,
                shortVideos.size
            )
            resolveToolbarTitle()
            resolveToolbarSubtitle()
        }
    }

    fun fireSurfaceCreated(adapterPosition: Int) {
        if (mCurrentIndex == adapterPosition) {
            resolvePlayerDisplay()
        }
    }

    private fun resolveToolbarTitle() {
        view?.setToolbarTitle(
            R.string.image_number,
            mCurrentIndex + 1,
            mShortVideos.size
        )
    }

    private fun resolvePlayerDisplay() {
        if (guiIsReady) {
            view?.attachDisplayToPlayer(
                mCurrentIndex,
                mShortVideoPlayer
            )
        } else {
            mShortVideoPlayer?.setDisplay(null)
        }
    }

    private fun initStoryPlayer() {
        val update: Boolean = mShortVideoPlayer != null
        val shortVideo = mShortVideos[mCurrentIndex]
        val url = firstNonEmptyString(
            shortVideo.mp4link2160, shortVideo.mp4link1440,
            shortVideo.mp4link1080, shortVideo.mp4link720, shortVideo.mp4link480,
            shortVideo.mp4link360, shortVideo.mp4link240
        )
        if (url == null) {
            view?.showError(R.string.unable_to_play_file)
            return
        }
        if (!update) {
            mShortVideoPlayer = storyPlayerFactory.createStoryPlayer(url, false)
            mShortVideoPlayer?.setPlaybackSpeed(isPlayBackSpeed)
            mShortVideoPlayer?.addStatusChangeListener(this)
            mShortVideoPlayer?.addVideoSizeChangeListener(this)
        } else {
            mShortVideoPlayer?.updateSource(url)
        }
        try {
            mShortVideoPlayer?.play()
        } catch (e: Exception) {
            view?.showError(R.string.unable_to_play_file)
        }
    }

    private fun selectPage(position: Int) {
        if (mCurrentIndex == position) {
            return
        }
        mCurrentIndex = position
        initStoryPlayer()
    }

    private fun resolveAspectRatio() {
        if (mShortVideoPlayer == null) {
            return
        }
        val size = mShortVideoPlayer?.videoSize
        if (size != null) {
            view?.setAspectRatioAt(
                mCurrentIndex,
                size.width.coerceAtLeast(1),
                size.height.coerceAtLeast(1)
            )
        } else {
            view?.setAspectRatioAt(
                mCurrentIndex,
                1,
                1
            )
        }
    }

    private fun resolvePreparingProgress() {
        val preparing =
            mShortVideoPlayer != null && mShortVideoPlayer?.playerStatus == IStoryPlayer.IStatus.PREPARING
        view?.setPreparingProgressVisible(
            mCurrentIndex,
            preparing
        )
    }

    private fun resolveToolbarSubtitle() {
        if (mShortVideos.isEmpty()) {
            return
        }
        view?.setToolbarSubtitle(
            mShortVideos[mCurrentIndex],
            accountId, isPlayBackSpeed
        )
    }

    fun firePageSelected(position: Int) {
        if (mCurrentIndex == position) {
            return
        }
        selectPage(position)
        resolveToolbarTitle()
        resolveToolbarSubtitle()
        resolvePreparingProgress()
        if (mCurrentIndex >= mShortVideos.size - 1 && !isEndContent && !loadingNow) {
            receiveShortVideos()
        }
    }

    fun fireHolderCreate(adapterPosition: Int) {
        val isProgress =
            adapterPosition == mCurrentIndex && (mShortVideoPlayer == null || mShortVideoPlayer?.playerStatus == IStoryPlayer.IStatus.PREPARING)
        var size = if (mShortVideoPlayer == null) null else mShortVideoPlayer?.videoSize
        if (size == null) {
            size = DEF_SIZE
        }
        if (size.width <= 0) {
            size.setWidth(1)
        }
        if (size.height <= 0) {
            size.setHeight(1)
        }
        view?.configHolder(
            adapterPosition,
            isProgress,
            size.width,
            size.width
        )
    }

    fun fireCommentsClick() {
        if (mShortVideos.isEmpty()) {
            return
        }
        mShortVideos[mCurrentIndex].let {
            val commented = Commented.from(it)
            view?.showComments(
                accountId,
                commented
            )
        }
    }

    private fun onLikesResponse(count: Int, userLikes: Boolean, index: Int) {
        mShortVideos[index].let {
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
        if (mShortVideos.isEmpty()) {
            return
        }
        mShortVideos[mCurrentIndex].let {
            val add = !it.isUserLikes
            appendDisposable(interactor.likeOrDislike(
                accountId,
                it.ownerId,
                it.id,
                it.accessKey,
                add
            )
                .fromIOToMain()
                .subscribe(
                    { pair: Pair<Int, Boolean> ->
                        onLikesResponse(
                            pair.first,
                            pair.second,
                            mCurrentIndex
                        )
                    }
                ) { throwable -> onLikeError(Utils.getCauseIfRuntime(throwable)) })
        }
    }

    fun fireLikeLongClick() {
        if (mShortVideos.isEmpty()) {
            return
        }
        mShortVideos[mCurrentIndex].let {
            view?.goToLikes(
                accountId,
                "video",
                it.ownerId,
                it.id
            )
        }
    }

    fun fireShareButtonClick() {
        if (mShortVideos.isEmpty()) {
            return
        }
        val shortVideo = mShortVideos[mCurrentIndex]
        view?.onShare(shortVideo, accountId)
    }

    fun fireDownloadButtonClick() {
        if (mShortVideos.isEmpty()) {
            return
        }
        if (!hasReadWriteStoragePermission(instance)) {
            view?.requestWriteExternalStoragePermission()
            return
        }
        downloadImpl()
    }

    private fun onWritePermissionResolved() {
        if (hasReadWriteStoragePermission(instance)) {
            downloadImpl()
        }
    }

    fun fireWritePermissionResolved() {
        onWritePermissionResolved()
    }

    public override fun onGuiPaused() {
        super.onGuiPaused()
        mShortVideoPlayer?.pause()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        if (mShortVideoPlayer != null) {
            try {
                mShortVideoPlayer?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyed() {
        if (mShortVideoPlayer != null) {
            mShortVideoPlayer?.release()
        }
        super.onDestroyed()
    }

    private fun downloadImpl() {
        val shortVideo = mShortVideos[mCurrentIndex]
        val url = firstNonEmptyString(
            shortVideo.mp4link2160, shortVideo.mp4link1440,
            shortVideo.mp4link1080, shortVideo.mp4link720, shortVideo.mp4link480,
            shortVideo.mp4link360, shortVideo.mp4link240
        )
        shortVideo.setTitle(shortVideo.optionalOwner?.fullName)
        url.nonNullNoEmpty {
            view?.downloadVideo(shortVideo, it, "ShortVideo")
        }
    }

    override fun onPlayerStatusChange(
        player: IStoryPlayer,
        previousStatus: Int,
        currentStatus: Int
    ) {
        if (mShortVideoPlayer === player) {
            if (currentStatus == IStoryPlayer.IStatus.ENDED) {
                view?.onNext()
                return
            }
            resolvePreparingProgress()
            resolvePlayerDisplay()
        }
    }

    override fun onVideoSizeChanged(player: IStoryPlayer, size: VideoSize) {
        if (mShortVideoPlayer === player) {
            resolveAspectRatio()
        }
    }

    companion object {
        private val DEF_SIZE = VideoSize(1, 1)
    }

    init {
        receiveShortVideos()
    }
}
