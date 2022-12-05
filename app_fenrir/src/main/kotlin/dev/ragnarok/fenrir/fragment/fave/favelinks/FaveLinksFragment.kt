package dev.ragnarok.fenrir.fragment.fave.favelinks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.link.LinkHelper.openLinkInBrowser
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.FaveLink
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class FaveLinksFragment : BaseMvpFragment<FaveLinksPresenter, IFaveLinksView>(), IFaveLinksView,
    FaveLinksAdapter.ClickListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: FaveLinksAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_fave_links, container, false)
        val recyclerView: RecyclerView = root.findViewById(android.R.id.list)
        mEmpty = root.findViewById(R.id.empty)
        val manager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        val add: FloatingActionButton = root.findViewById(R.id.add_button)
        add.setOnClickListener {
            presenter?.fireAdd(
                requireActivity()
            )
        }
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        mAdapter = FaveLinksAdapter(emptyList(), requireActivity())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    private fun resolveEmptyText() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun onLinkClick(index: Int, link: FaveLink) {
        presenter?.fireLinkClick(
            link
        )
    }

    override fun openLink(accountId: Int, link: FaveLink) {
        openLinkInBrowser(requireActivity(), link.url)
    }

    override fun notifyItemRemoved(index: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRemoved(index)
            resolveEmptyText()
        }
    }

    override fun onLinkDelete(index: Int, link: FaveLink) {
        presenter?.fireDeleteClick(
            link
        )
    }

    override fun displayLinks(links: List<FaveLink>) {
        if (mAdapter != null) {
            mAdapter?.setData(links)
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
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyText()
        }
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FaveLinksPresenter> {
        return object : IPresenterFactory<FaveLinksPresenter> {
            override fun create(): FaveLinksPresenter {
                return FaveLinksPresenter(
                    requireArguments().getInt(
                        Extra.ACCOUNT_ID
                    ), saveInstanceState
                )
            }
        }
    }

    companion object {

        fun newInstance(accountId: Int): FaveLinksFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val fragment = FaveLinksFragment()
            fragment.arguments = args
            return fragment
        }
    }
}