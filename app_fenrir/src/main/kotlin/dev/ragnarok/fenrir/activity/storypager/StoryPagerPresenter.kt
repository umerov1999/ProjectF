package dev.ragnarok.fenrir.activity.storypager

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.Includes.storyPlayerFactory
import dev.ragnarok.fenrir.R
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
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadPhoto
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadVideo
import dev.ragnarok.fenrir.util.DownloadWorkUtils.makeLegalFilename
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import java.io.File
import java.util.Calendar
import kotlin.math.abs

class StoryPagerPresenter(
    accountId: Long,
    private val mStories: ArrayList<Story>,
    private var mCurrentIndex: Int,
    private val context: Context,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IStoryPagerView>(accountId, savedInstanceState),
    IStatusChangeListener, IStoryPlayer.IVideoSizeChangeListener {
    private var mStoryPlayer: IStoryPlayer? = null
    fun isStoryIsVideo(pos: Int): Boolean {
        return mStories[pos].photo == null && mStories[pos].video != null
    }

    fun getStory(pos: Int): Story {
        return mStories[pos]
    }

    override fun onGuiCreated(viewHost: IStoryPagerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mStories.size, mCurrentIndex)
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
        if (mStoryPlayer != null) {
            val old: IStoryPlayer? = mStoryPlayer
            mStoryPlayer = null
            old?.release()
        }
        val story = mStories[mCurrentIndex]
        if (story.video == null) {
            return
        }
        val url = firstNonEmptyString(
            story.video?.mp4link2160, story.video?.mp4link1440,
            story.video?.mp4link1080, story.video?.mp4link720, story.video?.mp4link480,
            story.video?.mp4link360, story.video?.mp4link240
        )
        if (url == null) {
            view?.showError(R.string.unable_to_play_file)
            return
        }
        mStoryPlayer = storyPlayerFactory.createStoryPlayer(url, false)
        mStoryPlayer?.addStatusChangeListener(this)
        mStoryPlayer?.addVideoSizeChangeListener(this)
        try {
            mStoryPlayer?.play()
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

    private val isMy: Boolean
        get() = mStories[mCurrentIndex].ownerId == accountId

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
        view?.setToolbarSubtitle(
            mStories[mCurrentIndex],
            accountId
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
        val story = mStories[mCurrentIndex]
        view?.onShare(story, accountId)
    }

    fun fireDownloadButtonClick() {
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
        mStoryPlayer?.pause()
    }

    public override fun onGuiResumed() {
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
                    doDownloadVideo(context, s, it, "Story")
                }
            }
        }
    }

    private fun doSaveOnDrive(photo: Story) {
        val dir = File(Settings.get().other().photoDir)
        if (!dir.isDirectory) {
            val created = dir.mkdirs()
            if (!created) {
                view?.showError("Can't create directory $dir")
                return
            }
        } else dir.setLastModified(Calendar.getInstance().time.time)
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
        if (Prefix != null && Settings.get().other().isPhoto_to_user_dir) {
            val dir_final = File(dir.absolutePath + "/" + Prefix)
            if (!dir_final.isDirectory) {
                val created = dir_final.mkdirs()
                if (!created) {
                    view?.showError("Can't create directory $dir_final")
                    return
                }
            } else dir_final.setLastModified(Calendar.getInstance().time.time)
            dir = dir_final
        }
        val url = photo.getUrlForSize(PhotoSize.W, true)
        if (url != null) {
            doDownloadPhoto(
                context,
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

    companion object {
        private val DEF_SIZE = VideoSize(1, 1)
    }

    init {
        initStoryPlayer()
    }
}