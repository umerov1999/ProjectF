package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.search.SingleTabSearchFragment
import dev.ragnarok.fenrir.fragment.videos.IVideosListView
import dev.ragnarok.fenrir.fragment.videos.VideosFragment
import dev.ragnarok.fenrir.fragment.videos.VideosTabsFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.util.Utils

class VideoSelectActivity : NoMainActivity(), PlaceProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val accountId = (intent.extras ?: return).getLong(Extra.ACCOUNT_ID)
            val ownerId = (intent.extras ?: return).getLong(Extra.OWNER_ID)
            attachInitialFragment(accountId, ownerId)
        }
    }

    private fun attachInitialFragment(accountId: Long, ownerId: Long) {
        val fragment =
            VideosTabsFragment.newInstance(accountId, ownerId, IVideosListView.ACTION_SELECT)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
            .replace(getMainContainerViewId(), fragment)
            .addToBackStack("video-tabs")
            .commit()
    }

    override fun openPlace(place: Place) {
        when (place.type) {
            Place.VIDEO_ALBUM -> {
                val fragment: Fragment = VideosFragment.newInstance(place.safeArguments())
                supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                    .replace(getMainContainerViewId(), fragment)
                    .addToBackStack("video-album")
                    .commit()
            }
            Place.SINGLE_SEARCH -> {
                val singleTabSearchFragment =
                    SingleTabSearchFragment.newInstance(place.safeArguments())
                supportFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                    .replace(getMainContainerViewId(), singleTabSearchFragment)
                    .addToBackStack("video-search")
                    .commit()
            }
            Place.VIDEO_PREVIEW -> {
                setResult(
                    RESULT_OK, Intent().putParcelableArrayListExtra(
                        Extra.ATTACHMENTS, Utils.singletonArrayList(
                            place.safeArguments().getParcelableCompat(
                                Extra.VIDEO
                            )
                        )
                    )
                )
                finish()
            }
        }
    }

    companion object {
        /**
         * @param accountId От чьего имени получать
         * @param ownerId   Чьи получать
         */

        fun createIntent(context: Context, accountId: Long, ownerId: Long): Intent {
            return Intent(context, VideoSelectActivity::class.java)
                .putExtra(Extra.ACCOUNT_ID, accountId)
                .putExtra(Extra.OWNER_ID, ownerId)
        }
    }
}