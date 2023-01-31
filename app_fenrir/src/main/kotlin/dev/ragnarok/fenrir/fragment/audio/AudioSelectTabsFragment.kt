package dev.ragnarok.fenrir.fragment.audio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.audio.audioplaylists.AudioPlaylistsFragment
import dev.ragnarok.fenrir.fragment.audio.audios.AudiosFragment
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleTabSearchPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.createPageTransform
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class AudioSelectTabsFragment : BaseFragment(), MenuProvider {
    var accountId = 0L
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_audios_tabs, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        val viewPager: ViewPager2 = view.findViewById(R.id.fragment_audios_pager)
        viewPager.offscreenPageLimit = 1
        viewPager.setPageTransformer(
            createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        val adapter = Adapter(this)
        setupViewPager(viewPager, adapter)
        TabLayoutMediator(
            view.findViewById(R.id.fragment_audios_tabs),
            viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            val fid: Int = adapter.pFragments[position]
            if (fid == MY_AUDIO) tab.text =
                getString(R.string.my_saved) else if (fid == PLAYLISTS) tab.text =
                getString(R.string.playlists)
        }.attach()
    }

    internal fun CreateAudiosFragment(option_menu: Int): Fragment {
        return if (option_menu == PLAYLISTS) {
            val fragment = AudioPlaylistsFragment.newInstanceSelect(
                accountId
            )
            fragment.requireArguments().putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
            fragment
        } else {
            val args = AudiosFragment.buildArgs(accountId, accountId, null, null)
            args.putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
            AudiosFragment.newInstance(args, true)
        }
    }

    private fun setupViewPager(viewPager: ViewPager2, adapter: Adapter) {
        adapter.addFragment(MY_AUDIO)
        adapter.addFragment(PLAYLISTS)
        viewPager.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.select_audio)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_AUDIOS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_search) {
            val criteria = AudioSearchCriteria("", by_artist = false, in_main_page = true)
            getSingleTabSearchPlace(
                accountId,
                SearchContentType.AUDIOS_SELECT,
                criteria
            ).tryOpenWith(requireActivity())
            return true
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_audio_main, menu)
    }

    private inner class Adapter(fragmentActivity: Fragment) :
        FragmentStateAdapter(fragmentActivity) {
        val pFragments: MutableList<Int> = ArrayList()
        fun addFragment(fragment: Int) {
            pFragments.add(fragment)
        }

        override fun createFragment(position: Int): Fragment {
            return CreateAudiosFragment(pFragments[position])
        }

        override fun getItemCount(): Int {
            return pFragments.size
        }
    }

    companion object {
        const val MY_AUDIO = 0
        const val PLAYLISTS = 1
        fun buildArgs(accountId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            return args
        }

        fun newInstance(accountId: Long): AudioSelectTabsFragment {
            return newInstance(buildArgs(accountId))
        }

        fun newInstance(args: Bundle?): AudioSelectTabsFragment {
            val fragment = AudioSelectTabsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}