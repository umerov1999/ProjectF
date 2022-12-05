package dev.ragnarok.filegallery.activity

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Rational
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.customview.widget.ViewDragHelper
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.slidr.Slidr
import dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig
import dev.ragnarok.filegallery.activity.slidr.model.SlidrListener
import dev.ragnarok.filegallery.activity.slidr.model.SlidrPosition
import dev.ragnarok.filegallery.getParcelableCompat
import dev.ragnarok.filegallery.getParcelableExtraCompat
import dev.ragnarok.filegallery.listener.AppStyleable
import dev.ragnarok.filegallery.media.video.ExoVideoPlayer
import dev.ragnarok.filegallery.media.video.IVideoPlayer
import dev.ragnarok.filegallery.media.video.IVideoPlayer.IVideoSizeChangeListener
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.filegallery.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.filegallery.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.filegallery.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.settings.theme.ThemesController.currentStyle
import dev.ragnarok.filegallery.util.Logger
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.toast.CustomToast
import dev.ragnarok.filegallery.view.ExpandableSurfaceView
import dev.ragnarok.filegallery.view.VideoControllerView

class VideoPlayerActivity : AppCompatActivity(), SurfaceHolder.Callback,
    VideoControllerView.MediaPlayerControl, IVideoSizeChangeListener, AppStyleable {
    private var mDecorView: View? = null
    private var mSpeed: ImageView? = null
    private var mControllerView: VideoControllerView? = null
    private var mSurfaceView: ExpandableSurfaceView? = null
    private var mPlayer: IVideoPlayer? = null
    private var video: Video? = null
    private var onStopCalled = false
    private var seekSave: Long = -1
    private var isLandscape = false
    override fun onStop() {
        onStopCalled = true
        super.onStop()
    }

    override fun finish() {
        finishAndRemoveTask()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d(VideoPlayerActivity::class.java.name, "onNewIntent, intent: $intent")
        handleIntent(intent, true)
    }

    private fun handleIntent(intent: Intent?, update: Boolean) {
        if (intent == null) {
            return
        }
        video = intent.getParcelableExtraCompat(EXTRA_VIDEO)
        val actionBar = supportActionBar
        actionBar?.title = video?.title
        actionBar?.subtitle = video?.description
        if (update) {
            val url = video?.link
            mPlayer?.updateSource(this, url)
            mPlayer?.play()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("video", video)
        outState.putLong("seek", mPlayer?.currentPosition ?: -1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(currentStyle())
        Utils.prepareDensity(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mDecorView = window.decorView
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.arrow_left)
            toolbar.setNavigationOnClickListener { finish() }
        }
        if (savedInstanceState == null) {
            handleIntent(intent, false)
        } else {
            video = savedInstanceState.getParcelableCompat("video")
            seekSave = savedInstanceState.getLong("seek")
            val actionBar = supportActionBar
            actionBar?.title = video?.title
            actionBar?.subtitle = video?.description
        }
        mControllerView = VideoControllerView(this)
        val surfaceContainer = findViewById<ViewGroup>(R.id.videoSurfaceContainer)
        mSurfaceView = findViewById(R.id.videoSurface)
        if (get().main().isVideo_swipes()) {
            Slidr.attach(
                this,
                SlidrConfig.Builder().setAlphaForView(false).fromUnColoredToColoredStatusBar(true)
                    .position(SlidrPosition.LEFT)
                    .listener(object : SlidrListener {
                        override fun onSlideStateChanged(state: Int) {
                            if (state != ViewDragHelper.STATE_IDLE) {
                                if (supportActionBar?.isShowing == true) {
                                    resolveControlsVisibility()
                                }
                            }
                        }

                        @SuppressLint("Range")
                        override fun onSlideChange(percent: Float) {
                            var tmp = 1f - percent
                            tmp *= 4
                            tmp = Utils.clamp(1f - tmp, 0f, 1f)
                            if (Utils.hasOreo()) {
                                surfaceContainer?.setBackgroundColor(Color.argb(tmp, 0f, 0f, 0f))
                            } else {
                                surfaceContainer?.setBackgroundColor(
                                    Color.argb(
                                        (tmp * 255).toInt(),
                                        0,
                                        0,
                                        0
                                    )
                                )
                            }
                            mControllerView?.alpha = tmp
                            toolbar?.alpha = tmp
                            mSurfaceView?.alpha = Utils.clamp(percent, 0f, 1f)
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
        }
        surfaceContainer.setOnClickListener { resolveControlsVisibility() }
        val videoHolder = mSurfaceView?.holder
        videoHolder?.addCallback(this)
        resolveControlsVisibility()
        mPlayer = createPlayer()
        mPlayer?.addVideoSizeChangeListener(this)
        mPlayer?.play()
        if (seekSave > 0) {
            mPlayer?.seekTo(seekSave)
        }
        mSpeed = findViewById(R.id.toolbar_play_speed)
        Utils.setTint(
            mSpeed,
            if (mPlayer?.isPlaybackSpeed == true) getColorPrimary(this) else Color.parseColor("#ffffff")
        )
        mSpeed?.setOnClickListener {
            mPlayer?.togglePlaybackSpeed()
            Utils.setTint(
                mSpeed,
                if (mPlayer?.isPlaybackSpeed == true) getColorPrimary(this) else Color.parseColor("#ffffff")
            )
        }
        mControllerView?.setMediaPlayer(this)
        if (get().main().isVideo_controller_to_decor()) {
            mControllerView?.setAnchorView(mDecorView as ViewGroup?, true)
        } else {
            mControllerView?.setAnchorView(findViewById(R.id.panel), false)
        }
        mControllerView?.updatePip(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(
                PackageManager.FEATURE_PICTURE_IN_PICTURE
            ) && hasPipPermission()
        )
    }

    private fun createPlayer(): IVideoPlayer {
        val url = video?.link
        return ExoVideoPlayer(this, url, object : IVideoPlayer.IUpdatePlayListener {
            override fun onPlayChanged(isPause: Boolean) {
                mControllerView?.updatePausePlay()
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun resolveControlsVisibility() {
        val actionBar = supportActionBar ?: return
        if (actionBar.isShowing) {
            actionBar.hide()
            mControllerView?.hide()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mDecorView?.layoutParams =
                WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
            mDecorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE)
        } else {
            actionBar.show()
            mControllerView?.show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mDecorView?.layoutParams =
                WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT)
            mDecorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onDestroy() {
        mPlayer?.release()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setBarsColored(colored = false, invertIcons = false)
            .build()
            .apply(this)
        onStopCalled = false
        val actionBar = supportActionBar
        if (actionBar != null && actionBar.isShowing) {
            actionBar.hide()
            mControllerView?.hide()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mDecorView?.layoutParams =
            WindowManager.LayoutParams(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
        mDecorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val actionBar = supportActionBar ?: return
        if (isInPictureInPictureMode) {
            actionBar.hide()
            mControllerView?.hide()
        } else {
            if (onStopCalled) {
                finish()
            } else {
                actionBar.show()
                mControllerView?.show()
            }
        }
    }

    private fun canVideoPause(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            !isInPictureInPictureMode
        } else true
    }

    override fun onPause() {
        if (canVideoPause()) {
            mPlayer?.pause()
            mControllerView?.updatePausePlay()
        }
        super.onPause()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceCreated(holder: SurfaceHolder) {
        mPlayer?.setSurfaceHolder(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}
    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override val bufferPercentage: Int
        get() = mPlayer?.bufferPercentage ?: 0
    override val currentPosition: Long
        get() = mPlayer?.currentPosition ?: 0
    override val bufferPosition: Long
        get() = mPlayer?.bufferPosition ?: 0
    override val duration: Long
        get() = mPlayer?.duration ?: 0
    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying ?: false

    override fun pause() {
        mPlayer?.pause()
    }

    override fun seekTo(pos: Long) {
        mPlayer?.seekTo(pos)
    }

    override fun start() {
        mPlayer?.play()
    }

    override val isFullScreen: Boolean
        get() = false

    override fun toggleFullScreen() {
        try {
            requestedOrientation =
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } catch (e: Exception) {
            CustomToast.createCustomToast(this, mSurfaceView)?.setDuration(Toast.LENGTH_LONG)
                ?.showToastError(R.string.not_supported)
        }
    }

    @Suppress("DEPRECATION")
    private fun hasPipPermission(): Boolean {
        val appsOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager?
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                appsOps?.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                appsOps?.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            }
            else -> {
                false
            }
        }
    }

    override fun toPIPScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && hasPipPermission()
            ) if (!isInPictureInPictureMode) {
                mSurfaceView?.let {
                    val aspectRatio = Rational(it.width, it.height)
                    enterPictureInPictureMode(
                        PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
                    )
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false
        }
    }

    override fun onVideoSizeChanged(player: IVideoPlayer, w: Int, h: Int) {
        mSurfaceView?.setAspectRatio(w, h)
    }

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val statusbarNonColored = getStatusBarNonColored(this)
        val statusbarColored = getStatusBarColor(this)
        val w = window
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.statusBarColor = if (colored) statusbarColored else statusbarNonColored
        @ColorInt val navigationColor = if (colored) getNavigationBarColor(this) else Color.BLACK
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

    companion object {
        const val EXTRA_VIDEO = "video"
    }
}
