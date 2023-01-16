package dev.ragnarok.fenrir.fragment.communitycontrol.communityinfocontacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class CommunityInfoContactsFragment :
    BaseMvpFragment<CommunityInfoContactsPresenter, ICommunityInfoContactsView>(),
    ICommunityInfoContactsView {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: CommunityInfoContactsAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_managers, container, false)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        root.findViewById<View>(R.id.button_add).visibility = View.INVISIBLE
        mAdapter = CommunityInfoContactsAdapter(emptyList())
        mAdapter?.setActionListener(object : CommunityInfoContactsAdapter.ActionListener {
            override fun onManagerClick(manager: User) {
                presenter?.fireManagerClick(
                    manager
                )
            }
        })
        recyclerView.adapter = mAdapter
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityInfoContactsPresenter> {
        return object : IPresenterFactory<CommunityInfoContactsPresenter> {
            override fun create(): CommunityInfoContactsPresenter {
                return CommunityInfoContactsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.GROUP_ID)!!,
                    saveInstanceState
                )
            }
        }
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun displayRefreshing(loadingNow: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loadingNow
    }

    override fun displayData(managers: List<Manager>) {
        mAdapter?.setData(managers)
    }

    override fun showUserProfile(accountId: Long, user: User) {
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    override fun notifyItemRemoved(index: Int) {
        mAdapter?.notifyItemRemoved(index)
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index)
    }

    override fun notifyItemAdded(index: Int) {
        mAdapter?.notifyItemInserted(index)
    }

    companion object {
        fun newInstance(accountId: Long, groupId: Community?): CommunityInfoContactsFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.GROUP_ID, groupId)
            val fragment = CommunityInfoContactsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}