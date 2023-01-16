package dev.ragnarok.fenrir.fragment.absdocumentpreview

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Document

interface IBasicDocumentView : IMvpView, IToastView, IErrorView {
    fun shareDocument(accountId: Long, document: Document)
    fun requestWriteExternalStoragePermission()
}