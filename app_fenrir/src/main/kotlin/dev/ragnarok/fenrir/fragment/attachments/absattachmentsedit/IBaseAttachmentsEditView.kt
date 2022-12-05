package dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit

import android.net.Uri
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.LocalPhoto

interface IBaseAttachmentsEditView : IMvpView, IErrorView {
    fun displayInitialModels(models: MutableList<AttachmentEntry>)
    fun setSupportedButtons(
        photo: Boolean,
        audio: Boolean,
        video: Boolean,
        doc: Boolean,
        poll: Boolean,
        timer: Boolean
    )

    fun setTextBody(text: CharSequence?)
    fun openAddVkPhotosWindow(maxSelectionCount: Int, accountId: Int, ownerId: Int)
    fun openAddPhotoFromGalleryWindow(maxSelectionCount: Int)
    fun openCamera(photoCameraUri: Uri)
    fun openAddAudiosWindow(maxSelectionCount: Int, accountId: Int)
    fun openAddDocumentsWindow(maxSelectionCount: Int, accountId: Int)
    fun openAddVideosWindow(maxSelectionCount: Int, accountId: Int)
    fun openPollCreationWindow(accountId: Int, ownerId: Int)
    fun requestReadExternalStoragePermission()
    fun requestCameraPermission()
    fun displayChoosePhotoTypeDialog()
    fun notifySystemAboutNewPhoto(uri: Uri)
    fun displaySelectUploadPhotoSizeDialog(photos: List<LocalPhoto>)
    fun notifyDataSetChanged()
    fun updateProgressAtIndex(index: Int, progress: Int)
    fun setTimerValue(time: Long?)
    fun showEnterTimeDialog(initialTimeUnixtime: Long)
    fun notifyItemRangeInsert(position: Int, count: Int)
    fun notifyItemRemoved(position: Int)
    fun notifyItemChanged(position: Int)
    fun displayCropPhotoDialog(uri: Uri?)
}