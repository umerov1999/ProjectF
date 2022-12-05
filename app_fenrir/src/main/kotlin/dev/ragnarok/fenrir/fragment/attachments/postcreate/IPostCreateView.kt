package dev.ragnarok.fenrir.fragment.attachments.postcreate

import android.net.Uri
import dev.ragnarok.fenrir.fragment.attachments.abspostedit.IBasePostEditView
import dev.ragnarok.fenrir.fragment.base.core.IToolbarView

interface IPostCreateView : IBasePostEditView, IToolbarView {
    fun goBack()
    fun displayUploadUriSizeDialog(uris: List<Uri>)
}