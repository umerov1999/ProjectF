package dev.ragnarok.fenrir.fragment.attachments.commentedit

import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.IBaseAttachmentsEditView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView
import dev.ragnarok.fenrir.model.Comment

interface ICommentEditView : IBaseAttachmentsEditView, IProgressView {
    fun goBackWithResult(comment: Comment)
    fun showConfirmWithoutSavingDialog()
    fun goBack()
}