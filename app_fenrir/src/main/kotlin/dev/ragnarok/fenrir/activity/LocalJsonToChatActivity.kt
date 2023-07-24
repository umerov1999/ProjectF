package dev.ragnarok.fenrir.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.gifpager.GifPagerActivity
import dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity.Companion.newInstance
import dev.ragnarok.fenrir.activity.shortvideopager.ShortVideoPagerActivity
import dev.ragnarok.fenrir.activity.storypager.StoryPagerActivity
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.messages.localjsontochat.LocalJsonToChatFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.MusicPlaybackService
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils

class LocalJsonToChatActivity : NoMainActivity(), PlaceProvider, AppStyleable, ServiceConnection {
    private val mOnBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener { keyboardHide() }

    private var mAudioPlayServiceToken: MusicPlaybackController.ServiceToken? = null

    private fun bindToAudioPlayService() {
        if (mAudioPlayServiceToken == null) {
            mAudioPlayServiceToken = MusicPlaybackController.bindToServiceWithoutStart(this, this)
        }
    }

    private fun unbindFromAudioPlayService() {
        if (mAudioPlayServiceToken != null) {
            if (isChangingConfigurations) {
                MusicPlaybackController.doNotDestroyWhenActivityRecreated()
            }
            MusicPlaybackController.unbindFromService(mAudioPlayServiceToken)
            mAudioPlayServiceToken = null
        }
    }

    private val TAG = "LocalJsonToChatActivity_LOG"

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        if (name.className == MusicPlaybackService::class.java.name) {
            Logger.d(TAG, "Connected to MusicPlaybackService")
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        if (mAudioPlayServiceToken == null) return
        if (name.className == MusicPlaybackService::class.java.name) {
            Logger.d(TAG, "Disconnected from MusicPlaybackService")
            mAudioPlayServiceToken = null
            bindToAudioPlayService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
            supportFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener)
        }

        bindToAudioPlayService()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        val accountId = Settings.get().accounts().current
        if (accountId == ISettings.IAccountsSettings.INVALID_ID) {
            finish()
            return
        }
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            attachInitialFragment(LocalJsonToChatFragment.newInstance(accountId))
        }
    }

    fun keyboardHide() {
        try {
            val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            inputManager?.hideSoftInputFromWindow(
                window.decorView.rootView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        } catch (ignored: Exception) {
        }
    }

    private fun attachInitialFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
            .replace(getMainContainerViewId(), fragment)
            .addToBackStack("primary_local_chat")
            .commitAllowingStateLoss()
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.VK_PHOTO_ALBUM_GALLERY, Place.FAVE_PHOTOS_GALLERY, Place.SIMPLE_PHOTO_GALLERY, Place.VK_PHOTO_TMP_SOURCE, Place.VK_PHOTO_ALBUM_GALLERY_SAVED, Place.VK_PHOTO_ALBUM_GALLERY_NATIVE -> newInstance(
                this,
                place.type,
                args
            )?.let {
                place.launchActivityForResult(
                    this,
                    it
                )
            }

            Place.STORY_PLAYER -> place.launchActivityForResult(
                this,
                StoryPagerActivity.newInstance(this, args)
            )

            Place.SHORT_VIDEOS -> place.launchActivityForResult(
                this,
                ShortVideoPagerActivity.newInstance(this, args)
            )

            Place.SINGLE_PHOTO -> place.launchActivityForResult(
                this,
                SinglePhotoActivity.newInstance(this, args)
            )

            Place.GIF_PAGER -> place.launchActivityForResult(
                this,
                GifPagerActivity.newInstance(this, args)
            )

            Place.DOC_PREVIEW -> {
                val document: Document? = args.getParcelableCompat(Extra.DOC)
                if (document != null && document.hasValidGifVideoLink()) {
                    val aid = args.getLong(Extra.ACCOUNT_ID)
                    val documents = ArrayList(listOf(document))
                    val extra = GifPagerActivity.buildArgs(aid, documents, 0)
                    place.launchActivityForResult(this, GifPagerActivity.newInstance(this, extra))
                } else {
                    Utils.openPlaceWithSwipebleActivity(this, place)
                }
            }

            Place.PLAYER -> {
                val player = supportFragmentManager.findFragmentByTag("audio_player")
                if (player is AudioPlayerFragment) player.dismiss()
                newInstance(args).show(supportFragmentManager, "audio_player")
            }

            else -> Utils.openPlaceWithSwipebleActivity(this, place)
        }
    }

    public override fun onPause() {
        ViewUtils.keyboardHide(this)
        super.onPause()
    }

    public override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(mOnBackStackChangedListener)
        ViewUtils.keyboardHide(this)

        unbindFromAudioPlayService()
        super.onDestroy()
    }

    override fun hideMenu(hide: Boolean) {}
    override fun openMenu(open: Boolean) {}

    @Suppress("DEPRECATION")
    override fun setStatusbarColored(colored: Boolean, invertIcons: Boolean) {
        val statusbarNonColored = CurrentTheme.getStatusBarNonColored(this)
        val statusbarColored = CurrentTheme.getStatusBarColor(this)
        val w = window
        w.statusBarColor = if (colored) statusbarColored else statusbarNonColored
        @ColorInt val navigationColor =
            if (colored) CurrentTheme.getNavigationBarColor(this) else Color.BLACK
        w.navigationBarColor = navigationColor
        val ins = WindowInsetsControllerCompat(w, w.decorView)
        ins.isAppearanceLightStatusBars = invertIcons
        ins.isAppearanceLightNavigationBars = invertIcons

        if (!Utils.hasMarshmallow()) {
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }
}