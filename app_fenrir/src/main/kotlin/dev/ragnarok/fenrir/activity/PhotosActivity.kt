package dev.ragnarok.fenrir.activity

import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.localimagealbums.LocalImageAlbumsFragment
import dev.ragnarok.fenrir.fragment.localphotos.LocalPhotosFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider

class PhotosActivity : NoMainActivity(), PlaceProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            attachAlbumsFragment()
        }
    }

    private fun attachAlbumsFragment() {
        val ignoredFragment = LocalImageAlbumsFragment()
        ignoredFragment.arguments = intent.extras
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
            .replace(R.id.fragment, ignoredFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun openPlace(place: Place) {
        if (place.type == Place.LOCAL_IMAGE_ALBUM) {
            val maxSelectionCount = intent.getIntExtra(EXTRA_MAX_SELECTION_COUNT, 10)
            val album: LocalImageAlbum? = place.safeArguments().getParcelableCompat(Extra.ALBUM)
            val localPhotosFragment =
                LocalPhotosFragment.newInstance(maxSelectionCount, album, false)
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                .replace(R.id.fragment, localPhotosFragment)
                .addToBackStack("photos")
                .commit()
        } else if (place.type == Place.SINGLE_PHOTO) {
            place.launchActivityForResult(
                this,
                SinglePhotoActivity.newInstance(this, place.safeArguments())
            )
        }
    }

    companion object {
        const val EXTRA_MAX_SELECTION_COUNT = "max_selection_count"
    }
}