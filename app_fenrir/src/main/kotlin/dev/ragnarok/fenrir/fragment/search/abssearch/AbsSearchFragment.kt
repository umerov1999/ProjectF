package dev.ragnarok.fenrir.fragment.search.abssearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.search.filteredit.FilterEditFragment
import dev.ragnarok.fenrir.fragment.search.options.BaseOption
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

abstract class AbsSearchFragment<P : AbsSearchPresenter<V, *, T, *>, V : IBaseSearchView<T>, T, A : RecyclerView.Adapter<*>> :
    PlaceSupportMvpFragment<P, V>(), IBaseSearchView<T> {
    var mAdapter: A? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mEmptyText: TextView? = null
    protected var recyclerView: RecyclerView? = null
    private fun onSearchOptionsChanged() {
        presenter?.fireOptionsChanged()
    }

    open fun createViewLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = createViewLayout(inflater, container)
        recyclerView = root.findViewById(R.id.list)
        val manager = createLayoutManager()
        recyclerView?.layoutManager = manager
        recyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = createAdapter(mutableListOf())
        recyclerView?.adapter = mAdapter
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmptyText = root.findViewById(R.id.empty)
        mEmptyText?.setText(emptyText)
        postCreate(root)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            FilterEditFragment.REQUEST_FILTER_EDIT,
            this
        ) { _: String?, _: Bundle? -> onSearchOptionsChanged() }
    }

    @get:StringRes
    val emptyText: Int
        get() = R.string.list_is_empty

    fun fireTextQueryEdit(q: String?) {
        presenter?.fireTextQueryEdit(q)
    }

    override fun displayData(data: MutableList<T>) {
        mAdapter?.let {
            setAdapterData(it, data)
        }
    }

    override fun notifyItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index)
    }

    override fun setEmptyTextVisible(visible: Boolean) {
        mEmptyText?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun showLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    fun openSearchFilter() {
        presenter?.fireOpenFilterClick()
    }

    override fun displayFilter(accountId: Int, options: ArrayList<BaseOption>) {
        val fragment = FilterEditFragment.newInstance(accountId, options)
        fragment.show(parentFragmentManager, "filter-edit")
    }

    abstract fun setAdapterData(adapter: A, data: MutableList<T>)
    abstract fun postCreate(root: View)
    abstract fun createAdapter(data: MutableList<T>): A
    abstract fun createLayoutManager(): RecyclerView.LayoutManager
}