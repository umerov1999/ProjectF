package dev.ragnarok.fenrir.activity

import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.photos.vkphotoalbums.VKPhotoAlbumsFragment
import dev.ragnarok.fenrir.fragment.photos.vkphotos.VKPhotosFragment
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider

class PhotoAlbumsActivity : NoMainActivity(), PlaceProvider {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val intent = intent
            val accountId = (intent.extras ?: return).getLong(Extra.ACCOUNT_ID)
            val ownerId = (intent.extras ?: return).getLong(Extra.OWNER_ID)
            val action = intent.getStringExtra(Extra.ACTION)
            val fragment =
                VKPhotoAlbumsFragment.newInstance(accountId, ownerId, action, null, false)
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
                .add(R.id.fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun openPlace(place: Place) {
        if (place.type == Place.VK_PHOTO_ALBUM) {
            val fragment = VKPhotosFragment.newInstance(place.safeArguments())
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                .replace(R.id.fragment, fragment)
                .addToBackStack("photos")
                .commit()
        }
    }
}