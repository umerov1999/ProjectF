package dev.ragnarok.fenrir.fragment.communities

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.activity.DeltaOwnerActivity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.DataWrapper
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.MySearchView.OnBackButtonClickListener

class CommunitiesFragment : BaseMvpFragment<CommunitiesPresenter, ICommunitiesView>(),
    ICommunitiesView, MySearchView.OnQueryTextListener, CommunitiesAdapter.ActionListener,
    BackPressCallback, OnBackButtonClickListener {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: CommunitiesAdapter? = null
    private var mSearchView: MySearchView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_communities, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = CommunitiesAdapter(requireActivity(), emptyList(), arrayOfNulls(0))
        mAdapter?.setActionListener(this)
        recyclerView.adapter = mAdapter
        mSearchView = root.findViewById(R.id.searchview)
        mSearchView?.setOnBackButtonClickListener(this)
        mSearchView?.setRightButtonVisibility(false)
        mSearchView?.setOnQueryTextListener(this)
        mSearchView?.setLeftIcon(R.drawable.magnify)
        return root
    }

    override fun displayData(
        own: DataWrapper<Community>,
        filtered: DataWrapper<Community>,
        search: DataWrapper<Community>
    ) {
        if (mAdapter != null) {
            val wrappers: MutableList<DataWrapper<Community>> = ArrayList()
            wrappers.add(own)
            wrappers.add(filtered)
            wrappers.add(search)
            @StringRes val titles =
                arrayOf(null, R.string.quick_search_title, R.string.results_in_a_network)
            mAdapter?.setData(wrappers, titles)
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.COMMUNITIES)
        setToolbarTitle(this, R.string.groups)
        setToolbarSubtitle(this, null) // TODO: 04.10.2017
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyOwnDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(0, position, count)
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun showCommunityWall(accountId: Int, community: Community) {
        getOwnerWallPlace(accountId, community).tryOpenWith(requireActivity())
    }

    override fun notifySearchDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(2, position, count)
    }

    override fun showModCommunities(
        add: List<Owner>,
        remove: List<Owner>,
        accountId: Int,
        ownerId: Int
    ) {
        if (add.isEmpty() && remove.isEmpty()) {
            return
        }
        DeltaOwnerActivity.showDeltaActivity(
            requireActivity(),
            accountId,
            DeltaOwner().setOwner(ownerId).appendToList(
                requireActivity(),
                R.string.new_communities,
                add
            ).appendToList(
                requireActivity(),
                R.string.not_communities,
                remove
            )
        )
    }

    override fun showCommunityMenu(community: Community) {
        val delete = getString(R.string.delete)
        val options: MutableList<String> = ArrayList()
        options.add(delete)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(community.fullName)
            .setItems(options.toTypedArray()) { _: DialogInterface?, which: Int ->
                val selected = options[which]
                if (selected == delete) {
                    presenter?.fireUnsubscribe(
                        community
                    )
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunitiesPresenter> {
        return object : IPresenterFactory<CommunitiesPresenter> {
            override fun create(): CommunitiesPresenter {
                return CommunitiesPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getInt(Extra.USER_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        presenter?.fireSearchQueryChanged(
            query
        )
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        presenter?.fireSearchQueryChanged(
            newText
        )
        return true
    }

    override fun onCommunityClick(community: Community) {
        presenter?.fireCommunityClick(
            community
        )
    }

    override fun onCommunityLongClick(community: Community): Boolean {
        return presenter?.fireCommunityLongClick(community) ?: false
    }

    override fun onBackPressed(): Boolean {
        val query: CharSequence? = mSearchView?.text
        if (query.isNullOrEmpty()) {
            return true
        }
        mSearchView?.setQuery("")
        return false
    }

    override fun onBackButtonClick() {
        if (requireActivity().supportFragmentManager.backStackEntryCount == 1 && requireActivity() is AppStyleable) {
            (requireActivity() as AppStyleable).openMenu(true)
        } else {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        fun newInstance(accountId: Int, userId: Int): CommunitiesFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.USER_ID, userId)
            val fragment = CommunitiesFragment()
            fragment.arguments = args
            return fragment
        }
    }
}