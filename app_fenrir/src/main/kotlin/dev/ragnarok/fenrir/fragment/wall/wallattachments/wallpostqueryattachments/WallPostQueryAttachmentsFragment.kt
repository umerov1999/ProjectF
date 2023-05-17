package dev.ragnarok.fenrir.fragment.wall.wallattachments.wallpostqueryattachments

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
import dev.ragnarok.fenrir.fragment.wall.WallAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostEditor
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.view.MySearchView

class WallPostQueryAttachmentsFragment :
    PlaceSupportMvpFragment<WallPostQueryAttachmentsPresenter, IWallPostQueryAttachmentsView>(),
    IWallPostQueryAttachmentsView, WallAdapter.ClickListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: WallAdapter? = null
    private var mLoadMore: FloatingActionButton? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_wall_attachments_query, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.empty)
        mLoadMore = root.findViewById(R.id.goto_button)
        val mySearchView: MySearchView = root.findViewById(R.id.searchview)
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    query,
                    false
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    newText,
                    true
                )
                return false
            }
        })
        val recyclerView: RecyclerView = root.findViewById(android.R.id.list)
        recyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
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
        mAdapter = WallAdapter(requireActivity(), mutableListOf(), this, this)
        recyclerView.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    private fun resolveEmptyText() {
        mEmpty?.visibility =
            if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun displayData(posts: MutableList<Post>) {
        mAdapter?.setItems(posts)
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

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<WallPostQueryAttachmentsPresenter> {
        return object : IPresenterFactory<WallPostQueryAttachmentsPresenter> {
            override fun create(): WallPostQueryAttachmentsPresenter {
                return WallPostQueryAttachmentsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
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

    override fun onAvatarClick(ownerId: Long) {
        onOwnerClick(ownerId)
    }

    override fun onShareClick(post: Post) {
        presenter?.fireShareClick(
            post
        )
    }

    override fun onPostClick(post: Post) {
        presenter?.firePostBodyClick(
            post
        )
    }

    override fun onRestoreClick(post: Post) {
        presenter?.firePostRestoreClick(
            post
        )
    }

    override fun onCommentsClick(post: Post) {
        presenter?.fireCommentsClick(
            post
        )
    }

    override fun onLikeLongClick(post: Post) {
        presenter?.fireLikeLongClick(
            post
        )
    }

    override fun onShareLongClick(post: Post) {
        presenter?.fireShareLongClick(
            post
        )
    }

    override fun onLikeClick(post: Post) {
        presenter?.fireLikeClick(
            post
        )
    }

    override fun openPostEditor(accountId: Long, post: Post) {
        goToPostEditor(requireActivity(), accountId, post)
    }

    companion object {
        fun newInstance(accountId: Long, ownerId: Long): WallPostQueryAttachmentsFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            val fragment = WallPostQueryAttachmentsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}