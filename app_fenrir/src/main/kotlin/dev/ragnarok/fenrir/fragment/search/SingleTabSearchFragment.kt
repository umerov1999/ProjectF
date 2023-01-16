package dev.ragnarok.fenrir.fragment.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.fragment.search.SearchFragmentFactory.create
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.BaseSearchCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.MySearchView.OnAdditionalButtonClickListener
import dev.ragnarok.fenrir.view.MySearchView.OnBackButtonClickListener

class SingleTabSearchFragment : Fragment(), MySearchView.OnQueryTextListener,
    OnAdditionalButtonClickListener {
    @SearchContentType
    private var mContentType = 0
    private var mAccountId = 0L
    private var mInitialCriteria: BaseSearchCriteria? = null
    private var attachedChild = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContentType = requireArguments().getInt(Extra.TYPE)
        mAccountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        mInitialCriteria = requireArguments().getParcelableCompat(Extra.CRITERIA)
        if (savedInstanceState != null) {
            attachedChild = savedInstanceState.getBoolean("attachedChild")
        }
    }

    private fun resolveLeftButton(searchView: MySearchView?) {
        val count = requireActivity().supportFragmentManager.backStackEntryCount
        if (searchView != null) {
            searchView.setLeftIcon(if (count == 1 && requireActivity() is AppStyleable) R.drawable.magnify else R.drawable.arrow_left)
            searchView.setLeftIconTint(CurrentTheme.getColorPrimary(requireActivity()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_search_single, container, false)
        val searchView: MySearchView = root.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(this)
        searchView.setOnBackButtonClickListener(object : OnBackButtonClickListener {
            override fun onBackButtonClick() {
                if (requireActivity().supportFragmentManager.backStackEntryCount == 1
                    && requireActivity() is AppStyleable
                ) {
                    (requireActivity() as AppStyleable).openMenu(true)
                } else {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        searchView.setOnAdditionalButtonClickListener(this)
        searchView.setQuery(initialCriteriaText, true)
        resolveLeftButton(searchView)
        if (!attachedChild) {
            attachChildFragment()
            attachedChild = true
        }
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("attachedChild", attachedChild)
    }

    private fun fireNewQuery(query: String?) {
        val fragment = childFragmentManager.findFragmentById(R.id.child_container)

        // MVP
        if (fragment is AbsSearchFragment<*, *, *, *>) {
            fragment.fireTextQueryEdit(query)
        }
    }

    private fun attachChildFragment() {
        val fragment = create(mContentType, mAccountId, mInitialCriteria)
        childFragmentManager
            .beginTransaction()
            .replace(R.id.child_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
    }

    private val initialCriteriaText: String
        get() = mInitialCriteria?.query ?: ""

    override fun onQueryTextSubmit(query: String?): Boolean {
        fireNewQuery(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        fireNewQuery(newText)
        return false
    }

    override fun onAdditionalButtonClick() {
        val fragment = childFragmentManager.findFragmentById(R.id.child_container)
        if (fragment is AbsSearchFragment<*, *, *, *>) {
            fragment.openSearchFilter()
        }
    }

    companion object {
        fun buildArgs(
            accountId: Long,
            @SearchContentType contentType: Int,
            criteria: BaseSearchCriteria?
        ): Bundle {
            val args = Bundle()
            args.putInt(Extra.TYPE, contentType)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, criteria)
            return args
        }

        fun newInstance(args: Bundle?): SingleTabSearchFragment {
            val fragment = SingleTabSearchFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            accountId: Long,
            @SearchContentType contentType: Int
        ): SingleTabSearchFragment {
            val args = Bundle()
            args.putInt(Extra.TYPE, contentType)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = SingleTabSearchFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            accountId: Long,
            @SearchContentType contentType: Int,
            criteria: BaseSearchCriteria?
        ): SingleTabSearchFragment {
            val args = Bundle()
            args.putInt(Extra.TYPE, contentType)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, criteria)
            val fragment = SingleTabSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}