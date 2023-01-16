package dev.ragnarok.fenrir.fragment.fave

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
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.fragment.fave.favearticles.FaveArticlesFragment
import dev.ragnarok.fenrir.fragment.fave.favelinks.FaveLinksFragment
import dev.ragnarok.fenrir.fragment.fave.favepages.FavePagesFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.fave.favephotos.FavePhotosFragment
import dev.ragnarok.fenrir.fragment.fave.faveposts.FavePostsFragment
import dev.ragnarok.fenrir.fragment.fave.faveproducts.FaveProductsFragment
import dev.ragnarok.fenrir.fragment.fave.favevideos.FaveVideosFragment
import dev.ragnarok.fenrir.link.types.FaveLink
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.createPageTransform
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class FaveTabsFragment : BaseFragment() {
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
        val root = inflater.inflate(R.layout.fragment_fave_tabs, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewPager: ViewPager2 = view.findViewById(R.id.viewpager)
        viewPager.offscreenPageLimit = 1
        viewPager.setPageTransformer(
            createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        setupViewPager(viewPager, view)
        if (requireArguments().containsKey(Extra.TAB)) {
            val tab = requireArguments().getInt(Extra.TAB)
            requireArguments().remove(Extra.TAB)
            viewPager.currentItem = tab
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.BOOKMARKS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.bookmarks)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_BOOKMARKS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    private fun setupViewPager(viewPager: ViewPager2, view: View) {
        val tabs: MutableList<ITab> = ArrayList()
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return newInstance(
                    accountId, true
                )
            }
        }, getString(R.string.pages)))
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return newInstance(
                    accountId, false
                )
            }
        }, getString(R.string.groups)))
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return FavePostsFragment.newInstance(
                    accountId
                )
            }
        }, getString(R.string.posts)))
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return FaveLinksFragment.newInstance(
                    accountId
                )
            }
        }, getString(R.string.links)))
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return FaveArticlesFragment.newInstance(
                    accountId
                )
            }
        }, getString(R.string.articles)))
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return FaveProductsFragment.newInstance(
                    accountId
                )
            }
        }, getString(R.string.products)))
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return FavePhotosFragment.newInstance(
                    accountId
                )
            }
        }, getString(R.string.photos)))
        tabs.add(Tab(object : IFragmentCreator {
            override fun create(): Fragment {
                return FaveVideosFragment.newInstance(
                    accountId
                )
            }
        }, getString(R.string.videos)))
        val adapter = Adapter(tabs, this)
        viewPager.adapter = adapter
        TabLayoutMediator(
            view.findViewById(R.id.tablayout),
            viewPager
        ) { tab: TabLayout.Tab, position: Int -> tab.text = tabs[position].tabTitle }.attach()
    }

    interface ITab {
        val tabTitle: String?
        val fragmentCreator: IFragmentCreator
    }

    interface IFragmentCreator {
        fun create(): Fragment
    }

    private class Tab(override val fragmentCreator: IFragmentCreator, val title: String) : ITab {
        override val tabTitle: String
            get() = title
    }

    internal class Adapter(private val tabs: List<ITab>, fragmentActivity: Fragment) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return tabs[position].fragmentCreator.create()
        }

        override fun getItemCount(): Int {
            return tabs.size
        }
    }

    companion object {
        const val TAB_UNKNOWN = -1
        const val TAB_PAGES = 0
        const val TAB_GROUPS = 1
        private const val TAB_POSTS = 2
        private const val TAB_LINKS = 3
        private const val TAB_ARTICLES = 4
        private const val TAB_PRODUCTS = 5
        private const val TAB_PHOTOS = 6
        private const val TAB_VIDEOS = 7
        fun buildArgs(accountId: Long, tab: Int): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.TAB, tab)
            return args
        }

        fun newInstance(accountId: Long, tab: Int): FaveTabsFragment {
            return newInstance(buildArgs(accountId, tab))
        }

        fun newInstance(args: Bundle?): FaveTabsFragment {
            val faveTabsFragment = FaveTabsFragment()
            faveTabsFragment.arguments = args
            return faveTabsFragment
        }

        fun getTabByLinkSection(linkSection: String?): Int {
            return if (linkSection.isNullOrEmpty()) {
                TAB_PHOTOS
            } else when (linkSection) {
                FaveLink.SECTION_PHOTOS -> TAB_PHOTOS
                FaveLink.SECTION_VIDEOS -> TAB_VIDEOS
                FaveLink.SECTION_POSTS -> TAB_POSTS
                FaveLink.SECTION_PAGES -> TAB_PAGES
                FaveLink.SECTION_LINKS -> TAB_LINKS
                FaveLink.SECTION_ARTICLES -> TAB_ARTICLES
                FaveLink.SECTION_PRODUCTS -> TAB_PRODUCTS
                else -> TAB_UNKNOWN
            }
        }
    }
}