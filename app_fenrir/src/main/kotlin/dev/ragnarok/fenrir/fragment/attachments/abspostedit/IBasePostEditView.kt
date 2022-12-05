package dev.ragnarok.fenrir.fragment.attachments.abspostedit

import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.IBaseAttachmentsEditView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView

interface IBasePostEditView : IBaseAttachmentsEditView, IProgressView {
    fun displaySignerInfo(fullName: String?, photo: String?)
    fun setShowAuthorChecked(checked: Boolean)
    fun setSignerInfoVisible(visible: Boolean)
    fun setAddSignatureOptionVisible(visible: Boolean)
    fun setFromGroupOptionVisible(visible: Boolean)
    fun setFriendsOnlyOptionVisible(visible: Boolean)
    fun setFromGroupChecked(checked: Boolean)
    fun setFriendsOnlyChecked(checked: Boolean)
}