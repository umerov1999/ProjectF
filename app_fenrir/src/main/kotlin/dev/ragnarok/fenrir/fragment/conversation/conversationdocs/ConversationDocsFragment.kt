package dev.ragnarok.fenrir.fragment.conversation.conversationdocs

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.conversation.abschatattachments.AbsChatAttachmentsFragment
import dev.ragnarok.fenrir.fragment.docs.DocsAdapter
import dev.ragnarok.fenrir.model.Document

class ConversationDocsFragment :
    AbsChatAttachmentsFragment<Document, ChatAttachmentDocsPresenter, IChatAttachmentDocsView>(),
    DocsAdapter.ActionListener, IChatAttachmentDocsView {
    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
    }

    override fun createAdapter(): RecyclerView.Adapter<*> {
        val simpleDocRecycleAdapter = DocsAdapter(mutableListOf())
        simpleDocRecycleAdapter.setActionListener(this)
        return simpleDocRecycleAdapter
    }

    override fun displayAttachments(data: MutableList<Document>) {
        if (adapter is DocsAdapter) {
            (adapter as DocsAdapter).setItems(data)
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatAttachmentDocsPresenter> {
        return object : IPresenterFactory<ChatAttachmentDocsPresenter> {
            override fun create(): ChatAttachmentDocsPresenter {
                return ChatAttachmentDocsPresenter(
                    requireArguments().getLong(Extra.PEER_ID),
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onDocClick(index: Int, doc: Document) {
        presenter?.fireDocClick(
            doc
        )
    }

    override fun onDocLongClick(index: Int, doc: Document): Boolean {
        presenter?.fireGoToMessagesLookup(
            doc.msgPeerId,
            doc.msgId
        )
        return true
    }
}