package dev.ragnarok.fenrir.fragment.friends.requests

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
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.DeltaOwnerActivity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.friends.allfriends.FriendsRecycleAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.DeltaOwner
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UsersPart
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.MySearchView

class RequestsFragment : BaseMvpFragment<RequestsPresenter, IRequestsView>(),
    FriendsRecycleAdapter.Listener, IRequestsView {
    private var mAdapter: FriendsRecycleAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mCount: MaterialTextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        val root = inflater.inflate(R.layout.fragment_requests, container, false)
        val mRecyclerView: RecyclerView = root.findViewById(R.id.list)
        mCount = root.findViewById(R.id.count_data)
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

    override fun updateCount(count: Int) {
        if (count <= 0) {
            mCount?.visibility = View.GONE
        } else {
            mCount?.visibility = View.VISIBLE
        }
        mCount?.text = getString(R.string.people_count, count)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<RequestsPresenter> {
        return object : IPresenterFactory<RequestsPresenter> {
            override fun create(): RequestsPresenter {
                return RequestsPresenter(
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

    override fun showNotRequests(data: List<Owner>, accountId: Int, ownerId: Int) {
        if (data.isEmpty()) {
            return
        }
        DeltaOwnerActivity.showDeltaActivity(
            requireActivity(),
            accountId,
            DeltaOwner().setOwner(ownerId).appendToList(
                requireActivity(),
                R.string.not_request,
                data
            )
        )
    }

    override fun onUserClick(user: User) {
        presenter?.fireUserClick(
            user
        )
    }

    companion object {
        fun newInstance(accountId: Int, userId: Int): RequestsFragment {
            val args = Bundle()
            args.putInt(Extra.USER_ID, userId)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val requestsFragment = RequestsFragment()
            requestsFragment.arguments = args
            return requestsFragment
        }
    }
}