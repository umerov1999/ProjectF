package dev.ragnarok.fenrir.fragment.conversation.conversationphotos

import dev.ragnarok.fenrir.fragment.conversation.abschatattachments.IBaseChatAttachmentsView
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource

interface IChatAttachmentPhotosView : IBaseChatAttachmentsView<Photo> {
    fun goToTempPhotosGallery(accountId: Long, source: TmpSource, index: Int)
    fun goToTempPhotosGallery(accountId: Long, ptr: Long, index: Int)
}