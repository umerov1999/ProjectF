package dev.ragnarok.fenrir.fragment.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Accounts.fromArgs
import dev.ragnarok.fenrir.util.Utils.createPageTransform
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class AudioSearchTabsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_search_tabs, container, false)
        val mViewPager: ViewPager2 = root.findViewById(R.id.viewpager)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val mAdapter = Adapter(this)
        mViewPager.adapter = mAdapter
        mViewPager.offscreenPageLimit = 1
        mViewPager.setPageTransformer(
            createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        TabLayoutMediator(
            root.findViewById(R.id.tablayout),
            mViewPager
        ) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                TAB_MUSIC -> tab.setText(R.string.music)
                TAB_AUDIO_PLAYLISTS -> tab.setText(R.string.playlists)
                TAB_ARTISTS -> tab.setText(R.string.artists)
            }
        }.attach()
        mViewPager.currentItem = TAB_MUSIC
        return root
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
        setToolbarTitle(this, R.string.search)
        setToolbarSubtitle(this, null) //
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_AUDIOS)
        }
    }

    private inner class Adapter(fm: Fragment) : FragmentStateAdapter(fm) {
        override fun createFragment(position: Int): Fragment {
            val accountId = fromArgs(arguments)
            val fragment: Fragment = when (position) {
                TAB_MUSIC -> SingleTabSearchFragment.newInstance(
                    accountId,
                    SearchContentType.AUDIOS,
                    AudioSearchCriteria("", by_artist = false, in_main_page = true)
                )
                TAB_AUDIO_PLAYLISTS -> SingleTabSearchFragment.newInstance(
                    accountId,
                    SearchContentType.AUDIO_PLAYLISTS
                )
                TAB_ARTISTS -> SingleTabSearchFragment.newInstance(
                    accountId,
                    SearchContentType.ARTISTS
                )
                else -> throw IllegalArgumentException()
            }
            return fragment
        }

        override fun getItemCount(): Int {
            return 3
        }
    }

    companion object {
        const val TAB_MUSIC = 0
        const val TAB_AUDIO_PLAYLISTS = 1
        const val TAB_ARTISTS = 2
        fun buildArgs(accountId: Int): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            return args
        }

        fun newInstance(args: Bundle?): AudioSearchTabsFragment {
            val fragment = AudioSearchTabsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}