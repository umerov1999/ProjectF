package dev.ragnarok.fenrir.fragment.friends.friendstabs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.friends.allfriends.AllFriendsFragment
import dev.ragnarok.fenrir.fragment.friends.followers.FollowersFragment
import dev.ragnarok.fenrir.fragment.friends.mutualfriends.MutualFriendsFragment
import dev.ragnarok.fenrir.fragment.friends.onlinefriends.OnlineFriendsFragment
import dev.ragnarok.fenrir.fragment.friends.recommendationsfriends.RecommendationsFriendsFragment
import dev.ragnarok.fenrir.fragment.friends.requests.RequestsFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.FriendsCounters
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.createPageTransform
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class FriendsTabsFragment : BaseMvpFragment<FriendsTabsPresenter, IFriendsTabsView>(),
    IFriendsTabsView, MenuProvider {
    private var adapter: Adapter? = null
    private var tabLayout: TabLayout? = null
    private var viewPager: ViewPager2? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_friends_tab, menu)
        menu.findItem(R.id.action_birthdays).isVisible = (presenter?.isMe() == true)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_birthdays) {
            presenter?.fireFriendsBirthday()
            return true
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        val root = inflater.inflate(R.layout.fragment_friends_tabs, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        viewPager = root.findViewById(R.id.viewpager)
        viewPager?.setPageTransformer(
            createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        tabLayout = root.findViewById(R.id.tablayout)
        return root
    }

    private fun setupTabCounterView(id: Int, count: Int) {
        try {
            adapter?.updateCount(id, count)
        } catch (ignored: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.FRIENDS_AND_FOLLOWERS)
        setToolbarTitle(this, R.string.friends)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FriendsTabsPresenter> {
        return object : IPresenterFactory<FriendsTabsPresenter> {
            override fun create(): FriendsTabsPresenter {
                return FriendsTabsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.USER_ID),
                    requireArguments().getParcelableCompat(Extra.COUNTERS),
                    saveInstanceState
                )
            }
        }
    }

    override fun displayCounters(counters: FriendsCounters) {
        setupTabCounterView(TAB_ALL_FRIENDS, counters.getAll())
        setupTabCounterView(TAB_ONLINE, counters.getOnline())
        setupTabCounterView(TAB_FOLLOWERS, counters.getFollowers())
        setupTabCounterView(TAB_REQUESTS, 0)
        setupTabCounterView(TAB_MUTUAL, counters.getMutual())
        for (i in 0 until (tabLayout?.tabCount ?: 0)) {
            tabLayout?.getTabAt(i)?.text = adapter?.getPageTitle(i)
        }
    }

    override fun configTabs(accountId: Long, userId: Long, isNotMyPage: Boolean) {
        adapter = Adapter(requireActivity(), this, accountId, userId, isNotMyPage)
        viewPager?.adapter = adapter
        tabLayout?.let {
            viewPager?.let { it1 ->
                TabLayoutMediator(
                    it,
                    it1
                ) { tab: TabLayout.Tab, position: Int ->
                    tab.text = adapter?.getPageTitle(position)
                }.attach()
            }
        }
        if (requireArguments().containsKey(Extra.TAB)) {
            val tab = requireArguments().getInt(Extra.TAB)
            requireArguments().remove(Extra.TAB)
            var pos = 0
            var succ = false
            adapter?.mFragmentTitles.nonNullNoEmpty {
                for (i in it) {
                    if (i.Id == tab) {
                        succ = true
                        break
                    }
                    pos++
                }
            }
            if (succ) {
                viewPager?.currentItem = pos
            }
        }
    }

    override fun displayUserNameAtToolbar(userName: String?) {
        setToolbarSubtitle(this, userName)
    }

    override fun setDrawerFriendsSectionSelected(selected: Boolean) {
        if (requireActivity() is OnSectionResumeCallback) {
            if (selected) {
                (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_FRIENDS)
            } else {
                (requireActivity() as OnSectionResumeCallback).onClearSelection()
            }
        }
    }

    override fun onFriendsBirthday(accountId: Long, ownerId: Long) {
        PlaceFactory.getFriendsBirthdaysPlace(accountId, ownerId).tryOpenWith(requireActivity())
    }

    private interface CreateFriendsFragment {
        fun create(): Fragment
    }

    private class Adapter(
        context: Context,
        fm: Fragment,
        accountId: Long,
        userId: Long,
        private val isNotMyPage: Boolean
    ) : FragmentStateAdapter(fm) {
        val mFragmentTitles: MutableList<FriendSource>
        fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitles[position].getTitle()
        }

        override fun createFragment(position: Int): Fragment {
            return mFragmentTitles[position].fragment
        }

        override fun getItemCount(): Int {
            return if (isNotMyPage) 4 else 5
        }

        fun updateCount(id: Int, count: Int?) {
            for (i in mFragmentTitles) {
                if (i.isId(id)) {
                    i.updateCount(count)
                    break
                }
            }
        }

        init {
            mFragmentTitles = ArrayList(itemCount)
            mFragmentTitles.add(
                FriendSource(
                    TAB_ALL_FRIENDS,
                    context.getString(R.string.all_friends),
                    object : CreateFriendsFragment {
                        override fun create(): Fragment {
                            return AllFriendsFragment.newInstance(accountId, userId)
                        }
                    })
            )
            mFragmentTitles.add(
                FriendSource(
                    TAB_ONLINE,
                    context.getString(R.string.online),
                    object : CreateFriendsFragment {
                        override fun create(): Fragment {
                            return OnlineFriendsFragment.newInstance(accountId, userId)
                        }
                    })
            )
            mFragmentTitles.add(
                FriendSource(
                    TAB_FOLLOWERS,
                    context.getString(R.string.counter_followers),
                    object : CreateFriendsFragment {
                        override fun create(): Fragment {
                            return FollowersFragment.newInstance(accountId, userId)
                        }
                    })
            )
            if (isNotMyPage) {
                mFragmentTitles.add(
                    FriendSource(
                        TAB_MUTUAL,
                        context.getString(R.string.mutual_friends),
                        object : CreateFriendsFragment {
                            override fun create(): Fragment {
                                return MutualFriendsFragment.newInstance(
                                    accountId,
                                    userId
                                )
                            }
                        })
                )
            } else {
                mFragmentTitles.add(
                    FriendSource(
                        TAB_REQUESTS,
                        context.getString(R.string.counter_requests),
                        object : CreateFriendsFragment {
                            override fun create(): Fragment {
                                return RequestsFragment.newInstance(accountId, userId)
                            }
                        })
                )
                mFragmentTitles.add(
                    FriendSource(
                        TAB_RECOMMENDATIONS,
                        context.getString(R.string.recommendation),
                        object : CreateFriendsFragment {
                            override fun create(): Fragment {
                                return RecommendationsFriendsFragment.newInstance(
                                    accountId,
                                    userId
                                )
                            }
                        })
                )
            }
        }
    }

    private class FriendSource(
        val Id: Int,
        private val Title: String,
        private val call: CreateFriendsFragment
    ) {
        private var Count: Int? = null
        fun updateCount(Count: Int?) {
            this.Count = Count
        }

        fun getTitle(): String {
            return if (Count.orZero() > 0) {
                "$Title $Count"
            } else Title
        }

        fun isId(Id: Int): Boolean {
            return this.Id == Id
        }

        val fragment: Fragment
            get() = call.create()
    }

    companion object {
        const val TAB_ALL_FRIENDS = 0
        const val TAB_ONLINE = 1
        const val TAB_FOLLOWERS = 2
        const val TAB_REQUESTS = 3
        const val TAB_MUTUAL = 4
        const val TAB_RECOMMENDATIONS = 5
        fun buildArgs(accountId: Long, userId: Long, tab: Int, counters: FriendsCounters?): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.USER_ID, userId)
            args.putInt(Extra.TAB, tab)
            args.putParcelable(Extra.COUNTERS, counters)
            return args
        }

        fun newInstance(args: Bundle?): FriendsTabsFragment {
            val friendsFragment = FriendsTabsFragment()
            friendsFragment.arguments = args
            return friendsFragment
        }

        fun newInstance(
            accountId: Long,
            userId: Long,
            tab: Int,
            counters: FriendsCounters?
        ): FriendsTabsFragment {
            return newInstance(buildArgs(accountId, userId, tab, counters))
        }
    }
}