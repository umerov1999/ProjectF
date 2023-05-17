package dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationdocs

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.abschatattachments.BaseChatAttachmentsPresenter
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single

class ChatAttachmentDocsPresenter(peerId: Long, accountId: Long, savedInstanceState: Bundle?) :
    BaseChatAttachmentsPresenter<Document, IChatAttachmentDocsView>(
        peerId,
        accountId,
        savedInstanceState
    ) {
    override fun onDataChanged() {
        super.onDataChanged()
        resolveToolbar()
    }

    override fun requestAttachments(
        peerId: Long,
        nextFrom: String?
    ): Single<Pair<String?, List<Document>>> {
        return get().vkDefault(accountId)
            .messages()
            .getHistoryAttachments(peerId, VKApiAttachment.TYPE_DOC, nextFrom, 1, 1, 45, 50, null)
            .map { response ->
                val docs: MutableList<Document> = ArrayList(
                    Utils.safeCountOf(response.items)
                )
                response.items.nonNullNoEmpty {
                    for (one in it) {
                        if (one.entry != null && one.entry?.attachment is VKApiDoc) {
                            val dto = one.entry?.attachment as VKApiDoc
                            docs.add(
                                Dto2Model.transform(dto).setMsgId(one.messageId)
                                    .setMsgPeerId(peerId)
                            )
                        }
                    }
                }
                create(response.next_from, docs)
            }
    }

    override fun onGuiCreated(viewHost: IChatAttachmentDocsView) {
        super.onGuiCreated(viewHost)
        resolveToolbar()
    }

    private fun resolveToolbar() {
        view?.setToolbarTitleString(getString(R.string.attachments_in_chat))
        view?.setToolbarTitleString(getString(R.string.documents_count, Utils.safeCountOf(data)))
    }
}