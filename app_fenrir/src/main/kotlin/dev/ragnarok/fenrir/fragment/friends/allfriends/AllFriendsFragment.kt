package dev.ragnarok.fenrir.fragment.friends.allfriends

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.DeltaOwnerActivity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UsersPart
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.MySearchView

class AllFriendsFragment : BaseMvpFragment<AllFriendsPresenter, IAllFriendsView>(),
    FriendsRecycleAdapter.Listener, IAllFriendsView {
    private var mAdapter: FriendsRecycleAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        val root = inflater.inflate(R.layout.fragment_friends, container, false)
        val mRecyclerView: RecyclerView = root.findViewById(R.id.list)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val manager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        mRecyclerView.layoutManager = manager
        mRecyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        mRecyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        val mySearchView: MySearchView = root.findViewById(R.id.searchview)
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    query
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    newText
                )
                return false
            }
        })
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(mySearchView.windowToken, 0)
        mAdapter = FriendsRecycleAdapter(emptyList(), requireActivity())
        mAdapter?.setListener(this)
        mRecyclerView.adapter = mAdapter
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AllFriendsPresenter> {
        return object : IPresenterFactory<AllFriendsPresenter> {
            override fun create(): AllFriendsPresenter {
                return AllFriendsPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getInt(Extra.USER_ID), saveInstanceState
                )
            }
        }
    }

    override fun notifyDatasetChanged(grouping: Boolean) {
        mAdapter?.setGroup(grouping)
        mAdapter?.notifyDataSetChanged()
    }

    override fun setSwipeRefreshEnabled(enabled: Boolean) {
        mSwipeRefreshLayout?.isEnabled = enabled
    }

    override fun displayData(data: List<UsersPart>, grouping: Boolean) {
        mAdapter?.setData(data, grouping)
    }

    override fun notifyItemRangeInserted(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun showUserWall(accountId: Int, user: User) {
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun showModFriends(
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
                R.string.new_friend,
                add
            ).appendToList(
                requireActivity(),
                R.string.not_friend,
                remove
            )
        )
    }

    override fun onUserClick(user: User) {
        presenter?.fireUserClick(
            user
        )
    }

    companion object {

        fun newInstance(accountId: Int, userId: Int): AllFriendsFragment {
            val args = Bundle()
            args.putInt(Extra.USER_ID, userId)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val allFriendsFragment = AllFriendsFragment()
            allFriendsFragment.arguments = args
            return allFriendsFragment
        }
    }
}