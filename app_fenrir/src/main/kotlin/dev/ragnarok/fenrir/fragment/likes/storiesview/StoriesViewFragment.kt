package dev.ragnarok.fenrir.fragment.likes.storiesview

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
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.util.ViewUtils

class StoriesViewFragment : BaseMvpFragment<StoriesViewPresenter, IStoriesViewView>(),
    IStoriesViewView {
    private var mRecyclerView: RecyclerView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mOwnersAdapter: StoriesViewAdapter? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mCount: MaterialTextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(
            R.layout.fragment_abs_friends_with_toolbar,
            container,
            false
        )
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mRecyclerView = root.findViewById(R.id.list)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mCount = root.findViewById(R.id.count_data)
        mCount?.visibility = View.VISIBLE
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mLinearLayoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        mRecyclerView?.layoutManager = mLinearLayoutManager
        mRecyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        mRecyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mOwnersAdapter = StoriesViewAdapter(requireActivity(), emptyList())
        mOwnersAdapter?.setClickListener(object : StoriesViewAdapter.ClickListener {
            override fun onOwnerClick(owner: Owner) {
                presenter?.fireOwnerClick(owner)
            }
        })
        mRecyclerView?.adapter = mOwnersAdapter
        return root
    }

    override fun displayOwnerList(owners: List<Pair<Owner, Boolean>>) {
        if (mOwnersAdapter != null) {
            mOwnersAdapter?.setItems(owners)
            mCount?.text = getString(R.string.people_count, owners.size)
        }
    }

    override fun updateTitle(@StringRes res: Int) {
        val actionBar = supportToolbarFor(this)
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
        PlaceFactory.getOwnerWallPlace(accountId, owner).tryOpenWith(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.views)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<StoriesViewPresenter> {
        return object : IPresenterFactory<StoriesViewPresenter> {
            override fun create(): StoriesViewPresenter {
                return StoriesViewPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    requireArguments().getInt(Extra.ITEM_ID),
                    saveInstanceState
                )
            }
        }
    }

    companion object {
        fun buildArgs(
            accountId: Long,
            ownerId: Long,
            itemId: Int
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putInt(Extra.ITEM_ID, itemId)
            return args
        }

        fun newInstance(args: Bundle): StoriesViewFragment {
            val fragment = StoriesViewFragment()
            fragment.arguments = args
            return fragment
        }
    }
}