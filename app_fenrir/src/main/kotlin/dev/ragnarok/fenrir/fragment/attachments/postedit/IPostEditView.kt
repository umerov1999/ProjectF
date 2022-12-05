package dev.ragnarok.fenrir.fragment.attachments.postedit

import dev.ragnarok.fenrir.fragment.attachments.abspostedit.IBasePostEditView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.fragment.base.core.IToolbarView

interface IPostEditView : IBasePostEditView, IToastView, IToolbarView {
    fun closeAsSuccess()
    fun showConfirmExitDialog()
}