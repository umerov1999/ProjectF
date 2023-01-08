package dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit

import android.net.Uri
import android.os.Bundle
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppPerms.hasCameraPermission
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.FileUtil.createImageFile
import dev.ragnarok.fenrir.util.FileUtil.getExportedUriForFile
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.findInfoByPredicate
import dev.ragnarok.fenrir.util.Utils.safeCountOfMultiple
import java.io.File
import java.io.IOException

abstract class AbsAttachmentsEditPresenter<V : IBaseAttachmentsEditView> internal constructor(
    accountId: Int,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<V>(accountId, savedInstanceState) {
    val uploadManager: IUploadManager = Includes.uploadManager
    val data: ArrayList<AttachmentEntry>
    private var textBody: String? = null
    private var currentPhotoCameraUri: Uri? = null
    private var timerValue: Long? = null
    fun getTimerValue(): Long? {
        return timerValue
    }

    fun setTimerValue(timerValue: Long?) {
        this.timerValue = timerValue
        resolveTimerInfoView()
    }

    fun resolveTimerInfoView() {
        view?.setTimerValue(timerValue)
    }

    private fun resolveTextView() {
        view?.setTextBody(textBody)
    }

    fun getTextBody(): String? {
        return textBody
    }

    fun setTextBody(body: String?) {
        textBody = body
        resolveTextView()
    }

    open val needParcelSavingEntries: ArrayList<AttachmentEntry>
        get() = ArrayList(0)

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_CURRENT_PHOTO_CAMERA_URI, currentPhotoCameraUri)
        outState.putParcelableArrayList(SAVE_DATA, needParcelSavingEntries)
        outState.putString(SAVE_BODY, textBody)
        if (timerValue != null) {
            outState.putLong(SAVE_TIMER, timerValue ?: return)
        }
    }

    fun onUploadProgressUpdate(updates: List<IProgressUpdate>) {
        for (update in updates) {
            val info = findInfoByPredicate(
                data
            ) {
                (it.attachment is Upload
                        && it.attachment.getObjectId() == update.id)
            }
            if (info != null) {
                val entry = info.second
                val obj = entry.attachment as Upload
                if (obj.status != Upload.STATUS_UPLOADING) {
                    continue
                }
                view?.updateProgressAtIndex(
                    entry.id,
                    update.progress
                )
            }
        }
    }

    fun onUploadObjectRemovedFromQueue(ids: IntArray) {
        for (id in ids) {
            val index = findUploadIndexById(id)
            if (index != -1) {
                manuallyRemoveElement(index)
            }
        }
    }

    fun onUploadQueueUpdates(updates: List<Upload>, predicate: (Upload) -> Boolean) {
        val startSize = data.size
        var count = 0
        for (u in updates) {
            if (predicate.invoke(u)) {
                data.add(AttachmentEntry(true, u))
                count++
            }
        }
        if (count > 0) {
            safelyNotifyItemsAdded(startSize, count)
        }
    }

    fun safelyNotifyItemsAdded(position: Int, count: Int) {
        view?.notifyItemRangeInsert(position, count)
    }

    fun combine(
        first: List<AttachmentEntry>,
        second: List<AttachmentEntry>
    ): List<AttachmentEntry> {
        val data: MutableList<AttachmentEntry> = ArrayList(safeCountOfMultiple(first, second))
        data.addAll(first)
        data.addAll(second)
        return data
    }

    fun onUploadStatusUpdate(update: Upload) {
        val index = findUploadIndexById(update.getObjectId())
        if (index != -1) {
            safeNotifyDataSetChanged()
        }
    }

    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)
        viewHost.displayInitialModels(data)
        resolveTimerInfoView()
        resolveTextView()
    }

    fun fireRemoveClick(index: Int, attachment: AttachmentEntry) {
        if (attachment.attachment is Upload) {
            val upload = attachment.attachment
            uploadManager.cancel(upload.getObjectId())
            return
        }
        onAttachmentRemoveClick(index, attachment)
    }

    fun safelyNotifyItemRemoved(position: Int) {
        view?.notifyItemRemoved(position)
    }

    open fun onAttachmentRemoveClick(index: Int, attachment: AttachmentEntry) {
        throw UnsupportedOperationException()
    }

    fun manuallyRemoveElement(index: Int) {
        data.removeAt(index)
        safelyNotifyItemRemoved(index)

        //safeNotifyDataSetChanged();
    }

    private val maxCountOfAttachments: Int
        get() = 10

    private fun canAttachMore(): Boolean {
        return data.size < maxCountOfAttachments
    }

    private val maxFutureAttachmentCount: Int
        get() {
            val count = data.size - maxCountOfAttachments
            return count.coerceAtLeast(0)
        }

    fun firePhotoFromVkChoose() {
        view?.openAddVkPhotosWindow(
            maxFutureAttachmentCount,
            accountId,
            accountId
        )
    }

    private fun checkAbilityToAttachMore(): Boolean {
        return if (canAttachMore()) {
            true
        } else {
            view?.showError(R.string.reached_maximum_count_of_attachments)
            false
        }
    }

    fun firePhotoFromLocalGalleryChoose() {
        if (!hasReadStoragePermission(applicationContext)) {
            view?.requestReadExternalStoragePermission()
            return
        }
        view?.openAddPhotoFromGalleryWindow(maxFutureAttachmentCount)
    }

    fun firePhotoFromCameraChoose() {
        if (!hasCameraPermission(applicationContext)) {
            view?.requestCameraPermission()
            return
        }
        createImageFromCamera()
    }

    private fun createImageFromCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoCameraUri = getExportedUriForFile(applicationContext, photoFile)
            view?.openCamera(currentPhotoCameraUri ?: return)
        } catch (e: IOException) {
            view?.showError(e.message)
        }
    }

    fun firePhotoMaked() {
        view?.notifySystemAboutNewPhoto(currentPhotoCameraUri ?: return)
        val makedPhoto = LocalPhoto().setFullImageUri(currentPhotoCameraUri)
        doUploadPhotos(listOf(makedPhoto))
    }

    protected open fun doUploadPhotos(photos: List<LocalPhoto>, size: Int) {
        throw UnsupportedOperationException()
    }

    protected open fun doUploadFile(file: String, size: Int) {
        throw UnsupportedOperationException()
    }

    fun fireFileSelected(file: String) {
        doUploadFile(file, Upload.IMAGE_SIZE_FULL)
    }

    private fun doFinalUploadPhotos(photos: List<LocalPhoto>, size: Int) {
        if (size == Upload.IMAGE_SIZE_CROPPING && photos.size == 1) {
            var to_up = photos[0].getFullImageUri()
            if (to_up?.path?.let { File(it).isFile } == true) {
                to_up = Uri.fromFile(to_up.path?.let { File(it) })
            }
            view?.displayCropPhotoDialog(to_up)
        } else {
            doUploadPhotos(photos, size)
        }
    }

    private fun doUploadPhotos(photos: List<LocalPhoto>) {
        val size = Settings.get()
            .main()
            .uploadImageSize
        if (size == null) {
            view?.displaySelectUploadPhotoSizeDialog(photos)
        } else {
            doFinalUploadPhotos(photos, size)
        }
    }

    fun firePhotosFromGallerySelected(photos: ArrayList<LocalPhoto>) {
        doUploadPhotos(photos)
    }

    fun fireButtonPhotoClick() {
        if (checkAbilityToAttachMore()) {
            view?.displayChoosePhotoTypeDialog()
        }
    }

    fun fireButtonAudioClick() {
        if (checkAbilityToAttachMore()) {
            view?.openAddAudiosWindow(
                maxFutureAttachmentCount,
                accountId
            )
        }
    }

    fun fireButtonVideoClick() {
        if (checkAbilityToAttachMore()) {
            view?.openAddVideosWindow(
                maxFutureAttachmentCount,
                accountId
            )
        }
    }

    fun fireButtonDocClick() {
        if (checkAbilityToAttachMore()) {
            view?.openAddDocumentsWindow(
                maxFutureAttachmentCount,
                accountId
            )
        }
    }

    protected open fun onPollCreateClick() {
        throw UnsupportedOperationException()
    }

    protected open fun onTimerClick() {
        throw UnsupportedOperationException()
    }

    fun fireButtonPollClick() {
        onPollCreateClick()
    }

    fun fireButtonTimerClick() {
        onTimerClick()
    }

    protected open fun onModelsAdded(models: List<AbsModel>) {
        for (model in models) {
            data.add(AttachmentEntry(true, model))
        }
        safeNotifyDataSetChanged()
    }

    fun fireAttachmentsSelected(attachments: ArrayList<out AbsModel>) {
        onModelsAdded(attachments)
    }

    fun fireUploadPhotoSizeSelected(photos: List<LocalPhoto>, size: Int) {
        doFinalUploadPhotos(photos, size)
    }

    fun firePollCreated(poll: Poll) {
        onModelsAdded(listOf(poll))
    }

    protected fun safeNotifyDataSetChanged() {
        view?.notifyDataSetChanged()
    }

    fun fireTextChanged(s: CharSequence?) {
        textBody = s.toString()
    }

    fun fireVkPhotosSelected(photos: ArrayList<Photo>) {
        onModelsAdded(photos)
    }

    fun fireCameraPermissionResolved() {
        if (hasCameraPermission(applicationContext)) {
            createImageFromCamera()
        }
    }

    fun fireReadStoragePermissionResolved() {
        if (hasReadStoragePermission(applicationContext)) {
            view?.openAddPhotoFromGalleryWindow(
                maxFutureAttachmentCount
            )
        }
    }

    fun hasUploads(): Boolean {
        for (entry in data) {
            if (entry.attachment is Upload) {
                return true
            }
        }
        return false
    }

    fun findUploadIndexById(id: Int): Int {
        for (i in data.indices) {
            val item = data[i]
            if (item.attachment is Upload && item.attachment.getObjectId() == id) {
                return i
            }
        }
        return -1
    }

    open fun fireTimerTimeSelected(unixtime: Long) {
        throw UnsupportedOperationException()
    }

    companion object {
        private const val SAVE_DATA = "save_data"
        private const val SAVE_TIMER = "save_timer"
        private const val SAVE_BODY = "save_body"
        private const val SAVE_CURRENT_PHOTO_CAMERA_URI = "save_current_photo_camera_uri"
        fun createFrom(objects: List<Upload>?): List<AttachmentEntry> {
            val data: MutableList<AttachmentEntry> = ArrayList(objects?.size ?: 0)
            for (obj in objects.orEmpty()) {
                data.add(AttachmentEntry(true, obj))
            }
            return data
        }


        fun createFrom(
            pairs: List<Pair<Int, AbsModel>>,
            canDelete: Boolean
        ): List<AttachmentEntry> {
            val data: MutableList<AttachmentEntry> = ArrayList(pairs.size)
            for (pair in pairs) {
                data.add(AttachmentEntry(canDelete, pair.second).setOptionalId(pair.first))
            }
            return data
        }
    }

    init {
        if (savedInstanceState != null) {
            currentPhotoCameraUri =
                savedInstanceState.getParcelableCompat(SAVE_CURRENT_PHOTO_CAMERA_URI)
            textBody = savedInstanceState.getString(SAVE_BODY)
            timerValue = if (savedInstanceState.containsKey(SAVE_TIMER)) savedInstanceState.getLong(
                SAVE_TIMER
            ) else null
        }
        data = ArrayList()
        if (savedInstanceState != null) {
            val savedEntries: ArrayList<AttachmentEntry>? =
                savedInstanceState.getParcelableArrayListCompat(
                    SAVE_DATA
                )
            if (savedEntries.nonNullNoEmpty()) {
                data.addAll(savedEntries)
            }
        }
    }
}