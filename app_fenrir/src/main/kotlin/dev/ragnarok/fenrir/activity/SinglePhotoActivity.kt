package dev.ragnarok.fenrir.activity

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso3.Callback
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.TouchImageView
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import dev.ragnarok.fenrir.view.pager.WeakPicassoLoadCallback
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SinglePhotoActivity : NoMainActivity(), PlaceProvider, AppStyleable {
    private var url: String? = null
    private var prefix: String? = null
    private var photo_prefix: String? = null
    private var mFullscreen = false
    private var mDownload: CircleCounterButton? = null

    @LayoutRes
    override fun getNoMainContentView(): Int {
        return R.layout.fragment_single_url_photo
    }

    @SuppressLint("ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        mFullscreen = savedInstanceState?.getBoolean("mFullscreen") == true

        url?.let {
            mDownload?.visibility =
                if (it.contains("content://") || it.contains("file://")) View.GONE else View.VISIBLE
        }
        url ?: run {
            mDownload?.visibility = View.GONE
        }
        val ret = PhotoViewHolder(this)
        ret.bindTo(url)
        mDownload = findViewById(R.id.button_download)
        val mContentRoot = findViewById<RelativeLayout>(R.id.photo_single_root)
        attach(
            this,
            SlidrConfig.Builder().setAlphaForView(false).fromUnColoredToColoredStatusBar(true)
                .position(SlidrPosition.VERTICAL)
                .listener(object : SlidrListener {
                    override fun onSlideStateChanged(state: Int) {

                    }

                    @SuppressLint("Range")
                    override fun onSlideChange(percent: Float) {
                        var tmp = 1f - percent
                        tmp *= 4
                        tmp = Utils.clamp(1f - tmp, 0f, 1f)
                        if (Utils.hasOreo()) {
                            mContentRoot?.setBackgroundColor(Color.argb(tmp, 0f, 0f, 0f))
                        } else {
                            mContentRoot?.setBackgroundColor(
                                Color.argb(
                                    (tmp * 255).toInt(),
                                    0,
                                    0,
                                    0
                                )
                            )
                        }
                        mDownload?.alpha = tmp
                        ret.photo.alpha = Utils.clamp(percent, 0f, 1f)
                    }

                    override fun onSlideOpened() {
                    }

                    override fun onSlideClosed(): Boolean {
                        finish()
                        overridePendingTransition(0, 0)
                        return true
                    }

                }).build()
        )

        ret.photo.setOnLongClickListener {
            doSaveOnDrive(true)
            true
        }
        mDownload?.setOnClickListener { doSaveOnDrive(true) }
        resolveFullscreenViews()

        ret.photo.setOnTouchListener { view: View, event: MotionEvent ->
            if (event.pointerCount >= 2 || view.canScrollHorizontally(1) && view.canScrollHorizontally(
                    -1
                )
            ) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        mContentRoot?.requestDisallowInterceptTouchEvent(true)
                        return@setOnTouchListener false
                    }

                    MotionEvent.ACTION_UP -> {
                        mContentRoot?.requestDisallowInterceptTouchEvent(false)
                        return@setOnTouchListener true
                    }
                }
            }
            true
        }
    }

    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        doSaveOnDrive(false)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        if (Intent.ACTION_VIEW == intent.action) {
            val data = intent.data
            url = "full_" + data.toString()
            prefix = "tmp"
            photo_prefix = "tmp"
        } else {
            url = intent.extras?.getString(Extra.URL)
            prefix = DownloadWorkUtils.makeLegalFilenameFromArg(
                intent.extras?.getString(Extra.STATUS),
                null
            )
            photo_prefix = DownloadWorkUtils.makeLegalFilenameFromArg(
                intent.extras?.getString(Extra.KEY),
                null
            )
        }
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

    private fun doSaveOnDrive(Request: Boolean) {
        if (Request) {
            if (!AppPerms.hasReadWriteStoragePermission(App.instance)) {
                requestWritePermission.launch()
            }
        }
        var dir = File(Settings.get().other().photoDir)
        if (!dir.isDirectory) {
            val created = dir.mkdirs()
            if (!created) {
                CustomToast.createCustomToast(this).showToastError("Can't create directory $dir")
                return
            }
        } else dir.setLastModified(Calendar.getInstance().time.time)
        if (prefix != null && Settings.get().other().isPhoto_to_user_dir) {
            val dir_final = File(dir.absolutePath + "/" + prefix)
            if (!dir_final.isDirectory) {
                val created = dir_final.mkdirs()
                if (!created) {
                    CustomToast.createCustomToast(this)
                        .showToastError("Can't create directory $dir")
                    return
                }
            } else dir_final.setLastModified(Calendar.getInstance().time.time)
            dir = dir_final
        }
        val DOWNLOAD_DATE_FORMAT: DateFormat =
            SimpleDateFormat("yyyyMMdd_HHmmss", Utils.appLocale)
        url?.let {
            DownloadWorkUtils.doDownloadPhoto(
                this,
                it,
                dir.absolutePath,
                Utils.firstNonEmptyString(prefix, "null") + "." + Utils.firstNonEmptyString(
                    photo_prefix,
                    "null"
                ) + ".profile." + DOWNLOAD_DATE_FORMAT.format(Date())
            )
        }
    }

    private inner class PhotoViewHolder(view: SinglePhotoActivity) : Callback {
        private val ref = WeakReference(view)
        val reload: FloatingActionButton
        private val mPicassoLoadCallback: WeakPicassoLoadCallback
        val photo: TouchImageView
        val progress: RLottieImageView
        var animationDispose: Disposable = Disposable.disposed()
        private var mAnimationLoaded = false
        private var mLoadingNow = false
        fun bindTo(url: String?) {
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
                CustomToast.createCustomToast(ref.get()).showToast(R.string.empty_url)
            }
        }

        private fun resolveProgressVisibility(forceStop: Boolean) {
            animationDispose.dispose()
            if (mAnimationLoaded && !mLoadingNow && !forceStop) {
                mAnimationLoaded = false
                val k = ObjectAnimator.ofFloat(progress, View.ALPHA, 0.0f).setDuration(1000)
                k.addListener(object : StubAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        progress.clearAnimationDrawable()
                        progress.visibility = View.GONE
                        progress.alpha = 1f
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        progress.clearAnimationDrawable()
                        progress.visibility = View.GONE
                        progress.alpha = 1f
                    }
                })
                k.start()
            } else if (mAnimationLoaded && !mLoadingNow) {
                mAnimationLoaded = false
                progress.clearAnimationDrawable()
                progress.visibility = View.GONE
            } else if (mLoadingNow) {
                animationDispose = Completable.create {
                    it.onComplete()
                }.delay(300, TimeUnit.MILLISECONDS).fromIOToMain().subscribe({
                    mAnimationLoaded = true
                    progress.visibility = View.VISIBLE
                    progress.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.loading,
                        Utils.dp(100F),
                        Utils.dp(100F),
                        intArrayOf(
                            0x000000,
                            CurrentTheme.getColorPrimary(ref.get()),
                            0x777777,
                            CurrentTheme.getColorSecondary(ref.get())
                        )
                    )
                    progress.playAnimation()
                }, RxUtils.ignore())
            }
        }

        private fun loadImage(url: String?) {
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

    internal fun toggleFullscreen() {
        mFullscreen = !mFullscreen
        resolveFullscreenViews()
    }

    private fun resolveFullscreenViews() {
        mDownload?.visibility = if (mFullscreen) View.GONE else View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("mFullscreen", mFullscreen)
    }

    override fun hideMenu(hide: Boolean) {}
    override fun openMenu(open: Boolean) {}

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val statusbarNonColored = CurrentTheme.getStatusBarNonColored(this)
        val statusbarColored = CurrentTheme.getStatusBarColor(this)
        val w = window
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.statusBarColor = if (colored) statusbarColored else statusbarNonColored
        @ColorInt val navigationColor =
            if (colored) CurrentTheme.getNavigationBarColor(this) else Color.BLACK
        w.navigationBarColor = navigationColor
        if (Utils.hasMarshmallow()) {
            var flags = window.decorView.systemUiVisibility
            flags = if (invertIcons) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
        if (Utils.hasOreo()) {
            var flags = window.decorView.systemUiVisibility
            if (invertIcons) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                w.decorView.systemUiVisibility = flags
                w.navigationBarColor = Color.WHITE
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                w.decorView.systemUiVisibility = flags
            }
        }
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

    companion object {
        private const val ACTION_OPEN =
            "dev.ragnarok.fenrir.activity.SinglePhotoActivity"

        fun newInstance(context: Context, args: Bundle?): Intent {
            val ph = Intent(context, SinglePhotoActivity::class.java)
            val targetArgs = Bundle()
            targetArgs.putAll(args)
            ph.action = ACTION_OPEN
            ph.putExtras(targetArgs)
            return ph
        }

        fun buildArgs(url: String?, download_prefix: String?, photo_prefix: String?): Bundle {
            val args = Bundle()
            args.putString(Extra.URL, url)
            args.putString(Extra.STATUS, download_prefix)
            args.putString(Extra.KEY, photo_prefix)
            return args
        }
    }
}
