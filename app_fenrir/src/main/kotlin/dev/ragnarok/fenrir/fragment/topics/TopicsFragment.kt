package dev.ragnarok.fenrir.fragment.topics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Topic
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper.Companion.createFrom

class TopicsFragment : BaseMvpFragment<TopicsPresenter, ITopicsView>(),
    SwipeRefreshLayout.OnRefreshListener, ITopicsView, TopicsAdapter.ActionListener {
    private var mAdapter: TopicsAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var helper: LoadMoreFooterHelper? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_topics, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val manager: RecyclerView.LayoutManager = LinearLayoutManager(requireActivity())
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = TopicsAdapter(requireActivity(), mutableListOf(), this)
        val footer = inflater.inflate(R.layout.footer_load_more, recyclerView, false)
        helper = createFrom(footer, object : LoadMoreFooterHelper.Callback {
            override fun onLoadMoreClick() {
                presenter?.fireLoadMoreClick()
            }
        })
        mAdapter?.addFooter(footer)
        recyclerView.adapter = mAdapter
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.topics)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(topics: MutableList<Topic>) {
        mAdapter?.setItems(topics)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdd(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun setupLoadMore(@LoadMoreState state: Int) {
        helper?.switchToState(state)
    }

    override fun goToComments(accountId: Long, topic: Topic) {
        getCommentsPlace(accountId, Commented.from(topic), null)
            .tryOpenWith(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<TopicsPresenter> {
        return object : IPresenterFactory<TopicsPresenter> {
            override fun create(): TopicsPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                return TopicsPresenter(accountId, ownerId, saveInstanceState)
            }
        }
    }

    override fun onTopicClick(topic: Topic) {
        presenter?.fireTopicClick(
            topic
        )
    }

    companion object {
        fun buildArgs(accountId: Long, ownerId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            return args
        }

        fun newInstance(args: Bundle?): TopicsFragment {
            val fragment = TopicsFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(accountId: Long, ownerId: Long): TopicsFragment {
            return newInstance(buildArgs(accountId, ownerId))
        }
    }
}