package dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationposts

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.api.model.VKApiLink
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.abschatattachments.BaseChatAttachmentsPresenter
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatAttachmentPostsPresenter(peerId: Long, accountId: Long, savedInstanceState: Bundle?) :
    BaseChatAttachmentsPresenter<Link, IChatAttachmentPostsView>(
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
    ): Flow<Pair<String?, List<Link>>> {
        return get().vkDefault(accountId)
            .messages()
            .getHistoryAttachments(peerId, VKApiAttachment.TYPE_POST, nextFrom, 1, 1, 45, 50, null)
            .map { response ->
                val docs: MutableList<Link> = ArrayList(Utils.safeCountOf(response.items))
                response.items.nonNullNoEmpty {
                    for (one in it) {
                        if (one.entry != null && one.entry?.attachment is VKApiLink) {
                            val dto = one.entry?.attachment as VKApiLink
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

    override fun onGuiCreated(viewHost: IChatAttachmentPostsView) {
        super.onGuiCreated(viewHost)
        resolveToolbar()
    }

    private fun resolveToolbar() {
        view?.setToolbarTitleString(getString(R.string.attachments_in_chat))
        view?.setToolbarTitleString(getString(R.string.posts_count, Utils.safeCountOf(data)))
    }
}