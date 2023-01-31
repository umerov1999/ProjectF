package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.DualTabPhotosFragment
import dev.ragnarok.fenrir.fragment.localphotos.LocalPhotosFragment
import dev.ragnarok.fenrir.fragment.vkphotos.IVKPhotosView
import dev.ragnarok.fenrir.fragment.vkphotos.VKPhotosFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.model.selection.Sources
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider

class DualTabPhotoActivity : NoMainActivity(), PlaceProvider {
    private var mMaxSelectionCount = 0
    private var mSources: Sources? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            mMaxSelectionCount = intent.getIntExtra(Extra.MAX_COUNT, 10)
            mSources = intent.getParcelableExtraCompat(Extra.SOURCES)
            attachStartFragment()
        } else {
            mMaxSelectionCount = savedInstanceState.getInt("mMaxSelectionCount")
            mSources = savedInstanceState.getParcelableCompat("mSources")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("mMaxSelectionCount", mMaxSelectionCount)
        outState.putParcelable("mSources", mSources)
    }

    private fun attachStartFragment() {
        val fragment = DualTabPhotosFragment.newInstance(mSources)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
            .replace(getMainContainerViewId(), fragment)
            .addToBackStack("dual-tab-photos")
            .commit()
    }

    override fun openPlace(place: Place) {
        val args = place.safeArguments()
        when (place.type) {
            Place.VK_PHOTO_ALBUM -> {
                val albumId = args.getInt(Extra.ALBUM_ID)
                val accountId = args.getLong(Extra.ACCOUNT_ID)
                val ownerId = args.getLong(Extra.OWNER_ID)
                val fragment = VKPhotosFragment.newInstance(
                    accountId,
                    ownerId,
                    albumId,
                    IVKPhotosView.ACTION_SELECT_PHOTOS
                )
                supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                    .replace(R.id.fragment, fragment)
                    .addToBackStack("vk-album-photos")
                    .commit()
            }

            Place.VK_INTERNAL_PLAYER -> {
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtras(args)
                startActivity(intent)
            }

            Place.SINGLE_PHOTO -> {
                place.launchActivityForResult(this, SinglePhotoActivity.newInstance(this, args))
            }

            Place.LOCAL_IMAGE_ALBUM -> {
                val album: LocalImageAlbum? = args.getParcelableCompat(Extra.ALBUM)
                val localPhotosFragment =
                    LocalPhotosFragment.newInstance(mMaxSelectionCount, album, false)
                supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                    .replace(R.id.fragment, localPhotosFragment)
                    .addToBackStack("local-album-photos")
                    .commit()
            }
        }
    }

    companion object {

        fun createIntent(context: Context, maxSelectionCount: Int, sources: Sources): Intent {
            return Intent(context, DualTabPhotoActivity::class.java)
                .putExtra(Extra.MAX_COUNT, maxSelectionCount)
                .putExtra(Extra.SOURCES, sources)
        }
    }
}