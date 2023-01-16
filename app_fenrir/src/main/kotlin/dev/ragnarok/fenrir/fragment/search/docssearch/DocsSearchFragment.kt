package dev.ragnarok.fenrir.fragment.search.docssearch

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.docs.DocsAdapter
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Document

class DocsSearchFragment :
    AbsSearchFragment<DocsSearchPresenter, IDocSearchView, Document, DocsAdapter>(),
    DocsAdapter.ActionListener, IDocSearchView {
    override fun setAdapterData(adapter: DocsAdapter, data: MutableList<Document>) {
        adapter.setItems(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Document>): DocsAdapter {
        val adapter = DocsAdapter(data)
        adapter.setActionListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
    }

    override fun onDocClick(index: Int, doc: Document) {
        presenter?.fireDocClick(
            doc
        )
    }

    override fun onDocLongClick(index: Int, doc: Document): Boolean {
        return false
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<DocsSearchPresenter> {
        return object : IPresenterFactory<DocsSearchPresenter> {
            override fun create(): DocsSearchPresenter {
                return DocsSearchPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.CRITERIA),
                    saveInstanceState
                )
            }
        }
    }

    companion object {

        fun newInstance(
            accountId: Long,
            initialCriteria: DocumentSearchCriteria?
        ): DocsSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = DocsSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}