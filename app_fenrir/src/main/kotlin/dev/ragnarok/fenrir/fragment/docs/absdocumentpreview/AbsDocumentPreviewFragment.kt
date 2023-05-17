package dev.ragnarok.fenrir.fragment.docs.absdocumentpreview

import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostCreation
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.Utils.shareLink

abstract class AbsDocumentPreviewFragment<P : BaseDocumentPresenter<V>, V : IBasicDocumentView> :
    BaseMvpFragment<P, V>(), IBasicDocumentView {

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
        MaterialAlertDialogBuilder(requireActivity())
            .setItems(items) { _: DialogInterface?, i: Int ->
                when (i) {
                    0 -> shareLink(requireActivity(), document.generateWebLink(), document.title)
                    1 -> startForSendAttachments(requireActivity(), accountId, document)
                    2 -> goToPostCreation(
                        requireActivity(),
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