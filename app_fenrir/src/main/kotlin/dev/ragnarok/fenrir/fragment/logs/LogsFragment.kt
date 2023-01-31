package dev.ragnarok.fenrir.fragment.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalOptionsAdapter
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.LogEventType
import dev.ragnarok.fenrir.model.LogEventWrapper
import dev.ragnarok.fenrir.util.Utils.shareLink
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

class LogsFragment : BaseMvpFragment<LogsPresenter, ILogsView>(), ILogsView,
    HorizontalOptionsAdapter.Listener<LogEventType>, LogsAdapter.ActionListener, MenuProvider {
    private var mTypesAdapter: HorizontalOptionsAdapter<LogEventType>? = null
    private var mLogsAdapter: LogsAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mEmptyText: TextView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_logs, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.events_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        val headerView = inflater.inflate(R.layout.header_logs, recyclerView, false)
        val typesRecyclerView: RecyclerView = headerView.findViewById(R.id.types_recycler_view)
        typesRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mTypesAdapter = HorizontalOptionsAdapter(mutableListOf())
        mTypesAdapter?.setListener(this)
        typesRecyclerView.adapter = mTypesAdapter
        mLogsAdapter = LogsAdapter(mutableListOf(), this)
        mLogsAdapter?.addHeader(headerView)
        recyclerView.adapter = mLogsAdapter
        mEmptyText = root.findViewById(R.id.empty_text)
        return root
    }

    override fun displayTypes(types: MutableList<LogEventType>) {
        mTypesAdapter?.setItems(types)
    }

    override fun displayData(events: MutableList<LogEventWrapper>) {
        mLogsAdapter?.setItems(events)
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.application_logs)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun notifyEventDataChanged() {
        mLogsAdapter?.notifyDataSetChanged()
    }

    override fun notifyTypesDataChanged() {
        mTypesAdapter?.notifyDataSetChanged()
    }

    override fun setEmptyTextVisible(visible: Boolean) {
        mEmptyText?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LogsPresenter> {
        return object : IPresenterFactory<LogsPresenter> {
            override fun create(): LogsPresenter {
                return LogsPresenter(saveInstanceState)
            }
        }
    }

    override fun onOptionClick(entry: LogEventType) {
        presenter?.fireTypeClick(
            entry
        )
    }

    override fun onShareClick(wrapper: LogEventWrapper) {
        val event = wrapper.getEvent()
        shareLink(requireActivity(), event?.body, event?.tag)
    }

    override fun onCopyClick(wrapper: LogEventWrapper) {
        val event = wrapper.getEvent()
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText(event?.tag, event?.body)
        clipboard?.setPrimaryClip(clip)
        createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.delete_menu) {
            presenter?.fireClear()
            return true
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.logs_menu, menu)
    }

    companion object {
        fun newInstance(): LogsFragment {
            val args = Bundle()
            val fragment = LogsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}