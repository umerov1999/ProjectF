package dev.ragnarok.fenrir.fragment.conversation.conversationvideos

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.fragment.conversation.abschatattachments.BaseChatAttachmentsPresenter
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single

class ChatAttachmentVideoPresenter(peerId: Int, accountId: Int, savedInstanceState: Bundle?) :
    BaseChatAttachmentsPresenter<Video, IChatAttachmentVideoView>(
        peerId,
        accountId,
        savedInstanceState
    ) {
    override fun onDataChanged() {
        super.onDataChanged()
        resolveToolbar()
    }

    override fun requestAttachments(
        peerId: Int,
        nextFrom: String?
    ): Single<Pair<String?, List<Video>>> {
        return get().vkDefault(accountId)
            .messages()
            .getHistoryAttachments(peerId, "video", nextFrom, 1, 50, null)
            .map { response ->
                val videos: MutableList<Video> = ArrayList(
                    Utils.safeCountOf(response.items)
                )
                response.items.nonNullNoEmpty {
                    for (one in it) {
                        if (one.entry != null && one.entry?.attachment is VKApiVideo) {
                            val dto = one.entry?.attachment as VKApiVideo
                            videos.add(
                                Dto2Model.transform(dto).setMsgId(one.messageId)
                                    .setMsgPeerId(peerId)
                            )
                        }
                    }
                }
                create(response.next_from, videos)
            }
    }

    override fun onGuiCreated(viewHost: IChatAttachmentVideoView) {
        super.onGuiCreated(viewHost)
        resolveToolbar()
    }

    private fun resolveToolbar() {
        view?.setToolbarTitleString(getString(R.string.attachments_in_chat))
        view?.setToolbarTitleString(getString(R.string.videos_count, Utils.safeCountOf(data)))
    }
}