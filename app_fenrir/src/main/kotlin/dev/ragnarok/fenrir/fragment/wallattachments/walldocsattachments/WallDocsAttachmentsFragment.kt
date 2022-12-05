package dev.ragnarok.fenrir.fragment.wallattachments.walldocsattachments

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
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.docs.DocsAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class WallDocsAttachmentsFragment :
    PlaceSupportMvpFragment<WallDocsAttachmentsPresenter, IWallDocsAttachmentsView>(),
    IWallDocsAttachmentsView, DocsAdapter.ActionListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: DocsAdapter? = null
    private var mLoadMore: FloatingActionButton? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_wall_attachments, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.empty)
        mLoadMore = root.findViewById(R.id.goto_button)
        val recyclerView: RecyclerView = root.findViewById(android.R.id.list)
        recyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mLoadMore?.setOnClickListener {
            presenter?.fireScrollToEnd()
        }
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = DocsAdapter(mutableListOf())
        mAdapter?.setActionListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    private fun resolveEmptyText() {
        mEmpty?.visibility =
            if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun displayData(documents: MutableList<Document>) {
        mAdapter?.setItems(documents)
        resolveEmptyText()
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
        resolveEmptyText()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            (mAdapter ?: return).notifyItemRangeInserted(position, count)
            resolveEmptyText()
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        if (mSwipeRefreshLayout != null) {
            (mSwipeRefreshLayout ?: return).isRefreshing = refreshing
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<WallDocsAttachmentsPresenter> {
        return object : IPresenterFactory<WallDocsAttachmentsPresenter> {
            override fun create(): WallDocsAttachmentsPresenter {
                return WallDocsAttachmentsPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getInt(Extra.OWNER_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun toolbarTitle(title: String) {
        supportToolbarFor(this)?.title = title
    }

    override fun toolbarSubtitle(subtitle: String) {
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

    override fun onSetLoadingStatus(isLoad: Int) {
        when (isLoad) {
            1 -> mLoadMore?.setImageResource(R.drawable.audio_died)
            2 -> mLoadMore?.setImageResource(R.drawable.view)
            else -> mLoadMore?.setImageResource(R.drawable.ic_arrow_down)
        }
    }

    override fun onDocClick(index: Int, doc: Document) {
        presenter?.fireDocClick(
            doc
        )
    }

    override fun onDocLongClick(index: Int, doc: Document): Boolean {
        return false
    }

    companion object {
        fun newInstance(accountId: Int, ownerId: Int): WallDocsAttachmentsFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.OWNER_ID, ownerId)
            val fragment = WallDocsAttachmentsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}