package dev.ragnarok.fenrir.activity.shortvideopager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.util.size
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.BaseMvpActivity
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity
import dev.ragnarok.fenrir.activity.slidr.Slidr
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.media.story.IStoryPlayer
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.HelperSimple
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.ExpandableSurfaceView
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView
import java.lang.ref.WeakReference

class ShortVideoPagerActivity : BaseMvpActivity<ShortVideoPagerPresenter, IShortVideoPagerView>(),
    IShortVideoPagerView, PlaceProvider, AppStyleable {
    private val mHolderSparseArray = SparseArray<WeakReference<Holder>>()
    private var mViewPager: ViewPager2? = null
    private var mToolbar: Toolbar? = null
    private var mAvatar: ImageView? = null
    private var mPlaySpeed: ImageView? = null
    private var transformation: Transformation? = null
    private var mDownload: CircleCounterButton? = null
    private var mShare: CircleCounterButton? = null
    private var likeButton: CircleCounterButton? = null
    private var commentsButton: CircleCounterButton? = null
    private var mFullscreen = false
    private var mContentRoot: View? = null
    private var helpDisposable = CancelableJob()
    private var mAdapter: Adapter? = null
    private var mLoadingProgressBarDispose = CancelableJob()
    private var mLoadingProgressBarLoaded = false
    private var mLoadingProgressBar: ThorVGLottieView? = null
    private var shortVideoDuration: TextView? = null
    private var playDispose = CancelableJob()

    @get:LayoutRes
    override val noMainContentView: Int
        get() = R.layout.activity_shortvideo_pager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFullscreen = savedInstanceState?.getBoolean("mFullscreen") == true
        transformation = CurrentTheme.createTransformationForAvatar()
        mContentRoot = findViewById<RelativeLayout>(R.id.shortvideo_pager_root)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        mAvatar = findViewById(R.id.toolbar_avatar)
        mViewPager = findViewById(R.id.view_pager)
        likeButton = findViewById(R.id.like_button)
        commentsButton = findViewById(R.id.comments_button)
        shortVideoDuration = findViewById(R.id.item_short_video_duration)
        mPlaySpeed = findViewById(R.id.toolbar_play_speed)
        mPlaySpeed?.setOnClickListener {
            val stateSpeed = presenter?.togglePlaybackSpeed() == true
            Utils.setTint(
                mPlaySpeed,
                if (stateSpeed) CurrentTheme.getColorPrimary(this) else "#ffffff".toColor()
            )
        }
        commentsButton?.setOnClickListener {
            presenter?.fireCommentsClick()
        }
        likeButton?.setOnClickListener {
            presenter?.fireLikeClick()
        }
        likeButton?.setOnLongClickListener {
            presenter?.fireLikeLongClick()
            true
        }
        mLoadingProgressBar = findViewById(R.id.loading_progress_bar)
        mViewPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        val mHelper = findViewById<ThorVGLottieView?>(R.id.swipe_helper)
        if (HelperSimple.needHelp(HelperSimple.SHORT_VIDEO_HELPER, 2)) {
            mHelper?.visibility = View.VISIBLE
            mHelper?.fromRes(
                dev.ragnarok.fenrir_common.R.raw.story_guide_hand_swipe,
                intArrayOf(0x333333, CurrentTheme.getColorSecondary(this))
            )
            mHelper?.startAnimation()
            helpDisposable += delayTaskFlow(5000).toMain {
                mHelper?.clearAnimationDrawable(
                    callSuper = true, clearState = true,
                    cancelTask = true
                )
                mHelper?.visibility = View.GONE
            }
        } else {
            mHelper?.visibility = View.GONE
        }
        mDownload = findViewById(R.id.button_download)
        mShare = findViewById(R.id.button_share)
        mShare?.setOnClickListener { presenter?.fireShareButtonClick() }
        mDownload?.setOnClickListener { presenter?.fireDownloadButtonClick() }
        resolveFullscreenViews()
        val mButtonsRoot: View = findViewById(R.id.buttons)

        Slidr.attach(
            this,
            SlidrConfig.Builder().setAlphaForView(false).fromUnColoredToColoredStatusBar(true)
                .position(SlidrPosition.LEFT)
                .listener(object : SlidrListener {
                    override fun onSlideStateChanged(state: Int) {

                    }

                    @SuppressLint("Range")
                    override fun onSlideChange(percent: Float) {
                        var tmp = 1f - percent
                        tmp *= 4
                        tmp = Utils.clamp(1f - tmp, 0f, 1f)
                        mContentRoot?.setBackgroundColor(Color.argb(tmp, 0f, 0f, 0f))
                        mButtonsRoot.alpha = tmp
                        mToolbar?.alpha = tmp
                        mViewPager?.alpha = Utils.clamp(percent, 0f, 1f)
                    }

                    override fun onSlideOpened() {

                    }

                    override fun onSlideClosed(): Boolean {
                        Utils.finishActivityImmediate(this@ShortVideoPagerActivity)
                        return true
                    }

                }).build()
        )
    }

    override fun displayLikes(count: Int, userLikes: Boolean) {
        likeButton?.setIcon(if (userLikes) R.drawable.heart_filled else R.drawable.heart)
        likeButton?.count = count
        likeButton?.isActive = userLikes
    }

    override fun displayCommentCount(count: Int) {
        commentsButton?.count = count
    }

    override fun showComments(accountId: Long, commented: Commented) {
        PlaceFactory.getCommentsPlace(accountId, commented, null).tryOpenWith(this)
    }

    override fun goToLikes(accountId: Long, type: String, ownerId: Long, id: Int) {
        PlaceFactory.getLikesCopiesPlace(
            accountId,
            type,
            ownerId,
            id,
            ILikesInteractor.FILTER_LIKES
        )
            .tryOpenWith(this)
    }

    override fun downloadVideo(video: Video, url: String, Res: String) {
        DownloadWorkUtils.doDownloadVideo(this, video, url, Res)
    }

    override fun displayListLoading(loading: Boolean) {
        mLoadingProgressBarDispose.cancel()
        if (loading) {
            mLoadingProgressBarDispose += delayTaskFlow(300).toMain {
                mLoadingProgressBarLoaded = true
                mLoadingProgressBar?.visibility = View.VISIBLE
                mLoadingProgressBar?.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.loading,
                    intArrayOf(
                        0x000000,
                        Color.WHITE,
                        0x777777,
                        Color.WHITE
                    )
                )
                mLoadingProgressBar?.startAnimation()
            }
        } else if (mLoadingProgressBarLoaded) {
            mLoadingProgressBarLoaded = false
            mLoadingProgressBar?.visibility = View.GONE
            mLoadingProgressBar?.clearAnimationDrawable(
                callSuper = true, clearState = true,
                cancelTask = true
            )
        }
    }

    override fun showMessage(@StringRes message: Int, error: Boolean) {
        CustomSnackbars.createCustomSnackbars(mContentRoot, null, true)
            ?.setDurationSnack(Snackbar.LENGTH_LONG)?.let {
                if (error) {
                    it.coloredSnack(message, "#ff0000".toColor()).show()
                } else {
                    it.defaultSnack(message).show()
                }
            }
    }

    override fun showMessage(message: String, error: Boolean) {
        CustomSnackbars.createCustomSnackbars(mContentRoot, null, true)
            ?.setDurationSnack(Snackbar.LENGTH_LONG)?.let {
                if (error) {
                    it.coloredSnack(message, "#ff0000".toColor()).show()
                } else {
                    it.defaultSnack(message).show()
                }
            }
    }

    override fun updateCount(count: Int) {
        mAdapter?.updateCount(count)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdded(pos: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(pos, count)
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) player.dismiss()
                AudioPlayerFragment.newInstance(args).show(supportFragmentManager, "audio_player")
            }

            else -> Utils.openPlaceWithSwipebleActivity(this, place)
        }
    }

    override fun hideMenu(hide: Boolean) {}
    override fun openMenu(open: Boolean) {}

    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val w = window
        @Suppress("deprecation")
        if (!hasVanillaIceCreamTarget()) {
            w.statusBarColor =
                if (colored) getStatusBarColor(this) else getStatusBarNonColored(
                    this
                )
            w.navigationBarColor =
                if (colored) getNavigationBarColor(this) else Color.BLACK
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                w.isNavigationBarContrastEnforced = colored
            }
        }
        val ins = WindowInsetsControllerCompat(w, w.decorView)
        ins.isAppearanceLightStatusBars = invertIcons
        ins.isAppearanceLightNavigationBars = invertIcons
    }

    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        lazyPresenter { fireWritePermissionResolved() }
    }

    override fun requestWriteExternalStoragePermission() {
        requestWritePermission.launch()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("mFullscreen", mFullscreen)
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(colored = false, invertIcons = false)
            .build()
            .apply(this)
    }

    internal fun toggleFullscreen() {
        mFullscreen = !mFullscreen
        resolveFullscreenViews()
    }

    private fun resolveFullscreenViews() {
        mToolbar?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
        mDownload?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
        mShare?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
        likeButton?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
        commentsButton?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): ShortVideoPagerPresenter {
        val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
        val ownerId: Long? = if (!requireArguments().getBoolean(Extra.NO_OWNER_ID)) {
            requireArguments().getLong(Extra.OWNER_ID)
        } else {
            null
        }
        return ShortVideoPagerPresenter(
            aid,
            ownerId,
            saveInstanceState
        )
    }

    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            playDispose.cancel()
            playDispose += delayTaskFlow(400)
                .toMain { presenter?.firePageSelected(position) }
        }
    }

    override fun displayData(pageCount: Int, selectedIndex: Int) {
        mViewPager?.unregisterOnPageChangeCallback(pageChangeListener)
        mAdapter = Adapter(pageCount)
        mViewPager?.adapter = mAdapter
        mViewPager?.setCurrentItem(selectedIndex, false)
        mViewPager?.registerOnPageChangeCallback(pageChangeListener)
    }

    override fun setAspectRatioAt(position: Int, w: Int, h: Int) {
        findByPosition(position)?.setAspectRatio(w, h)
    }

    override fun setPreparingProgressVisible(position: Int, preparing: Boolean) {
        for (i in 0 until mHolderSparseArray.size) {
            val key = mHolderSparseArray.keyAt(i)
            val holder = findByPosition(key)
            val isCurrent = position == key
            val progressVisible = isCurrent && preparing
            holder?.setProgressVisible(progressVisible)
            holder?.setSurfaceVisible(if (isCurrent && !preparing) View.VISIBLE else View.GONE)
        }
    }

    override fun attachDisplayToPlayer(adapterPosition: Int, storyPlayer: IStoryPlayer?) {
        val holder = findByPosition(adapterPosition)
        if (holder?.isSurfaceReady == true) {
            storyPlayer?.setDisplay(holder.mSurfaceHolder)
        }
    }

    override fun setToolbarTitle(@StringRes titleRes: Int, vararg params: Any?) {
        supportActionBar?.title = getString(titleRes, *params)
    }

    override fun setToolbarSubtitle(shortVideo: Video, account_id: Long, isPlaySpeed: Boolean) {
        supportActionBar?.subtitle = shortVideo.optionalOwner?.fullName
        mAvatar?.setOnClickListener {
            shortVideo.optionalOwner?.let { it1 ->
                PlaceFactory.getOwnerWallPlace(account_id, it1)
                    .tryOpenWith(this)
            }
        }
        mAvatar?.let {
            ViewUtils.displayAvatar(
                it,
                transformation,
                shortVideo.optionalOwner?.maxSquareAvatar,
                Constants.PICASSO_TAG
            )
        }
        Utils.setTint(
            mPlaySpeed,
            if (isPlaySpeed) CurrentTheme.getColorPrimary(this) else "#ffffff".toColor()
        )
        shortVideoDuration?.text = AppTextUtils.getDurationString(shortVideo.duration)
        displayLikes(shortVideo.likesCount, shortVideo.isUserLikes)
        displayCommentCount(shortVideo.commentsCount)
    }

    override fun onShare(shortVideo: Video, account_id: Long) {
        SendAttachmentsActivity.startForSendAttachments(this, account_id, shortVideo)
    }

    override fun configHolder(
        adapterPosition: Int,
        progress: Boolean,
        aspectRatioW: Int,
        aspectRatioH: Int
    ) {
        val holder = findByPosition(adapterPosition)
        holder?.setProgressVisible(progress)
        holder?.setAspectRatio(aspectRatioW, aspectRatioH)
        holder?.setSurfaceVisible(if (progress) View.GONE else View.VISIBLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        helpDisposable.cancel()
        mLoadingProgressBarDispose.cancel()
        playDispose.cancel()
    }

    override fun onNext() {
        mViewPager?.let {
            it.adapter?.let { so ->
                if (so.itemCount > it.currentItem + 1) {
                    it.setCurrentItem(it.currentItem + 1, true)
                }
            }
        }
    }

    internal fun fireHolderCreate(holder: Holder) {
        presenter?.fireHolderCreate(holder.bindingAdapterPosition)
    }

    private fun findByPosition(position: Int): Holder? {
        val weak = mHolderSparseArray[position]
        return weak?.get()
    }

    inner class Holder(rootView: View) : RecyclerView.ViewHolder(rootView), SurfaceHolder.Callback {
        val mSurfaceView: ExpandableSurfaceView = rootView.findViewById(R.id.videoSurface)
        val mSurfaceHolder: SurfaceHolder = mSurfaceView.holder
        val mProgressBar: ThorVGLottieView
        var isSurfaceReady = false
        override fun surfaceCreated(holder: SurfaceHolder) {
            isSurfaceReady = true
            presenter?.fireSurfaceCreated(bindingAdapterPosition)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            isSurfaceReady = false
        }

        fun setProgressVisible(visible: Boolean) {
            mProgressBar.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                mProgressBar.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.loading,
                    intArrayOf(
                        0x000000,
                        CurrentTheme.getColorPrimary(this@ShortVideoPagerActivity),
                        0x777777,
                        CurrentTheme.getColorSecondary(this@ShortVideoPagerActivity)
                    )
                )
                mProgressBar.startAnimation()
            } else {
                mProgressBar.clearAnimationDrawable(
                    callSuper = true, clearState = true,
                    cancelTask = true
                )
            }
        }

        fun setAspectRatio(w: Int, h: Int) {
            mSurfaceView.setAspectRatio(w, h)
        }

        fun setSurfaceVisible(Vis: Int) {
            mSurfaceView.visibility = Vis
        }

        init {
            mSurfaceHolder.addCallback(this)
            mProgressBar = rootView.findViewById(R.id.preparing_progress_bar)
            mSurfaceView.setOnClickListener { toggleFullscreen() }
        }
    }

    private inner class Adapter(private var mPageCount: Int) : RecyclerView.Adapter<Holder>() {
        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(container: ViewGroup, viewType: Int): Holder {
            return Holder(
                LayoutInflater.from(container.context)
                    .inflate(R.layout.content_shortvideo_page, container, false)
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {

        }

        fun updateCount(count: Int) {
            mPageCount = count
        }

        override fun getItemCount(): Int {
            return mPageCount
        }

        override fun onViewDetachedFromWindow(holder: Holder) {
            super.onViewDetachedFromWindow(holder)
            mHolderSparseArray.remove(holder.bindingAdapterPosition)
        }

        override fun onViewAttachedToWindow(holder: Holder) {
            super.onViewAttachedToWindow(holder)
            mHolderSparseArray.put(holder.bindingAdapterPosition, WeakReference(holder))
            fireHolderCreate(holder)
        }

        init {
            mHolderSparseArray.clear()
        }
    }

    companion object {
        const val ACTION_OPEN =
            "dev.ragnarok.fenrir.activity.shortvideopager.ShortVideoPagerActivity"

        fun newInstance(context: Context, args: Bundle?): Intent {
            val ph = Intent(context, ShortVideoPagerActivity::class.java)
            val targetArgs = Bundle()
            targetArgs.putAll(args)
            ph.action = ACTION_OPEN
            ph.putExtras(targetArgs)
            return ph
        }

        fun buildArgs(aid: Long, ownerId: Long?): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            if (ownerId != null) {
                args.putBoolean(Extra.NO_OWNER_ID, false)
                args.putLong(Extra.OWNER_ID, ownerId)
            } else {
                args.putBoolean(Extra.NO_OWNER_ID, true)
            }
            return args
        }
    }
}
