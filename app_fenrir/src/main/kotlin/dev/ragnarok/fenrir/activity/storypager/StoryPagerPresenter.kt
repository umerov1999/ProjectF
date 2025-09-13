package dev.ragnarok.fenrir.activity.storypager

import android.os.Bundle
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.Includes.storyPlayerFactory
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.media.story.IStoryPlayer
import dev.ragnarok.fenrir.media.story.IStoryPlayer.IStatusChangeListener
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.VideoSize
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.DownloadWorkUtils.makeLegalFilename
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import java.io.File
import java.util.Calendar
import kotlin.math.abs

class StoryPagerPresenter(
    accountId: Long,
    private val mStories: ArrayList<Story>,
    private var mCurrentIndex: Int,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IStoryPagerView>(accountId, savedInstanceState),
    IStatusChangeListener, IStoryPlayer.IVideoSizeChangeListener {
    private val storiesInteractor = InteractorFactory.createStoriesInteractor()
    private var mStoryPlayer: IStoryPlayer? = null
    private var isPlayBackSpeed = false
    private var loadingNow = false

    fun isStoryIsVideo(pos: Int): Boolean {
        return if (mStories.isEmpty()) {
            false
        } else {
            mStories[pos].isStoryIsVideo()
        }
    }

    fun togglePlaybackSpeed(): Boolean {
        isPlayBackSpeed = !isPlayBackSpeed
        mStoryPlayer?.setPlaybackSpeed(isPlayBackSpeed)
        return isPlayBackSpeed
    }

    fun getStory(pos: Int): Story? {
        return mStories.getOrNull(pos)
    }

    override fun onGuiCreated(viewHost: IStoryPagerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mStories.size, mCurrentIndex)
        viewHost.displayListLoading(loadingNow)
        resolveToolbarTitle()
        resolvePlayerDisplay()
        resolveAspectRatio()
        resolvePreparingProgress()
        resolveToolbarSubtitle()
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
            mStories.size
        )
    }

    private fun resolvePlayerDisplay() {
        if (guiIsReady) {
            view?.attachDisplayToPlayer(
                mCurrentIndex,
                mStoryPlayer
            )
        } else {
            mStoryPlayer?.setDisplay(null)
        }
    }

    private fun initStoryPlayer() {
        val story = mStories[mCurrentIndex]
        if (story.video == null) {
            if (mStoryPlayer != null) {
                val old: IStoryPlayer? = mStoryPlayer
                mStoryPlayer = null
                old?.release()
            }
            return
        }
        val update: Boolean = mStoryPlayer != null
        val url = firstNonEmptyString(
            story.video?.mp4link2160, story.video?.mp4link1440,
            story.video?.mp4link1080, story.video?.mp4link720, story.video?.mp4link480,
            story.video?.mp4link360, story.video?.mp4link240
        )
        if (url == null) {
            view?.showError(R.string.unable_to_play_file)
            return
        }
        if (!update) {
            mStoryPlayer = storyPlayerFactory.createStoryPlayer(url, false)
            mStoryPlayer?.setPlaybackSpeed(isPlayBackSpeed)
            mStoryPlayer?.addStatusChangeListener(this)
            mStoryPlayer?.addVideoSizeChangeListener(this)
        } else {
            mStoryPlayer?.updateSource(url)
        }
        try {
            mStoryPlayer?.play()
        } catch (_: Exception) {
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

    private val isMy: Boolean
        get() = mStories.isEmpty() || mStories[mCurrentIndex].ownerId == accountId

    private fun resolveAspectRatio() {
        if (mStoryPlayer == null) {
            return
        }
        val size = mStoryPlayer?.videoSize
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
            mStoryPlayer != null && mStoryPlayer?.playerStatus == IStoryPlayer.IStatus.PREPARING
        view?.setPreparingProgressVisible(
            mCurrentIndex,
            preparing
        )
    }

    private fun resolveToolbarSubtitle() {
        if (mStories.isEmpty()) {
            return
        }
        view?.setToolbarSubtitle(
            mStories[mCurrentIndex],
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
    }

    fun fireHolderCreate(adapterPosition: Int) {
        if (!isStoryIsVideo(adapterPosition)) return
        val isProgress =
            adapterPosition == mCurrentIndex && (mStoryPlayer == null || mStoryPlayer?.playerStatus == IStoryPlayer.IStatus.PREPARING)
        var size = if (mStoryPlayer == null) null else mStoryPlayer?.videoSize
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

    fun fireShareButtonClick() {
        if (mStories.isEmpty()) {
            return
        }
        val story = mStories[mCurrentIndex]
        view?.onShare(story, accountId)
    }

    fun fireDownloadButtonClick() {
        if (mStories.isEmpty()) {
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

    override fun onGuiPaused() {
        super.onGuiPaused()
        mStoryPlayer?.pause()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        if (mStoryPlayer != null) {
            try {
                mStoryPlayer?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyed() {
        if (mStoryPlayer != null) {
            mStoryPlayer?.release()
        }
        super.onDestroyed()
    }

    private fun downloadImpl() {
        val story = mStories[mCurrentIndex]
        if (story.photo != null) doSaveOnDrive(story)
        if (story.video != null) {
            val url = firstNonEmptyString(
                story.video?.mp4link2160, story.video?.mp4link1440,
                story.video?.mp4link1080, story.video?.mp4link720, story.video?.mp4link480,
                story.video?.mp4link360, story.video?.mp4link240
            )
            story.video?.setTitle(story.owner?.fullName)
            url.nonNullNoEmpty {
                story.video.requireNonNull { s ->
                    view?.downloadVideo(s, it, "Story")
                }
            }
        }
    }

    private fun doSaveOnDrive(photo: Story) {
        val dir = File(Settings.get().main().photoDir)
        if (!dir.isDirectory) {
            val created = dir.mkdirs()
            if (!created) {
                view?.showError("Can't create directory $dir")
                return
            }
        } else dir.setLastModified(Calendar.getInstance().timeInMillis)
        photo.photo?.let {
            downloadResult(photo.owner?.fullName?.let { it1 ->
                makeLegalFilename(
                    it1,
                    null
                )
            }, dir, it)
        }
    }

    private fun transform_owner(owner_id: Long): String {
        return if (owner_id < 0) "club" + abs(owner_id) else "id$owner_id"
    }

    private fun downloadResult(Prefix: String?, dirF: File, photo: Photo) {
        var dir = dirF
        if (Prefix != null && Settings.get().main().isPhoto_to_user_dir) {
            val dir_final = File(dir.absolutePath + "/" + Prefix)
            if (!dir_final.isDirectory) {
                val created = dir_final.mkdirs()
                if (!created) {
                    view?.showError("Can't create directory $dir_final")
                    return
                }
            } else dir_final.setLastModified(Calendar.getInstance().timeInMillis)
            dir = dir_final
        }
        val url = photo.getUrlForSize(PhotoSize.W, true)
        if (url != null) {
            view?.downloadPhoto(
                url,
                dir.absolutePath,
                (if (Prefix != null) Prefix + "_" else "") + transform_owner(photo.ownerId) + "_" + photo.getObjectId()
            )
        }
    }

    override fun onPlayerStatusChange(
        player: IStoryPlayer,
        previousStatus: Int,
        currentStatus: Int
    ) {
        if (mStoryPlayer === player) {
            if (currentStatus == IStoryPlayer.IStatus.ENDED) {
                view?.onNext()
                return
            }
            resolvePreparingProgress()
            resolvePlayerDisplay()
        }
    }

    override fun onVideoSizeChanged(player: IStoryPlayer, size: VideoSize) {
        if (mStoryPlayer === player) {
            resolveAspectRatio()
        }
    }

    fun receiveAvailableStories() {
        loadingNow = true
        view?.displayListLoading(loadingNow)
        appendJob(
            storiesInteractor.getStories(accountId, null).fromIOToMain(
                {
                    if (it.isEmpty()) {
                        view?.customToast?.showToastError(R.string.list_is_empty)
                        view?.endAction()
                    } else {
                        loadingNow = false
                        view?.displayListLoading(loadingNow)
                        mCurrentIndex = 0
                        mStories.clear()
                        mStories.addAll(it)
                        view?.updateCount(mStories.size)
                        view?.notifyDataSetChanged()
                        initStoryPlayer()
                        resolveToolbarTitle()
                        resolveToolbarSubtitle()
                    }
                },
                {
                    loadingNow = false
                    view?.displayListLoading(loadingNow)
                    view?.showThrowable(it)
                    view?.endAction()
                })
        )
    }

    companion object {
        private val DEF_SIZE = VideoSize(1, 1)
    }

    init {
        if (mStories.isNotEmpty()) {
            initStoryPlayer()
        } else {
            mCurrentIndex = -1
            receiveAvailableStories()
        }
    }
}