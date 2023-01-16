package dev.ragnarok.fenrir.fragment.communitycontrol

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
import dev.ragnarok.fenrir.fragment.communitycontrol.communityblacklist.CommunityBlacklistFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.communitycontrol.communitylinks.CommunityLinksFragment
import dev.ragnarok.fenrir.fragment.communitycontrol.communitymanagers.CommunityManagersFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.communitycontrol.communityoptions.CommunityOptionsFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.GroupSettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.createPageTransform

class CommunityControlFragment : Fragment() {
    private lateinit var mCommunity: Community

    private var mSettings: GroupSettings? = null
    private var mAccountId = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSettings = requireArguments().getParcelableCompat(Extra.SETTINGS)
        mAccountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        mCommunity = (requireArguments().getParcelableCompat(Extra.OWNER) ?: return)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_control, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val pager: ViewPager2 = root.findViewById(R.id.view_pager)
        pager.offscreenPageLimit = 1
        pager.setPageTransformer(
            createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        val tabs: MutableList<ITab> = ArrayList()
        if (mCommunity.adminLevel > 0) tabs.add(
            Tab(
                getString(R.string.community_blacklist_tab_title),
                object : IFragmentCreator {
                    override fun create(): Fragment {
                        return newInstance(mAccountId, mCommunity.id)
                    }
                })
        )
        tabs.add(
            Tab(
                getString(R.string.community_links_tab_title),
                object : IFragmentCreator {
                    override fun create(): Fragment {
                        return CommunityLinksFragment.newInstance(
                            mAccountId,
                            mCommunity.id
                        )
                    }
                })
        )
        tabs.add(
            Tab(
                if (mCommunity.adminLevel == 0) getString(R.string.community_managers_contacts) else getString(
                    R.string.community_managers_tab_title
                ), object : IFragmentCreator {
                    override fun create(): Fragment {
                        return newInstance(mAccountId, mCommunity)
                    }
                })
        )
        if (mSettings != null) {
            tabs.add(
                Tab(
                    getString(R.string.settings),
                    object : IFragmentCreator {
                        override fun create(): Fragment {
                            return CommunityOptionsFragment.newInstance(
                                mAccountId,
                                mCommunity,
                                mSettings!!
                            )
                        }
                    })
            )
        }
        val tab_set = Adapter(tabs, this)
        pager.adapter = tab_set
        TabLayoutMediator(
            root.findViewById(R.id.tablayout),
            pager
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = tab_set.tabs[position].tabTitle
        }.attach()
        return root
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(this, R.string.community_control)
        setToolbarSubtitle(this, mCommunity.fullName)
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    private interface ITab {
        val tabTitle: String?
        val fragmentCreator: IFragmentCreator
    }

    private interface IFragmentCreator {
        fun create(): Fragment
    }

    private class Tab(val title: String, override val fragmentCreator: IFragmentCreator) : ITab {
        override val tabTitle: String
            get() = title
    }

    private class Adapter(val tabs: List<ITab>, fragmentActivity: Fragment) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return tabs[position].fragmentCreator.create()
        }

        override fun getItemCount(): Int {
            return tabs.size
        }
    }

    companion object {
        fun newInstance(
            accountId: Long,
            community: Community?,
            settings: GroupSettings?
        ): CommunityControlFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.SETTINGS, settings)
            args.putParcelable(Extra.OWNER, community)
            val fragment = CommunityControlFragment()
            fragment.arguments = args
            return fragment
        }
    }
}