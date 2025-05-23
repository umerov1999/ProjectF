package dev.ragnarok.fenrir.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.gifpager.GifPagerActivity
import dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity.Companion.newInstance
import dev.ragnarok.fenrir.activity.shortvideopager.ShortVideoPagerActivity
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.storypager.StoryPagerActivity
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.messages.chat.ChatFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.messages.notreadmessages.NotReadMessagesFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.hasVanillaIceCreamTarget
import dev.ragnarok.fenrir.util.ViewUtils

class NotReadMessagesActivity : NoMainActivity(), PlaceProvider, AppStyleable {
    //resolveToolbarNavigationIcon();
    private val mOnBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener { keyboardHide() }
    internal val frontFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(noMainContainerViewId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attach(this, SlidrConfig.Builder().listener(object : SlidrListener {
            override fun onSlideStateChanged(state: Int) {}
            override fun onSlideChange(percent: Float) {}
            override fun onSlideOpened() {}
            override fun onSlideClosed(): Boolean {
                val fragment = frontFragment
                if (fragment is NotReadMessagesFragment) {
                    fragment.fireFinish()
                } else {
                    return false
                }
                return true
            }
        }).scrimColor(CurrentTheme.getColorBackground(this)).build())
        if (savedInstanceState == null) {
            handleIntent(intent)
            supportFragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        val action = intent.action
        if (ACTION_OPEN_PLACE == action) {
            val place: Place? = intent.getParcelableExtraCompat(Extra.PLACE)
            if (place == null) {
                finish()
                return
            }
            openPlace(place)
        }
    }

    fun keyboardHide() {
        try {
            val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
            inputManager?.hideSoftInputFromWindow(
                window.decorView.rootView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        } catch (_: Exception) {
        }
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.CHAT -> {
                val peer: Peer = args.getParcelableCompat(Extra.PEER) ?: return
                val chatFragment =
                    newInstance(args.getLong(Extra.ACCOUNT_ID), args.getLong(Extra.OWNER_ID), peer)
                attachToFront(chatFragment)
            }

            Place.UNREAD_MESSAGES -> attachToFront(NotReadMessagesFragment.newInstance(args))
            Place.VK_PHOTO_ALBUM_GALLERY, Place.FAVE_PHOTOS_GALLERY, Place.SIMPLE_PHOTO_GALLERY, Place.SIMPLE_PHOTO_GALLERY_NATIVE, Place.VK_PHOTO_TMP_SOURCE, Place.VK_PHOTO_ALBUM_GALLERY_SAVED, Place.VK_PHOTO_ALBUM_GALLERY_NATIVE -> newInstance(
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

    private fun attachToFront(fragment: Fragment, animate: Boolean = true) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate) fragmentTransaction.setCustomAnimations(
            R.anim.fragment_enter,
            R.anim.fragment_exit
        )
        fragmentTransaction
            .replace(noMainContainerViewId, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    override fun onPause() {
        ViewUtils.keyboardHide(this)
        super.onPause()
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(mOnBackStackChangedListener)
        ViewUtils.keyboardHide(this)
        super.onDestroy()
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
        }
        val ins = WindowInsetsControllerCompat(w, w.decorView)
        ins.isAppearanceLightStatusBars = invertIcons
        ins.isAppearanceLightNavigationBars = invertIcons
    }

    companion object {
        const val ACTION_OPEN_PLACE =
            "dev.ragnarok.fenrir.activity.NotReadMessagesActivity.openPlace"
    }
}