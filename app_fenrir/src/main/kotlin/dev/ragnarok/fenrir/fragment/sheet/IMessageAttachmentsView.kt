package dev.ragnarok.fenrir.fragment.sheet

import android.net.Uri
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.ModelsBundle

interface IMessageAttachmentsView : IMvpView, IErrorView, IToastView {
    fun displayAttachments(entries: List<AttachmentEntry>)
    fun notifyDataAdded(positionStart: Int, count: Int)
    fun addPhoto(accountId: Long, ownerId: Long)
    fun notifyEntryRemoved(index: Int)
    fun displaySelectUploadPhotoSizeDialog(photos: List<LocalPhoto>)
    fun displayCropPhotoDialog(uri: Uri)
    fun displaySelectUploadFileSizeDialog(file: String)
    fun changePercentageSmoothly(id: Int, progress: Int)
    fun notifyItemChanged(index: Int)
    fun setEmptyViewVisible(visible: Boolean)
    fun requestCameraPermission()
    fun startCamera(fileUri: Uri)
    fun syncAccompanyingWithParent(accompanying: ModelsBundle)
    fun startAddDocumentActivity(accountId: Long)
    fun startAddVideoActivity(accountId: Long, ownerId: Long)
    fun startAddAudioActivity(accountId: Long)
}