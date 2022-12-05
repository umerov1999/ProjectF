package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.gifpager.GifPagerActivity
import dev.ragnarok.fenrir.activity.photopager.PhotoPagerActivity.Companion.newInstance
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.activity.slidr.model.SlidrListener
import dev.ragnarok.fenrir.activity.storypager.StoryPagerActivity
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment
import dev.ragnarok.fenrir.fragment.audio.AudioPlayerFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.messages.chat.ChatFragment.Companion.newInstance
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils

class ChatActivityBubbles : NoMainActivity(), PlaceProvider, AppStyleable {
    //resolveToolbarNavigationIcon();
    private val mOnBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener { keyboardHide() }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attach(this, SlidrConfig.Builder().listener(object : SlidrListener {
            override fun onSlideStateChanged(state: Int) {}
            override fun onSlideChange(percent: Float) {}
            override fun onSlideOpened() {}
            override fun onSlideClosed(): Boolean {
                NotificationHelper.resetBubbleOpened(this@ChatActivityBubbles, false)
                finish()
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
        } catch (ignored: Exception) {
        }
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.CHAT -> {
                val peer: Peer = args.getParcelableCompat(Extra.PEER) ?: return
                val chatFragment =
                    newInstance(args.getInt(Extra.ACCOUNT_ID), args.getInt(Extra.OWNER_ID), peer)
                attachToFront(chatFragment)
            }
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
            Place.SINGLE_PHOTO -> place.launchActivityForResult(
                this,
                SinglePhotoActivity.newInstance(this, args)
            )
            Place.STORY_PLAYER -> place.launchActivityForResult(
                this,
                StoryPagerActivity.newInstance(this, args)
            )
            Place.GIF_PAGER -> place.launchActivityForResult(
                this,
                GifPagerActivity.newInstance(this, args)
            )
            Place.DOC_PREVIEW -> {
                val document: Document? = args.getParcelableCompat(Extra.DOC)
                if (document != null && document.hasValidGifVideoLink()) {
                    val aid = args.getInt(Extra.ACCOUNT_ID)
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
            .replace(R.id.fragment, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    public override fun onPause() {
        ViewUtils.keyboardHide(this)
        super.onPause()
    }

    public override fun onResume() {
        val data = intent
        if (data != null && data.extras != null) {
            NotificationHelper.setBubbleOpened(
                (data.extras ?: return).getInt(Extra.ACCOUNT_ID),
                (data.extras ?: return).getInt(Extra.OWNER_ID)
            )
        }
        super.onResume()
    }

    public override fun onDestroy() {
        NotificationHelper.resetBubbleOpened(this, true)
        supportFragmentManager.removeOnBackStackChangedListener(mOnBackStackChangedListener)
        ViewUtils.keyboardHide(this)
        super.onDestroy()
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

    companion object {
        const val ACTION_OPEN_PLACE = "dev.ragnarok.fenrir.activity.ChatActivityBubbles.openPlace"


        fun forStart(context: Context?, accountId: Int, peer: Peer): Intent {
            val intent = Intent(context, ChatActivityBubbles::class.java)
            intent.action = ACTION_OPEN_PLACE
            intent.putExtra(Extra.PLACE, PlaceFactory.getChatPlace(accountId, accountId, peer))
            intent.putExtra(Extra.ACCOUNT_ID, accountId)
            intent.putExtra(Extra.OWNER_ID, peer.id)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }
    }
}