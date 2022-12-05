package dev.ragnarok.fenrir.fragment.attachments.commentcreate

import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.IBaseAttachmentsEditView

interface ICreateCommentView : IBaseAttachmentsEditView {
    fun returnDataToParent(textBody: String?)
    fun goBack()
}