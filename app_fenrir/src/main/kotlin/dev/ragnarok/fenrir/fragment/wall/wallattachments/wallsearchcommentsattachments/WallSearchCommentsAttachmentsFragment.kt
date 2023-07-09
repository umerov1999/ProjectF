package dev.ragnarok.fenrir.fragment.wall.wallattachments.wallsearchcommentsattachments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.comments.CommentsAdapter
import dev.ragnarok.fenrir.fragment.comments.CommentsAdapter.OnCommentActionListener
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.place.PlaceFactory.getPostPreviewPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars

class WallSearchCommentsAttachmentsFragment :
    PlaceSupportMvpFragment<WallSearchCommentsAttachmentsPresenter, IWallSearchCommentsAttachmentsView>(),
    IWallSearchCommentsAttachmentsView, OnCommentActionListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: CommentsAdapter? = null
    private var mLoadMore: FloatingActionButton? = null
    private var recyclerView: RecyclerView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_wall_attachments, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.empty)
        mLoadMore = root.findViewById(R.id.goto_button)
        recyclerView = root.findViewById(R.id.content_list)
        recyclerView?.layoutManager = LinearLayoutManager(
            requireActivity(),
            RecyclerView.VERTICAL,
            false
        )
        recyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd(false)
            }
        })
        mLoadMore?.setOnClickListener {
            presenter?.fireScrollToEnd(true)
        }
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            mSwipeRefreshLayout?.isRefreshing = false
            CustomSnackbars.createCustomSnackbars(view, null, true)
                ?.setDurationSnack(Snackbar.LENGTH_LONG)?.defaultSnack(R.string.do_update)
                ?.setAction(R.string.button_yes) {
                    presenter?.fireRefresh()
                }?.show()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = CommentsAdapter(requireActivity(), mutableListOf(), this)
        mAdapter?.setListener(this)
        recyclerView?.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    private fun resolveEmptyText() {
        mEmpty?.visibility =
            if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun displayData(comments: MutableList<Comment>) {
        mAdapter?.setItems(comments)
        resolveEmptyText()
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
        resolveEmptyText()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
        resolveEmptyText()
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<WallSearchCommentsAttachmentsPresenter> {
        return object : IPresenterFactory<WallSearchCommentsAttachmentsPresenter> {
            override fun create(): WallSearchCommentsAttachmentsPresenter {
                return WallSearchCommentsAttachmentsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    requireArguments().getIntegerArrayList(Extra.POST_ID)!!,
                    saveInstanceState
                )
            }
        }
    }

    override fun toolbarTitle(title: String) {
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.title = title
        }
    }

    override fun toolbarSubtitle(subtitle: String) {
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.subtitle = subtitle
        }
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onSetLoadingStatus(isLoad: Int) {
        when (isLoad) {
            1 -> mLoadMore?.setImageResource(R.drawable.audio_died)
            2 -> mLoadMore?.setImageResource(R.drawable.view)
            else -> mLoadMore?.setImageResource(R.drawable.ic_arrow_down)
        }
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index + (mAdapter?.headersCount ?: 0))
    }

    override fun goToPost(accountId: Long, ownerId: Long, postId: Int) {
        getPostPreviewPlace(accountId, postId, ownerId).tryOpenWith(requireActivity())
    }

    override fun moveFocusTo(index: Int) {
        val adapterPosition = index + (mAdapter?.headersCount ?: 0)
        recyclerView?.smoothScrollToPosition(adapterPosition)
    }

    override fun onReplyToOwnerClick(ownerId: Long, commentId: Int) {
        presenter?.fireReplyToOwnerClick(
            commentId
        )
    }

    override fun onRestoreComment(commentId: Int) {}
    override fun onAvatarClick(ownerId: Long) {
        onOwnerClick(ownerId)
    }

    override fun onCommentLikeClick(comment: Comment, add: Boolean) {
        presenter?.fireWhoLikesClick(
            comment
        )
    }

    override fun populateCommentContextMenu(comment: Comment) {
        presenter?.fireGoCommentPostClick(
            comment
        )
    }

    companion object {
        fun buildArgs(accountId: Long, ownerId: Long, posts: ArrayList<Int>): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putIntegerArrayList(Extra.POST_ID, posts)
            return args
        }

        fun newInstance(args: Bundle?): WallSearchCommentsAttachmentsFragment {
            val fragment = WallSearchCommentsAttachmentsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}