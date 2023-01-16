package dev.ragnarok.fenrir.fragment.newsfeedcomments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.NewsfeedComment
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.is600dp
import dev.ragnarok.fenrir.util.Utils.isLandscape
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class NewsfeedCommentsFragment :
    PlaceSupportMvpFragment<NewsfeedCommentsPresenter, INewsfeedCommentsView>(),
    INewsfeedCommentsView, NewsfeedCommentsAdapter.ActionListener {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: NewsfeedCommentsAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_newsfeed_comments, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val manager: RecyclerView.LayoutManager = if (is600dp(requireActivity())) {
            StaggeredGridLayoutManager(
                if (isLandscape(requireActivity())) 2 else 1,
                StaggeredGridLayoutManager.VERTICAL
            )
        } else {
            LinearLayoutManager(requireActivity())
        }
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = NewsfeedCommentsAdapter(requireActivity(), emptyList(), this)
        mAdapter?.setActionListener(this)
        mAdapter?.setOwnerClickListener(this)
        recyclerView.adapter = mAdapter
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<NewsfeedCommentsPresenter> {
        return object : IPresenterFactory<NewsfeedCommentsPresenter> {
            override fun create(): NewsfeedCommentsPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                return NewsfeedCommentsPresenter(accountId, saveInstanceState)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.NEWSFEED_COMMENTS)
        setToolbarTitle(this, R.string.drawer_newsfeed_comments)
        setToolbarSubtitle(this, null)
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_NEWSFEED_COMMENTS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayData(data: List<NewsfeedComment>) {
        mAdapter?.setData(data)
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun showLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    override fun onPostBodyClick(comment: NewsfeedComment) {
        presenter?.firePostClick(
            (comment.getModel() as Post)
        )
    }

    override fun onCommentBodyClick(comment: NewsfeedComment) {
        presenter?.fireCommentBodyClick(
            comment
        )
    }

    companion object {
        fun newInstance(accountId: Long): NewsfeedCommentsFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = NewsfeedCommentsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}