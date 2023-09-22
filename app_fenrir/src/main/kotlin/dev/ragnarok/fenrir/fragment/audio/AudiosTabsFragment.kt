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
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.fragment.audio.audioplaylists.AudioPlaylistsFragment
import dev.ragnarok.fenrir.fragment.audio.audios.AudiosFragment
import dev.ragnarok.fenrir.fragment.audio.audiosrecommendation.AudiosRecommendationFragment
import dev.ragnarok.fenrir.fragment.audio.local.audioslocal.AudiosLocalFragment
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.fragment.localserver.audioslocalserver.AudiosLocalServerFragment
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getAudiosTabsSearchPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.createPageTransform
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class AudiosTabsFragment : BaseFragment(), MenuProvider {
    var accountId = 0L
        private set
    private var ownerId = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        ownerId = requireArguments().getLong(Extra.OWNER_ID)
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
            when (val fid = adapter.pFragments[position]) {
                MY_AUDIO -> tab.text =
                    getString(R.string.my_saved)

                PLAYLISTS -> tab.text =
                    getString(R.string.playlists)

                MY_RECOMMENDATIONS -> tab.text =
                    getString(R.string.recommendation)

                LOCAL -> tab.text =
                    getString(R.string.local_audios)

                LOCAL_SERVER -> tab.text =
                    getString(R.string.on_server)

                else -> tab.text =
                    VKApiAudio.Genre.getTitleByGenre(requireActivity(), fid)
            }
        }.attach()
        AppPerms.ignoreBattery(requireActivity())
    }

    internal fun CreateAudiosFragment(option_menu: Int): Fragment {
        return when (option_menu) {
            PLAYLISTS -> {
                val fragment = AudioPlaylistsFragment.newInstance(accountId, ownerId)
                fragment.requireArguments().putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                fragment
            }

            LOCAL -> {
                val fragment = AudiosLocalFragment.newInstance(accountId)
                fragment.requireArguments().putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                fragment
            }

            LOCAL_SERVER -> {
                AudiosLocalServerFragment.newInstance(accountId)
            }

            MY_AUDIO -> {
                val args = AudiosFragment.buildArgs(accountId, ownerId, null, null)
                args.putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                AudiosFragment.newInstance(args)
            }

            MY_RECOMMENDATIONS -> {
                val fragment = AudiosRecommendationFragment.newInstance(
                    accountId, ownerId, false, 0
                )
                fragment.requireArguments().putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                fragment
            }

            else -> {
                val fragment = AudiosRecommendationFragment.newInstance(
                    accountId, ownerId, true, option_menu
                )
                fragment.requireArguments().putBoolean(AudiosFragment.EXTRA_IN_TABS_CONTAINER, true)
                fragment
            }
        }
    }

    private fun setupViewPager(viewPager: ViewPager2, adapter: Adapter) {
        adapter.addFragment(MY_AUDIO)
        adapter.addFragment(PLAYLISTS)
        if (ownerId >= 0) {
            if (accountId == ownerId) {
                adapter.addFragment(LOCAL)
                if (Settings.get().main().localServer.enabled) {
                    adapter.addFragment(LOCAL_SERVER)
                }
            }
            adapter.addFragment(MY_RECOMMENDATIONS)
        }
        if (accountId == ownerId && Settings.get().main().isEnable_show_audio_top) {
            adapter.addFragment(VKApiAudio.Genre.TOP_ALL)
            adapter.addFragment(VKApiAudio.Genre.ETHNIC)
            adapter.addFragment(VKApiAudio.Genre.INSTRUMENTAL)
            adapter.addFragment(VKApiAudio.Genre.ACOUSTIC_AND_VOCAL)
            adapter.addFragment(VKApiAudio.Genre.ALTERNATIVE)
            adapter.addFragment(VKApiAudio.Genre.CLASSICAL)
            adapter.addFragment(VKApiAudio.Genre.DANCE_AND_HOUSE)
            adapter.addFragment(VKApiAudio.Genre.DRUM_AND_BASS)
            adapter.addFragment(VKApiAudio.Genre.EASY_LISTENING)
            adapter.addFragment(VKApiAudio.Genre.ELECTROPOP_AND_DISCO)
            adapter.addFragment(VKApiAudio.Genre.INDIE_POP)
            adapter.addFragment(VKApiAudio.Genre.METAL)
            adapter.addFragment(VKApiAudio.Genre.OTHER)
            adapter.addFragment(VKApiAudio.Genre.POP)
            adapter.addFragment(VKApiAudio.Genre.REGGAE)
            adapter.addFragment(VKApiAudio.Genre.ROCK)
            adapter.addFragment(VKApiAudio.Genre.TRANCE)
        }
        viewPager.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.music)
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
            getAudiosTabsSearchPlace(accountId).tryOpenWith(requireActivity())
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
        const val LOCAL_SERVER = -5
        const val LOCAL = -4
        const val PLAYLISTS = -3
        const val MY_RECOMMENDATIONS = -2
        const val MY_AUDIO = -1
        fun buildArgs(accountId: Long, ownerId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            return args
        }

        fun newInstance(accountId: Long, ownerId: Long): AudiosTabsFragment {
            return newInstance(buildArgs(accountId, ownerId))
        }

        fun newInstance(args: Bundle?): AudiosTabsFragment {
            val fragment = AudiosTabsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}