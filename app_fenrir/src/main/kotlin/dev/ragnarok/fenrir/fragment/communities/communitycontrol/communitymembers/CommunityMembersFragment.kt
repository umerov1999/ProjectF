package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitymembers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.activity.DeltaOwnerActivity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.fragment.search.peoplesearch.PeopleAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils

class CommunityMembersFragment :
    BaseMvpFragment<CommunityMembersPresenter, ICommunityMembersView>(),
    ICommunityMembersView, PeopleAdapter.ClickListener, SwipeRefreshLayout.OnRefreshListener,
    MenuProvider {
    private var mAdapter: PeopleAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mEmpty: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_group_members, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        PicassoPauseOnScrollListener.addListener(recyclerView)
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mEmpty = root.findViewById(R.id.empty)
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = PeopleAdapter(requireActivity(), emptyList())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_group_members, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_search -> {
                presenter?.fireSearch()
                return true
            }

            R.id.action_options -> {
                presenter?.fireFilter()
                return true
            }
        }
        return false
    }

    override fun showModMembers(
        add: List<Owner>,
        remove: List<Owner>,
        accountId: Long,
        ownerId: Long
    ) {
        if (add.isEmpty() && remove.isEmpty()) {
            return
        }
        DeltaOwnerActivity.showDeltaActivity(
            requireActivity(),
            accountId,
            DeltaOwner().setOwner(ownerId).appendToList(
                requireActivity(),
                R.string.new_follower,
                add
            ).appendToList(
                requireActivity(),
                R.string.not_follower,
                remove
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(users: List<Owner>) {
        mAdapter?.setItems(users)
        resolveEmptyTextVisibility()
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
        resolveEmptyTextVisibility()
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
        resolveEmptyTextVisibility()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
        resolveEmptyTextVisibility()
    }

    override fun openUserWall(accountId: Long, user: Owner) {
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun onSearch(accountId: Long, groupId: Long) {
        val criteria = PeopleSearchCriteria("")
            .setGroupId(groupId)
        PlaceFactory.getSingleTabSearchPlace(accountId, SearchContentType.PEOPLE, criteria)
            .tryOpenWith(
                requireActivity()
            )
    }

    override fun onOptions(filter: String?) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        if (filter.nonNullNoEmpty()) {
            menus.add(
                OptionRequest(
                    1,
                    getString(R.string.all),
                    R.drawable.ic_arrow_down,
                    true
                )
            )
        }
        if (filter != "friends") {
            menus.add(
                OptionRequest(
                    2,
                    getString(R.string.friends),
                    R.drawable.ic_arrow_down,
                    true
                )
            )
        }
        if (filter != "unsure") {
            menus.add(
                OptionRequest(
                    3,
                    getString(R.string.unsure),
                    R.drawable.ic_arrow_down,
                    true
                )
            )
        }
        if (filter != "donut") {
            menus.add(
                OptionRequest(
                    4,
                    getString(R.string.donut),
                    R.drawable.ic_arrow_down,
                    true
                )
            )
        }
        if (filter != "managers") {
            menus.add(
                OptionRequest(
                    5,
                    getString(R.string.managers),
                    R.drawable.ic_arrow_down,
                    true
                )
            )
        }
        menus.columns(1)
        menus.show(
            childFragmentManager,
            "community_members_options"
        ) { _, option ->
            when (option.id) {
                1 -> presenter?.fireFilter(null)
                2 -> presenter?.fireFilter("friends")
                3 -> presenter?.fireFilter("unsure")
                4 -> presenter?.fireFilter("donut")
                5 -> presenter?.fireFilter("managers")
            }
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityMembersPresenter> {
        return object : IPresenterFactory<CommunityMembersPresenter> {
            override fun create(): CommunityMembersPresenter {
                return CommunityMembersPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.GROUP_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onOwnerClick(owner: Owner) {
        presenter?.fireUserClick(
            owner
        )
    }

    override fun onResume() {
        super.onResume()
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.group_members)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    companion object {
        fun buildArgs(accountId: Long, groupId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.GROUP_ID, groupId)
            return args
        }

        fun newInstance(bundle: Bundle?): CommunityMembersFragment {
            val fragment = CommunityMembersFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}