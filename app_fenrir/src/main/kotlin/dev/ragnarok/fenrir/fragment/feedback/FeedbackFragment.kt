package dev.ragnarok.fenrir.fragment.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.dialog.feedbacklink.FeedbackLinkDialog
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper.Companion.createFrom
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class FeedbackFragment : PlaceSupportMvpFragment<FeedbackPresenter, IFeedbackView>(),
    SwipeRefreshLayout.OnRefreshListener, IFeedbackView, FeedbackAdapter.ClickListener {
    private var mAdapter: FeedbackAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mEmptyText: TextView? = null
    private var mLoadMoreHelper: LoadMoreFooterHelper? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_feedback, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmptyText = root.findViewById(R.id.fragment_feedback_empty_text)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val manager: RecyclerView.LayoutManager = LinearLayoutManager(requireActivity())
        val recyclerView: RecyclerView = root.findViewById(R.id.recycleView)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToLast()
            }
        })
        val footerView = inflater.inflate(R.layout.footer_load_more, recyclerView, false)
        mLoadMoreHelper = createFrom(footerView, object : LoadMoreFooterHelper.Callback {
            override fun onLoadMoreClick() {
                presenter?.fireLoadMoreClick()
            }
        })
        mLoadMoreHelper?.switchToState(LoadMoreState.INVISIBLE)
        mAdapter = FeedbackAdapter(requireActivity(), mutableListOf(), this)
        mAdapter?.addFooter(footerView)
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.NOTIFICATIONS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.drawer_feedback)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_FEEDBACK)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayData(data: MutableList<Feedback>) {
        if (mAdapter != null) {
            mAdapter?.setItems(data)
            resolveEmptyTextVisibility()
        }
    }

    override fun showLoading(loading: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = loading }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmptyText != null && mAdapter != null) {
            mEmptyText?.visibility =
                if (mAdapter?.realItemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun notifyDataAdding(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyFirstListReceived() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).readAllNotifications()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun configLoadMore(@LoadMoreState loadmoreState: Int) {
        mLoadMoreHelper?.switchToState(loadmoreState)
    }

    override fun showLinksDialog(accountId: Int, notification: Feedback) {
        FeedbackLinkDialog.newInstance(accountId, notification)
            .show(parentFragmentManager, "feedback_links")
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FeedbackPresenter> {
        return object : IPresenterFactory<FeedbackPresenter> {
            override fun create(): FeedbackPresenter {
                return FeedbackPresenter(
                    requireArguments().getInt(
                        Extra.ACCOUNT_ID
                    ), saveInstanceState
                )
            }
        }
    }

    override fun onNotificationClick(notification: Feedback) {
        presenter?.fireItemClick(
            notification
        )
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    companion object {
        fun buildArgs(accountId: Int): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            return args
        }

        fun newInstance(accountId: Int): FeedbackFragment {
            return newInstance(buildArgs(accountId))
        }

        fun newInstance(args: Bundle?): FeedbackFragment {
            val feedsFragment = FeedbackFragment()
            feedsFragment.arguments = args
            return feedsFragment
        }
    }
}