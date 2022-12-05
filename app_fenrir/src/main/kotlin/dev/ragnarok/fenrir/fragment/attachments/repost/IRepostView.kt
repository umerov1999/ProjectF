package dev.ragnarok.fenrir.fragment.attachments.repost

import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.IBaseAttachmentsEditView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView

interface IRepostView : IBaseAttachmentsEditView, IProgressView {
    fun goBack()
}