package dev.ragnarok.fenrir.fragment.videos

import android.os.Bundle
import android.view.*
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
import dev.ragnarok.fenrir.db.OwnerHelper
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.fragment.localserver.videoslocalserver.VideosLocalServerFragment
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.fragment.videoalbums.VideoAlbumsFragment
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleTabSearchPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.createPageTransform
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class VideosTabsFragment : BaseFragment(), MenuProvider {
    var accountId = 0
        private set
    private var ownerId = 0
    private var action: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
        ownerId = requireArguments().getInt(Extra.OWNER_ID)
        action = requireArguments().getString(Extra.ACTION)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_videos_tabs, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        val viewPager: ViewPager2 = view.findViewById(R.id.fragment_videos_pager)
        viewPager.offscreenPageLimit = 1
        viewPager.setPageTransformer(
            createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        val adapter = Adapter(this)
        viewPager.adapter = adapter
        adapter.addFragment(VIDEOS)
        adapter.addFragment(ALBUMS)
        if (accountId == ownerId && Settings.get()
                .other().localServer.enabled && !IVideosListView.ACTION_SELECT.equals(
                action,
                ignoreCase = true
            )
        ) {
            adapter.addFragment(LOCAL_SERVER)
        }
        TabLayoutMediator(
            view.findViewById(R.id.fragment_videos_tabs),
            viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            when (adapter.pFragments[position]) {
                LOCAL_SERVER -> tab.setText(R.string.on_server)
                VIDEOS -> tab.setText(R.string.videos_my)
                ALBUMS -> tab.setText(R.string.videos_albums)
            }
        }.attach()
    }

    private val isMy: Boolean
        get() = accountId == ownerId

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.VIDEOS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.videos)
            actionBar.subtitle =
                if (isMy) null else OwnerHelper.loadOwnerFullName(
                    requireActivity(),
                    accountId,
                    ownerId
                )
        }
        if (requireActivity() is OnSectionResumeCallback) {
            if (isMy) {
                (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_VIDEOS)
            } else {
                (requireActivity() as OnSectionResumeCallback).onClearSelection()
            }
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
            val criteria = VideoSearchCriteria("", true, action)
            getSingleTabSearchPlace(accountId, SearchContentType.VIDEOS, criteria).tryOpenWith(
                requireActivity()
            )
            return true
        }
        return false
    }

    internal fun CreateVideosFragment(option_menu: Int): Fragment {
        when (option_menu) {
            LOCAL_SERVER -> return VideosLocalServerFragment.newInstance(
                accountId
            )
            VIDEOS -> {
                val fragment = VideosFragment.newInstance(accountId, ownerId, 0, action, null)
                fragment.requireArguments().putBoolean(VideosFragment.EXTRA_IN_TABS_CONTAINER, true)
                return fragment
            }
            ALBUMS -> return VideoAlbumsFragment.newInstance(accountId, ownerId, action)
        }
        throw UnsupportedOperationException()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_video_main, menu)
    }

    internal inner class Adapter(fm: Fragment) : FragmentStateAdapter(fm) {
        val pFragments: MutableList<Int> = ArrayList()
        fun addFragment(fragment: Int) {
            pFragments.add(fragment)
        }

        override fun createFragment(position: Int): Fragment {
            return CreateVideosFragment(pFragments[position])
        }

        override fun getItemCount(): Int {
            return pFragments.size
        }
    }

    companion object {
        const val LOCAL_SERVER = -1
        const val VIDEOS = 0
        const val ALBUMS = 1
        fun buildArgs(accountId: Int, ownerId: Int, action: String?): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.OWNER_ID, ownerId)
            args.putString(Extra.ACTION, action)
            return args
        }

        fun newInstance(accountId: Int, ownerId: Int, action: String?): VideosTabsFragment {
            return newInstance(buildArgs(accountId, ownerId, action))
        }

        fun newInstance(args: Bundle?): VideosTabsFragment {
            val fragment = VideosTabsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}