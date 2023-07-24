package dev.ragnarok.fenrir.fragment.messages.conversationattachments.abschatattachments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationphotos.ConversationPhotosFragment
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

abstract class AbsChatAttachmentsFragment<T, P : BaseChatAttachmentsPresenter<T, V>, V : IBaseChatAttachmentsView<T>> :
    PlaceSupportMvpFragment<P, V>(), IBaseChatAttachmentsView<T> {
    protected var mRecyclerView: RecyclerView? = null
    protected var mEmpty: TextView? = null
    protected var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    protected var adapter: RecyclerView.Adapter<*>? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_photos, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mRecyclerView = root.findViewById(R.id.content_list)
        mEmpty = root.findViewById(R.id.empty)
        val manager = createLayoutManager()
        mRecyclerView?.layoutManager = manager
        PicassoPauseOnScrollListener.addListener(mRecyclerView)
        mRecyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        adapter = createAdapter()
        mRecyclerView?.adapter = adapter
        return root
    }

    protected abstract fun createLayoutManager(): RecyclerView.LayoutManager
    abstract fun createAdapter(): RecyclerView.Adapter<*>?
    override fun notifyDataAdded(position: Int, count: Int) {
        adapter?.notifyItemRangeInserted(position, count)
    }

    override fun notifyDatasetChanged() {
        adapter?.notifyDataSetChanged()
    }

    override fun showLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    override fun setEmptyTextVisible(visible: Boolean) {
        mEmpty?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setToolbarTitleString(title: String) {
        supportToolbarFor(this)?.title = title
    }

    override fun setToolbarSubtitleString(subtitle: String) {
        supportToolbarFor(this)?.subtitle = subtitle
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

    companion object {
        val TAG: String = ConversationPhotosFragment::class.java.simpleName
    }
}