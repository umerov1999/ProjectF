package dev.ragnarok.fenrir.activity.storypager

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.util.size
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso3.Callback
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.StubAnimatorListener
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.BaseMvpActivity
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity
import dev.ragnarok.fenrir.activity.slidr.Slidr
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.media.story.IStoryPlayer
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
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
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.ExpandableSurfaceView
import dev.ragnarok.fenrir.view.TouchImageView
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView
import dev.ragnarok.fenrir.view.pager.WeakPicassoLoadCallback
import java.lang.ref.WeakReference
import java.util.Calendar

class StoryPagerActivity : BaseMvpActivity<StoryPagerPresenter, IStoryPagerView>(),
    IStoryPagerView, PlaceProvider, AppStyleable {
    private val mHolderSparseArray = SparseArray<WeakReference<MultiHolder>>()
    private var mViewPager: ViewPager2? = null
    private var mToolbar: Toolbar? = null
    private var mAvatar: ImageView? = null
    private var mExpires: TextView? = null
    private var mDuration: TextView? = null
    private var transformation: Transformation? = null
    private var mDownload: CircleCounterButton? = null
    private var mShare: CircleCounterButton? = null
    private var mLink: CircleCounterButton? = null
    private var mFullscreen = false
    private var hasExternalUrl = false
    private var mPlaySpeed: ImageView? = null
    private var helpDisposable = CancelableJob()
    private var playDispose = CancelableJob()

    @get:LayoutRes
    override val noMainContentView: Int
        get() = R.layout.activity_story_pager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFullscreen = savedInstanceState?.getBoolean("mFullscreen") == true
        transformation = CurrentTheme.createTransformationForAvatar()
        val mContentRoot = findViewById<RelativeLayout>(R.id.story_pager_root)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        mAvatar = findViewById(R.id.toolbar_avatar)
        mViewPager = findViewById(R.id.view_pager)
        mViewPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        mExpires = findViewById(R.id.item_story_expires)
        mDuration = findViewById(R.id.item_story_duration)
        mPlaySpeed = findViewById(R.id.toolbar_play_speed)
        mPlaySpeed?.setOnClickListener {
            val stateSpeed = presenter?.togglePlaybackSpeed() == true
            Utils.setTint(
                mPlaySpeed,
                if (stateSpeed) CurrentTheme.getColorPrimary(this) else "#ffffff".toColor()
            )
        }
        val mHelper = findViewById<ThorVGLottieView?>(R.id.swipe_helper)
        if (HelperSimple.needHelp(HelperSimple.STORY_HELPER, 2)) {
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
        mLink = findViewById(R.id.button_link)
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
                        Utils.finishActivityImmediate(this@StoryPagerActivity)
                        return true
                    }

                }).build()
        )
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

    override fun downloadPhoto(url: String, dir: String, file: String) {
        DownloadWorkUtils.doDownloadPhoto(this, url, dir, file)
    }

    override fun downloadVideo(video: Video, url: String, Res: String) {
        DownloadWorkUtils.doDownloadVideo(this, video, url, Res)
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
        mLink?.visibility = if (mFullscreen || !hasExternalUrl) View.GONE else View.VISIBLE
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): StoryPagerPresenter {
        val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
        val index = requireArguments().getInt(Extra.INDEX)
        if (FenrirNative.isNativeLoaded && Settings.get()
                .main().isNative_parcel_story
        ) {
            var pointer = requireArguments().getLong(Extra.STORY)
            requireArguments().putLong(Extra.STORY, 0)
            if (!Utils.isParcelNativeRegistered(pointer)) {
                pointer = 0
            }
            Utils.unregisterParcelNative(pointer)
            return StoryPagerPresenter(
                aid,
                ParcelNative.loadParcelableArrayList(
                    pointer,
                    Story.NativeCreator,
                    ParcelFlags.EMPTY_LIST
                )!!,
                index,
                saveInstanceState
            )
        } else {
            return StoryPagerPresenter(
                aid,
                requireArguments().getParcelableArrayListCompat(Extra.STORY)!!,
                index,
                saveInstanceState
            )
        }
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
        val adapter = Adapter(pageCount)
        mViewPager?.adapter = adapter
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

    override fun setToolbarSubtitle(story: Story, account_id: Long, isPlaySpeed: Boolean) {
        supportActionBar?.subtitle = story.owner?.fullName
        mAvatar?.setOnClickListener {
            story.owner?.let { it1 ->
                PlaceFactory.getOwnerWallPlace(account_id, it1)
                    .tryOpenWith(this)
            }
        }
        mAvatar?.let {
            ViewUtils.displayAvatar(
                it,
                transformation,
                story.owner?.maxSquareAvatar,
                Constants.PICASSO_TAG
            )
        }
        if (story.expires <= 0) mExpires?.visibility = View.GONE else {
            mExpires?.visibility = View.VISIBLE
            val exp = (story.expires - Calendar.getInstance().timeInMillis / 1000) / 3600
            mExpires?.text = getString(
                R.string.expires,
                exp.toString(),
                getString(
                    Utils.declOfNum(
                        exp,
                        intArrayOf(R.string.hour, R.string.hour_sec, R.string.hours)
                    )
                )
            )
        }
        if (story.isStoryIsVideo()) {
            mDuration?.visibility = View.VISIBLE
            mDuration?.text = story.video?.duration?.let { AppTextUtils.getDurationString(it) }
            mPlaySpeed?.visibility = View.VISIBLE
        } else {
            mDuration?.visibility = View.GONE
            mPlaySpeed?.visibility = View.GONE
        }
        Utils.setTint(
            mPlaySpeed,
            if (isPlaySpeed) CurrentTheme.getColorPrimary(this) else "#ffffff".toColor()
        )
        if (story.target_url.isNullOrEmpty()) {
            mLink?.visibility = View.GONE
            hasExternalUrl = false
        } else {
            hasExternalUrl = true
            mLink?.visibility = View.VISIBLE
            mLink?.setOnClickListener {
                LinkHelper.openUrl(
                    this,
                    account_id,
                    story.target_url, false
                )
            }
        }
    }

    override fun onShare(story: Story, account_id: Long) {
        SendAttachmentsActivity.startForSendAttachments(this, account_id, story)
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

    internal fun fireHolderCreate(holder: MultiHolder) {
        presenter?.fireHolderCreate(holder.bindingAdapterPosition)
    }

    private fun findByPosition(position: Int): MultiHolder? {
        val weak = mHolderSparseArray[position]
        return weak?.get()
    }

    open class MultiHolder internal constructor(rootView: View) :
        RecyclerView.ViewHolder(rootView) {
        lateinit var mSurfaceHolder: SurfaceHolder
        open val isSurfaceReady: Boolean
            get() = false

        open fun setProgressVisible(visible: Boolean) {}
        open fun setAspectRatio(w: Int, h: Int) {}
        open fun setSurfaceVisible(Vis: Int) {}
        open fun bindTo(story: Story) {}
    }

    private inner class Holder(rootView: View) : MultiHolder(rootView), SurfaceHolder.Callback {
        val mSurfaceView: ExpandableSurfaceView = rootView.findViewById(R.id.videoSurface)
        val mProgressBar: ThorVGLottieView
        override var isSurfaceReady = false
        override fun surfaceCreated(holder: SurfaceHolder) {
            isSurfaceReady = true
            presenter?.fireSurfaceCreated(bindingAdapterPosition)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            isSurfaceReady = false
        }

        override fun setProgressVisible(visible: Boolean) {
            mProgressBar.visibility = if (visible) View.VISIBLE else View.GONE
            if (visible) {
                mProgressBar.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.loading,
                    intArrayOf(
                        0x000000,
                        CurrentTheme.getColorPrimary(this@StoryPagerActivity),
                        0x777777,
                        CurrentTheme.getColorSecondary(this@StoryPagerActivity)
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

        override fun setAspectRatio(w: Int, h: Int) {
            mSurfaceView.setAspectRatio(w, h)
        }

        override fun setSurfaceVisible(Vis: Int) {
            mSurfaceView.visibility = Vis
        }

        init {
            mSurfaceHolder = mSurfaceView.holder
            mSurfaceHolder.addCallback(this)
            mProgressBar = rootView.findViewById(R.id.preparing_progress_bar)
            mSurfaceView.setOnClickListener { toggleFullscreen() }
        }
    }

    private inner class PhotoViewHolder(view: View) : MultiHolder(view), Callback {
        val reload: FloatingActionButton
        private val mPicassoLoadCallback: WeakPicassoLoadCallback
        val photo: TouchImageView
        val progress: ThorVGLottieView
        var animationDispose = CancelableJob()
        private var mAnimationLoaded = false
        private var mLoadingNow = false
        override fun bindTo(story: Story) {
            photo.resetZoom()
            photo.orientationLocked = TouchImageView.OrientationLocked.VERTICAL
            if (story.isIs_expired) {
                createCustomToast(this@StoryPagerActivity).showToastError(R.string.is_expired)
                mLoadingNow = false
                resolveProgressVisibility(true)
                return
            }
            if (story.photo == null) return
            val url = story.photo?.getUrlForSize(PhotoSize.W, true)
            reload.setOnClickListener {
                reload.visibility = View.INVISIBLE
                if (url.nonNullNoEmpty()) {
                    loadImage(url)
                } else PicassoInstance.with().cancelRequest(photo)
            }
            if (url.nonNullNoEmpty()) {
                loadImage(url)
            } else {
                PicassoInstance.with().cancelRequest(photo)
                createCustomToast(this@StoryPagerActivity).showToast(R.string.empty_url)
            }
        }

        private fun resolveProgressVisibility(forceStop: Boolean) {
            animationDispose.cancel()
            if (mAnimationLoaded && !mLoadingNow && !forceStop) {
                mAnimationLoaded = false
                val k = ObjectAnimator.ofFloat(progress, View.ALPHA, 0.0f).setDuration(1000)
                k.addListener(object : StubAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        progress.clearAnimationDrawable(
                            callSuper = true, clearState = true,
                            cancelTask = true
                        )
                        progress.visibility = View.GONE
                        progress.alpha = 1f
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        progress.clearAnimationDrawable(
                            callSuper = true, clearState = true,
                            cancelTask = true
                        )
                        progress.visibility = View.GONE
                        progress.alpha = 1f
                    }
                })
                k.start()
            } else if (mAnimationLoaded && !mLoadingNow) {
                mAnimationLoaded = false
                progress.clearAnimationDrawable(
                    callSuper = true, clearState = true,
                    cancelTask = true
                )
                progress.visibility = View.GONE
            } else if (mLoadingNow) {
                animationDispose += delayTaskFlow(300).toMain {
                    mAnimationLoaded = true
                    progress.visibility = View.VISIBLE
                    progress.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.loading,
                        intArrayOf(
                            0x000000,
                            CurrentTheme.getColorPrimary(this@StoryPagerActivity),
                            0x777777,
                            CurrentTheme.getColorSecondary(this@StoryPagerActivity)
                        )
                    )
                    progress.startAnimation()
                }
            }
        }

        private fun loadImage(url: String) {
            PicassoInstance.with().cancelRequest(photo)
            mLoadingNow = true
            resolveProgressVisibility(true)
            PicassoInstance.with()
                .load(url)
                .into(photo, mPicassoLoadCallback)
        }

        @IdRes
        private fun idOfImageView(): Int {
            return R.id.image_view
        }

        @IdRes
        private fun idOfProgressBar(): Int {
            return R.id.progress_bar
        }

        override fun onSuccess() {
            mLoadingNow = false
            resolveProgressVisibility(false)
            reload.visibility = View.INVISIBLE
        }

        override fun onError(t: Throwable) {
            mLoadingNow = false
            resolveProgressVisibility(true)
            reload.visibility = View.VISIBLE
        }

        init {
            photo = view.findViewById(idOfImageView())
            photo.maxZoom = 8f
            photo.doubleTapScale = 2f
            photo.doubleTapMaxZoom = 4f
            progress = view.findViewById(idOfProgressBar())
            reload = view.findViewById(R.id.goto_button)
            mPicassoLoadCallback = WeakPicassoLoadCallback(this)
            photo.setOnClickListener { toggleFullscreen() }
        }
    }

    private inner class Adapter(val mPageCount: Int) : RecyclerView.Adapter<MultiHolder>() {
        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(container: ViewGroup, viewType: Int): MultiHolder {
            if (viewType == 0) return Holder(
                LayoutInflater.from(container.context)
                    .inflate(R.layout.content_story_page, container, false)
            )
            val ret = PhotoViewHolder(
                LayoutInflater.from(container.context)
                    .inflate(R.layout.content_photo_page, container, false)
            )
            ret.photo.setOnTouchListener { view, event ->
                if (event.pointerCount >= 2 || view is TouchImageView && view.isZoomed) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            container.requestDisallowInterceptTouchEvent(true)
                            return@setOnTouchListener false
                        }

                        MotionEvent.ACTION_UP -> {
                            container.requestDisallowInterceptTouchEvent(false)
                            return@setOnTouchListener true
                        }
                    }
                }
                true
            }
            return ret
        }

        override fun onBindViewHolder(holder: MultiHolder, position: Int) {
            presenter?.let {
                if (!it.isStoryIsVideo(position)) holder.bindTo(it.getStory(position))
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (presenter?.isStoryIsVideo(position) == true) 0 else 1
        }

        override fun getItemCount(): Int {
            return mPageCount
        }

        override fun onViewDetachedFromWindow(holder: MultiHolder) {
            super.onViewDetachedFromWindow(holder)
            mHolderSparseArray.remove(holder.bindingAdapterPosition)
        }

        override fun onViewAttachedToWindow(holder: MultiHolder) {
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
            "dev.ragnarok.fenrir.activity.storypager.StoryPagerActivity"

        fun newInstance(context: Context, args: Bundle?): Intent {
            val ph = Intent(context, StoryPagerActivity::class.java)
            val targetArgs = Bundle()
            targetArgs.putAll(args)
            ph.action = ACTION_OPEN
            ph.putExtras(targetArgs)
            return ph
        }

        fun buildArgs(aid: Long, stories: ArrayList<Story>, index: Int): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putInt(Extra.INDEX, index)
            if (FenrirNative.isNativeLoaded && Settings.get().main().isNative_parcel_story) {
                val pointer = ParcelNative.createParcelableList(stories, ParcelFlags.NULL_LIST)
                Utils.registerParcelNative(pointer)
                args.putLong(
                    Extra.STORY,
                    pointer
                )
            } else {
                args.putParcelableArrayList(Extra.STORY, stories)
            }
            return args
        }
    }
}
