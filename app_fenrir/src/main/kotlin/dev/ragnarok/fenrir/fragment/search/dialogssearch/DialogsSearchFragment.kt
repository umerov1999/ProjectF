package dev.ragnarok.fenrir.fragment.search.dialogssearch

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.DialogsSearchCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Conversation

class DialogsSearchFragment :
    AbsSearchFragment<DialogsSearchPresenter, IDialogsSearchView, Conversation, DialogPreviewAdapter>(),
    IDialogsSearchView, DialogPreviewAdapter.ActionListener {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<DialogsSearchPresenter> {
        return object : IPresenterFactory<DialogsSearchPresenter> {
            override fun create(): DialogsSearchPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val criteria: DialogsSearchCriteria? =
                    requireArguments().getParcelableCompat(Extra.CRITERIA)
                return DialogsSearchPresenter(accountId, criteria, saveInstanceState)
            }
        }
    }

    override fun setAdapterData(adapter: DialogPreviewAdapter, data: MutableList<Conversation>) {
        adapter.setData(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Conversation>): DialogPreviewAdapter {
        return DialogPreviewAdapter(data, this)
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity())
    }

    override fun onEntryClick(o: Conversation) {
        presenter?.fireEntryClick(
            o
        )
    }

    companion object {
        fun newInstance(accountId: Long, criteria: DialogsSearchCriteria?): DialogsSearchFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, criteria)
            val fragment = DialogsSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}