package dev.ragnarok.fenrir.fragment.shortedlinks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.ShortLink
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class ShortedLinksFragment : BaseMvpFragment<ShortedLinksPresenter, IShortedLinksView>(),
    IShortedLinksView, ShortedLinksAdapter.ClickListener {
    private var mEmpty: TextView? = null
    private var mLink: TextInputEditText? = null
    private var do_Short: MaterialButton? = null
    private var do_Validate: MaterialButton? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: ShortedLinksAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_shorted_links, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.fragment_shorted_links_empty_text)
        val manager: RecyclerView.LayoutManager = LinearLayoutManager(requireActivity())
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = manager
        PicassoPauseOnScrollListener.addListener(recyclerView)
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mLink = root.findViewById(R.id.input_url)
        do_Short = root.findViewById(R.id.do_short)
        do_Validate = root.findViewById(R.id.do_validate)
        do_Short?.isEnabled = false
        do_Validate?.isEnabled = false
        mLink?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                do_Validate?.isEnabled = !s.isNullOrEmpty()
                do_Short?.isEnabled = !s.isNullOrEmpty()
                presenter?.fireInputEdit(
                    s
                )
            }
        })
        do_Short?.setOnClickListener {
            presenter?.fireShort()
        }
        do_Validate?.setOnClickListener {
            presenter?.fireValidate()
        }
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = ShortedLinksAdapter(emptyList(), requireActivity())
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

    override fun displayData(links: List<ShortLink>) {
        if (mAdapter != null) {
            mAdapter?.setData(links)
            resolveEmptyText()
        }
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.DIALOGS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.short_link)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_DIALOGS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
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

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun updateLink(url: String?) {
        mLink?.setText(url)
        mLink?.setSelection(mLink?.text?.length ?: 0)
        do_Short?.isEnabled = false
        do_Validate?.isEnabled = false
    }

    override fun showLinkStatus(status: String?) {
        var stat = ""
        var color = Color.parseColor("#ff0000")
        when (status) {
            "not_banned" -> {
                stat = getString(R.string.link_not_banned)
                color = Color.parseColor("#cc00aa00")
            }

            "banned" -> {
                stat = getString(R.string.link_banned)
                color = Color.parseColor("#ccaa0000")
            }

            "processing" -> {
                stat = getString(R.string.link_processing)
                color = Color.parseColor("#cc0000aa")
            }
        }
        CustomSnackbars.createCustomSnackbars(mLink, view?.findViewById(R.id.recycler_view))
            ?.setDurationSnack(Snackbar.LENGTH_LONG)?.coloredSnack(stat, color)?.show()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ShortedLinksPresenter> {
        return object : IPresenterFactory<ShortedLinksPresenter> {
            override fun create(): ShortedLinksPresenter {
                return ShortedLinksPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onCopy(index: Int, link: ShortLink) {
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("response", link.short_url)
        clipboard?.setPrimaryClip(clip)
        createCustomToast(context).showToast(R.string.copied)
    }

    override fun onDelete(index: Int, link: ShortLink) {
        presenter?.fireDelete(
            index,
            link
        )
    }

    companion object {
        fun newInstance(accountId: Long): ShortedLinksFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = ShortedLinksFragment()
            fragment.arguments = args
            return fragment
        }
    }
}