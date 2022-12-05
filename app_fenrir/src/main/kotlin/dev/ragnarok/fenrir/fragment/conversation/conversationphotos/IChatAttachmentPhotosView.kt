package dev.ragnarok.fenrir.fragment.conversation.conversationphotos

import dev.ragnarok.fenrir.fragment.conversation.abschatattachments.IBaseChatAttachmentsView
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource

interface IChatAttachmentPhotosView : IBaseChatAttachmentsView<Photo> {
    fun goToTempPhotosGallery(accountId: Int, source: TmpSource, index: Int)
    fun goToTempPhotosGallery(accountId: Int, ptr: Long, index: Int)
}