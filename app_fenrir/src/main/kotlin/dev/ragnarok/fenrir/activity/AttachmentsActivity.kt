package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.model.AttachmentsTypes
import dev.ragnarok.fenrir.fragment.docs.DocsFragment
import dev.ragnarok.fenrir.fragment.docs.DocsListPresenter
import dev.ragnarok.fenrir.fragment.videos.IVideosListView
import dev.ragnarok.fenrir.fragment.videos.VideosFragment
import dev.ragnarok.fenrir.fragment.videos.VideosTabsFragment
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider

class AttachmentsActivity : NoMainActivity(), PlaceProvider {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            var fragment: Fragment? = null
            val type = (intent.extras ?: return).getInt(Extra.TYPE)
            val accountId = (intent.extras ?: return).getLong(Extra.ACCOUNT_ID)
            when (type) {
                AttachmentsTypes.DOC -> fragment =
                    DocsFragment.newInstance(accountId, accountId, DocsListPresenter.ACTION_SELECT)

                AttachmentsTypes.VIDEO -> fragment = VideosTabsFragment.newInstance(
                    accountId,
                    accountId,
                    IVideosListView.ACTION_SELECT
                )
            }
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.fragment, fragment ?: return)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun openPlace(place: Place) {
        if (place.type == Place.VIDEO_ALBUM) {
            val fragment: Fragment = VideosFragment.newInstance(place.safeArguments())
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                .replace(R.id.fragment, fragment)
                .addToBackStack("video_album")
                .commit()
        }
    }

    companion object {

        fun createIntent(context: Context, accountId: Long, type: Int): Intent {
            return Intent(context, AttachmentsActivity::class.java)
                .putExtra(Extra.TYPE, type)
                .putExtra(Extra.ACCOUNT_ID, accountId)
        }
    }
}