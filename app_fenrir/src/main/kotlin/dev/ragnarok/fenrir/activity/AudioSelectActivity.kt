package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.audio.AudioSelectTabsFragment
import dev.ragnarok.fenrir.fragment.audio.audios.AudiosFragment
import dev.ragnarok.fenrir.fragment.search.SingleTabSearchFragment
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider

class AudioSelectActivity : NoMainActivity(), PlaceProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState ?: run {
            val accountId = (intent.extras ?: return@run).getLong(Extra.ACCOUNT_ID)
            attachInitialFragment(accountId)
        }
    }

    private fun attachInitialFragment(accountId: Long) {
        val fragment = AudioSelectTabsFragment.newInstance(accountId)
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
            .replace(getMainContainerViewId(), fragment)
            .addToBackStack("audio-select")
            .commit()
    }

    override fun openPlace(place: Place) {
        if (place.type == Place.SINGLE_SEARCH) {
            val singleTabSearchFragment = SingleTabSearchFragment.newInstance(place.safeArguments())
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                .replace(getMainContainerViewId(), singleTabSearchFragment)
                .addToBackStack("audio-search-select")
                .commit()
        } else if (place.type == Place.AUDIOS_IN_ALBUM) {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                .replace(
                    getMainContainerViewId(),
                    AudiosFragment.newInstance(place.safeArguments(), true)
                )
                .addToBackStack("audio-in_playlist-select")
                .commit()
        }
    }

    companion object {
        /**
         * @param accountId От чьего имени получать
         */

        fun createIntent(context: Context, accountId: Long): Intent {
            return Intent(context, AudioSelectActivity::class.java)
                .putExtra(Extra.ACCOUNT_ID, accountId)
        }
    }
}