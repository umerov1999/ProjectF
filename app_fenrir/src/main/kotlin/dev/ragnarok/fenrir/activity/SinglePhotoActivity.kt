package dev.ragnarok.fenrir.activity

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso3.Callback
import dev.ragnarok.fenrir.App
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.StubAnimatorListener
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.slidr.model.SlidrPosition
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.TouchImageView
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView
import dev.ragnarok.fenrir.view.pager.WeakPicassoLoadCallback
import java.io.File
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class SinglePhotoActivity : NoMainActivity(), PlaceProvider, AppStyleable {
    private var url: String? = null
    private var prefix: String? = null
    private var photo_prefix: String? = null
    private var mFullscreen = false
    private var mDownload: CircleCounterButton? = null

    @get:LayoutRes
    override val noMainContentView: Int
        get() = R.layout.activity_single_url_photo

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
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
                        mContentRoot?.setBackgroundColor(Color.argb(tmp, 0f, 0f, 0f))
                        mDownload?.alpha = tmp
                        ret.photo.alpha = Utils.clamp(percent, 0f, 1f)
                    }

                    override fun onSlideOpened() {
                    }

                    override fun onSlideClosed(): Boolean {
                        Utils.finishActivityImmediate(this@SinglePhotoActivity)
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

        ret.photo.setOnTouchListener { view, event ->
            if (event.pointerCount >= 2 || view is TouchImageView && view.isZoomed) {
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
            url = "full_$data"
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
        var dir = File(Settings.get().main().photoDir)
        if (!dir.isDirectory) {
            val created = dir.mkdirs()
            if (!created) {
                CustomToast.createCustomToast(this).showToastError("Can't create directory $dir")
                return
            }
        } else dir.setLastModified(Calendar.getInstance().timeInMillis)
        if (prefix != null && Settings.get().main().isPhoto_to_user_dir) {
            val dir_final = File(dir.absolutePath + "/" + prefix)
            if (!dir_final.isDirectory) {
                val created = dir_final.mkdirs()
                if (!created) {
                    CustomToast.createCustomToast(this)
                        .showToastError("Can't create directory $dir")
                    return
                }
            } else dir_final.setLastModified(Calendar.getInstance().timeInMillis)
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
        val progress: ThorVGLottieView
        var animationDispose = CancelableJob()
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
                            CurrentTheme.getColorPrimary(ref.get()),
                            0x777777,
                            CurrentTheme.getColorSecondary(ref.get())
                        )
                    )
                    progress.startAnimation()
                }
            }
        }

        private fun loadImage(url: String?) {
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
