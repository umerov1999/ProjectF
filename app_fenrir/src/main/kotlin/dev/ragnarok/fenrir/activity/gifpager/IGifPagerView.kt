package dev.ragnarok.fenrir.activity.gifpager

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.docs.absdocumentpreview.IBasicDocumentView
import dev.ragnarok.fenrir.model.Document

interface IGifPagerView : IBasicDocumentView {
    fun displayData(mDocuments: List<Document>, selectedIndex: Int)
    fun setupAddRemoveButton(addEnable: Boolean)
    fun toolbarTitle(@StringRes titleRes: Int, vararg params: Any?)
    fun toolbarSubtitle(@StringRes titleRes: Int, vararg params: Any?)
}