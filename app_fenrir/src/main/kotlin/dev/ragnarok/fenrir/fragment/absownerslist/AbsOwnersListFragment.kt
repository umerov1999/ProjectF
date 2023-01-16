package dev.ragnarok.fenrir.fragment.absownerslist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

abstract class AbsOwnersListFragment<P : SimpleOwnersPresenter<V>, V : ISimpleOwnersView> :
    BaseMvpFragment<P, V>(), ISimpleOwnersView {
    protected var mRecyclerView: RecyclerView? = null
    protected var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    protected var mOwnersAdapter: OwnersAdapter? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mCount: MaterialTextView? = null
    protected abstract fun hasToolbar(): Boolean
    protected abstract fun needShowCount(): Boolean
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(
            if (hasToolbar()) R.layout.fragment_abs_friends_with_toolbar else R.layout.fragment_abs_friends,
            container,
            false
        )
        if (hasToolbar()) {
            (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        }
        mRecyclerView = root.findViewById(R.id.list)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mCount = root.findViewById(R.id.count_data)
        mCount?.visibility = if (needShowCount()) View.VISIBLE else View.GONE
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mLinearLayoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        mRecyclerView?.layoutManager = mLinearLayoutManager
        mRecyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        mRecyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mOwnersAdapter = OwnersAdapter(requireActivity(), emptyList())
        mOwnersAdapter?.setClickListener(object : OwnersAdapter.ClickListener {
            override fun onOwnerClick(owner: Owner) {
                presenter?.fireOwnerClick(owner)
            }
        })
        mOwnersAdapter?.setLongClickListener(object : OwnersAdapter.LongClickListener {
            override fun onOwnerLongClick(owner: Owner): Boolean {
                return onLongClick(
                    owner
                )
            }
        })
        mRecyclerView?.adapter = mOwnersAdapter
        return root
    }

    protected open fun onLongClick(owner: Owner): Boolean {
        return false
    }

    override fun displayOwnerList(owners: List<Owner>) {
        if (mOwnersAdapter != null) {
            mOwnersAdapter?.setItems(owners)
            mCount?.text = getString(R.string.people_count, owners.size)
        }
    }

    override fun updateTitle(@StringRes res: Int) {
        val actionBar = ActivityUtils.supportToolbarFor(this)
        actionBar?.setTitle(res)
    }

    override fun notifyDataSetChanged() {
        if (mOwnersAdapter != null) {
            mOwnersAdapter?.notifyDataSetChanged()
            mCount?.text = getString(R.string.people_count, mOwnersAdapter?.itemCount ?: 0)
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mOwnersAdapter != null) {
            mOwnersAdapter?.notifyItemRangeInserted(position, count)
            mCount?.text = getString(R.string.people_count, mOwnersAdapter?.itemCount ?: 0)
        }
    }

    override fun notifyDataRemoved(position: Int, count: Int) {
        if (mOwnersAdapter != null) {
            mOwnersAdapter?.notifyItemRangeRemoved(position, count)
            mCount?.text = getString(R.string.people_count, mOwnersAdapter?.itemCount ?: 0)
        }
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun showOwnerWall(accountId: Long, owner: Owner) {
        getOwnerWallPlace(accountId, owner).tryOpenWith(requireActivity())
    }
}