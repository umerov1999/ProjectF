package dev.ragnarok.fenrir.fragment.fave.faveposts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.util.Utils.is600dp
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class FavePostsFragment : PlaceSupportMvpFragment<FavePostsPresenter, IFavePostsView>(),
    FavePostAdapter.ClickListener, IFavePostsView {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: FavePostAdapter? = null
    private var mEmpty: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_fave_posts, container, false) as ViewGroup
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmpty = root.findViewById(R.id.empty)
        val manager: RecyclerView.LayoutManager = if (is600dp(requireActivity())) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        }
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = FavePostAdapter(requireActivity(), mutableListOf(), this, this)
        recyclerView.adapter = mAdapter
        return root
    }

    private fun resolveEmptyText() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onPollOpen(poll: Poll) {
        presenter?.firePollClick(
            poll
        )
    }

    override fun onAvatarClick(ownerId: Int) {
        onOpenOwner(ownerId)
    }

    override fun onShareClick(post: Post) {
        presenter?.fireShareClick(
            post
        )
    }

    override fun onPostClick(post: Post) {
        presenter?.firePostClick(
            post
        )
    }

    override fun onCommentsClick(post: Post) {
        presenter?.fireCommentsClick(
            post
        )
    }

    override fun onLikeLongClick(post: Post) {
        presenter?.fireCopiesLikesClick(
            "post",
            post.ownerId,
            post.vkid,
            ILikesInteractor.FILTER_LIKES
        )
    }

    override fun onShareLongClick(post: Post) {
        presenter?.fireCopiesLikesClick(
            "post",
            post.ownerId,
            post.vkid,
            ILikesInteractor.FILTER_COPIES
        )
    }

    override fun onLikeClick(post: Post) {
        presenter?.fireLikeClick(
            post
        )
    }

    override fun onDelete(index: Int, post: Post) {
        presenter?.firePostDelete(
            index,
            post
        )
    }

    override fun displayData(posts: MutableList<Post>) {
        if (mAdapter != null) {
            mAdapter?.setItems(posts)
            resolveEmptyText()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyText()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position + (mAdapter?.headersCount ?: 0), count)
            resolveEmptyText()
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index + (mAdapter?.headersCount ?: 0))
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FavePostsPresenter> {
        return object : IPresenterFactory<FavePostsPresenter> {
            override fun create(): FavePostsPresenter {
                return FavePostsPresenter(
                    requireArguments().getInt(
                        Extra.ACCOUNT_ID
                    ), saveInstanceState
                )
            }
        }
    }

    companion object {
        fun newInstance(accountId: Int): FavePostsFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val favePostsFragment = FavePostsFragment()
            favePostsFragment.arguments = args
            return favePostsFragment
        }
    }
}