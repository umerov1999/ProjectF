package dev.ragnarok.fenrir.fragment.absdocumentpreview

import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.BaseMvpActivity
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostCreation
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.shareLink

abstract class AbsDocumentPreviewActivity<P : BaseDocumentPresenter<V>, V : IBasicDocumentView> :
    BaseMvpActivity<P, V>(), IBasicDocumentView {

    abstract val requestWritePermission: AppPerms.DoRequestPermissions

    override fun requestWriteExternalStoragePermission() {
        requestWritePermission.launch()
    }

    override fun shareDocument(accountId: Long, document: Document) {
        val items = arrayOf(
            getString(R.string.share_link),
            getString(R.string.repost_send_message),
            getString(R.string.repost_to_wall)
        )
        MaterialAlertDialogBuilder(this)
            .setItems(items) { _: DialogInterface?, i: Int ->
                when (i) {
                    0 -> shareLink(this, document.generateWebLink(), document.title)
                    1 -> startForSendAttachments(this, accountId, document)
                    2 -> goToPostCreation(
                        this,
                        accountId,
                        accountId,
                        EditingPostType.TEMP,
                        listOf(document)
                    )
                }
            }
            .setCancelable(true)
            .setTitle(R.string.share_document_title)
            .show()
    }
}
