package dev.ragnarok.fenrir.fragment.sheet

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IBaseEvent
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.*
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.util.AppPerms.hasCameraPermission
import dev.ragnarok.fenrir.util.FileUtil.createImageFile
import dev.ragnarok.fenrir.util.FileUtil.getExportedUriForFile
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.findIndexByPredicate
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import dev.ragnarok.fenrir.util.rxutils.RxUtils.subscribeOnIOAndIgnore
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.IOException

class MessageAttachmentsPresenter(
    private val accountId: Long,
    private val messageOwnerId: Long,
    private val messageId: Int,
    private val context: Context,
    bundle: ModelsBundle?,
    savedInstanceState: Bundle?
) : RxSupportPresenter<IMessageAttachmentsView>(savedInstanceState) {
    private val entries: MutableList<AttachmentEntry>
    private val attachmentsRepository: IAttachmentsRepository
    private val destination: UploadDestination = UploadDestination.forMessage(messageId)
    private val uploadManager: IUploadManager
    private var currentPhotoCameraUri: Uri? = null
    private fun handleInputModels(bundle: ModelsBundle?) {
        if (bundle == null) {
            return
        }
        for (model in bundle) {
            entries.add(AttachmentEntry(true, model).setAccompanying(true))
        }
    }

    private fun resolveEmptyViewVisibility() {
        view?.setEmptyViewVisible(
            entries.isEmpty()
        )
    }

    private fun onUploadProgressUpdates(updates: List<IProgressUpdate>) {
        for (update in updates) {
            val index = findUploadObjectIndex(update.id)
            if (index != -1) {
                val upId = entries[index].id
                val upload = entries[index].attachment as Upload
                if (upload.status != Upload.STATUS_UPLOADING) {
                    // for uploading only
                    continue
                }
                upload.progress = update.progress
                view?.changePercentageSmoothly(
                    upId,
                    update.progress
                )
            }
        }
    }

    private fun onUploadStatusChanges(upload: Upload) {
        val index = findUploadObjectIndex(upload.getObjectId())
        if (index != -1) {
            (entries[index].attachment as Upload)
                .setStatus(upload.status).errorText = upload.errorText
            view?.notifyItemChanged(
                index
            )
        }
    }

    private fun onUploadsRemoved(ids: IntArray) {
        for (id in ids) {
            val index = findUploadObjectIndex(id)
            if (index != -1) {
                entries.removeAt(index)
                view?.notifyEntryRemoved(
                    index
                )
                resolveEmptyViewVisibility()
            }
        }
    }

    private fun onUploadsAdded(uploads: List<Upload>) {
        var count = 0
        for (i in uploads.indices.reversed()) {
            val upload = uploads[i]
            if (destination.compareTo(upload.destination)) {
                val entry = AttachmentEntry(true, upload)
                entries.add(0, entry)
                count++
            }
        }
        val finalCount = count
        view?.notifyDataAdded(
            0,
            finalCount
        )
        resolveEmptyViewVisibility()
    }

    private fun findUploadObjectIndex(id: Int): Int {
        return findIndexByPredicate(
            entries
        ) {
            val model = it.attachment
            model is Upload && model.getObjectId() == id
        }
    }

    private fun onAttachmentRemoved(optionId: Int) {
        for (i in entries.indices) {
            if (entries[i].optionalId == optionId) {
                entries.removeAt(i)
                view?.notifyEntryRemoved(
                    i
                )
                break
            }
        }
        resolveEmptyViewVisibility()
    }

    private fun onAttachmentsAdded(pairs: List<Pair<Int, AbsModel>>) {
        onDataReceived(entities2entries(pairs))
    }

    private fun loadData() {
        appendDisposable(
            createLoadAllSingle()
                .fromIOToMain()
                .subscribe({ data -> onDataReceived(data) }, ignore())
        )
    }

    private fun createLoadAllSingle(): Single<List<AttachmentEntry>> {
        return attachmentsRepository
            .getAttachmentsWithIds(messageOwnerId, AttachToType.MESSAGE, messageId)
            .map { entities2entries(it) }
            .zipWith(
                uploadManager[messageOwnerId, destination]
            ) { atts: List<AttachmentEntry>, uploads: List<Upload> ->
                val data: MutableList<AttachmentEntry> = ArrayList(atts.size + uploads.size)
                for (u in uploads) {
                    data.add(AttachmentEntry(true, u))
                }
                data.addAll(atts)
                data
            }
    }

    private fun onDataReceived(data: List<AttachmentEntry>) {
        if (data.isEmpty()) {
            return
        }
        val startCount = entries.size
        entries.addAll(data)
        resolveEmptyViewVisibility()
        view?.notifyDataAdded(
            startCount,
            data.size
        )
    }

    override fun onGuiCreated(viewHost: IMessageAttachmentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayAttachments(entries)
        resolveEmptyViewVisibility()
    }

    fun fireAddPhotoButtonClick() {
        // Если сообщения группы - предлагать фотографии сообщества, а не группы
        view?.addPhoto(
            accountId, messageOwnerId
        )
    }

    fun firePhotosSelected(
        photos: ArrayList<Photo>?,
        localPhotos: ArrayList<LocalPhoto>?,
        file: String?,
        video: LocalVideo?
    ) {
        when {
            file.nonNullNoEmpty() -> doUploadFile(file)
            photos.nonNullNoEmpty() -> {
                fireAttachmentsSelected(photos)
            }

            localPhotos.nonNullNoEmpty() -> {
                doUploadPhotos(localPhotos)
            }

            video != null -> {
                doUploadVideo(video.getData().toString())
            }
        }
    }

    private fun doUploadFile(file: String) {
        for (i in Settings.get().other().photoExt()) {
            if (file.endsWith(i, true)) {
                val size = Settings.get()
                    .main()
                    .uploadImageSize
                when (size) {
                    null -> {
                        view?.displaySelectUploadFileSizeDialog(
                            file
                        )
                    }

                    Upload.IMAGE_SIZE_CROPPING -> {
                        view?.displayCropPhotoDialog(
                            Uri.fromFile(File(file))
                        )
                    }

                    else -> {
                        doUploadFile(file, size, 1)
                    }
                }
                return
            }
        }
        for (i in Settings.get().other().videoExt()) {
            if (file.endsWith(i, true)) {
                doUploadFile(
                    file,
                    0,
                    0
                )
                return
            }
        }
        for (i in Settings.get().other().audioExt()) {
            if (file.endsWith(i, true)) {
                doUploadFile(
                    file,
                    0,
                    2
                )
                return
            }
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select)
            .setNegativeButton(R.string.video) { _: DialogInterface?, _: Int ->
                doUploadFile(
                    file,
                    0,
                    0
                )
            }
            .setNeutralButton(R.string.select_audio) { _: DialogInterface?, _: Int ->
                doUploadFile(
                    file,
                    0,
                    2
                )
            }
            .setPositiveButton(R.string.photo) { _: DialogInterface?, _: Int ->
                val size = Settings.get()
                    .main()
                    .uploadImageSize
                when (size) {
                    null -> {
                        view?.displaySelectUploadFileSizeDialog(
                            file
                        )
                    }

                    Upload.IMAGE_SIZE_CROPPING -> {
                        view?.displayCropPhotoDialog(
                            Uri.fromFile(File(file))
                        )
                    }

                    else -> {
                        doUploadFile(file, size, 1)
                    }
                }
            }
            .create().show()
    }

    private fun doUploadPhotos(photos: List<LocalPhoto>) {
        val size = Settings.get()
            .main()
            .uploadImageSize
        if (size == null) {
            view?.displaySelectUploadPhotoSizeDialog(
                photos
            )
        } else if (size == Upload.IMAGE_SIZE_CROPPING && photos.size == 1) {
            var to_up = photos[0].getFullImageUri() ?: return
            if (to_up.path?.let { File(it).isFile } == true) {
                to_up = Uri.fromFile(to_up.path?.let { File(it) })
            }
            val finalTo_up = to_up
            view?.displayCropPhotoDialog(
                finalTo_up
            )
        } else {
            doUploadPhotos(photos, size)
        }
    }

    fun doUploadFile(file: String, size: Int, type: Int) {
        val intents: List<UploadIntent> = when (type) {
            0 -> {
                UploadUtils.createIntents(
                    messageOwnerId, UploadDestination.forMessage(
                        messageId, MessageMethod.VIDEO
                    ), file, size, true
                )
            }

            1 -> {
                UploadUtils.createIntents(messageOwnerId, destination, file, size, true)
            }

            else -> {
                UploadUtils.createIntents(
                    messageOwnerId, UploadDestination.forMessage(
                        messageId, MessageMethod.AUDIO
                    ), file, size, true
                )
            }
        }
        uploadManager.enqueue(intents)
    }

    private fun doUploadVideo(file: String) {
        val intents = UploadUtils.createVideoIntents(
            messageOwnerId, UploadDestination.forMessage(messageId, MessageMethod.VIDEO), file, true
        )
        uploadManager.enqueue(intents)
    }

    private fun doUploadPhotos(photos: List<LocalPhoto>, size: Int) {
        val intents = UploadUtils.createIntents(
            messageOwnerId, destination, photos, size, true
        )
        uploadManager.enqueue(intents)
    }

    fun fireRetryClick(entry: AttachmentEntry) {
        fireRemoveClick(entry)
        if (entry.attachment is Upload) {
            val upl = entry.attachment
            val intents: MutableList<UploadIntent> = ArrayList()
            intents.add(
                UploadIntent(accountId, upl.destination)
                    .setSize(upl.size)
                    .setAutoCommit(upl.isAutoCommit)
                    .setFileId(upl.fileId)
                    .setFileUri(upl.fileUri)
            )
            uploadManager.enqueue(intents)
        }
    }

    fun fireRemoveClick(entry: AttachmentEntry) {
        if (entry.optionalId != 0) {
            subscribeOnIOAndIgnore(
                attachmentsRepository.remove(
                    messageOwnerId,
                    AttachToType.MESSAGE,
                    messageId,
                    entry.optionalId
                )
            )
            return
        }
        if (entry.attachment is Upload) {
            uploadManager.cancel(entry.attachment.getObjectId())
            return
        }
        if (entry.isAccompanying) {
            for (i in entries.indices) {
                if (entries[i].id == entry.id) {
                    entries.removeAt(i)
                    view?.notifyEntryRemoved(
                        i
                    )
                    syncAccompanyingWithParent()
                    break
                }
            }
        }
    }

    fun fireUploadPhotoSizeSelected(photos: List<LocalPhoto>, imageSize: Int) {
        if (imageSize == Upload.IMAGE_SIZE_CROPPING && photos.size == 1) {
            var to_up = photos[0].getFullImageUri() ?: return
            if (to_up.path?.let { File(it).isFile } == true) {
                to_up = Uri.fromFile(to_up.path?.let { File(it) })
            }
            val finalTo_up = to_up
            view?.displayCropPhotoDialog(
                finalTo_up
            )
        } else {
            doUploadPhotos(photos, imageSize)
        }
    }

    fun fireUploadFileSizeSelected(file: String, imageSize: Int) {
        if (imageSize == Upload.IMAGE_SIZE_CROPPING) {
            view?.displayCropPhotoDialog(
                Uri.fromFile(File(file))
            )
        } else {
            doUploadFile(file, imageSize, 1)
        }
    }

    fun fireCameraPermissionResolved() {
        if (hasCameraPermission(applicationContext)) {
            makePhotoInternal()
        }
    }

    fun fireButtonCameraClick() {
        if (hasCameraPermission(applicationContext)) {
            makePhotoInternal()
        } else {
            view?.requestCameraPermission()
        }
    }

    fun fireCompressSettings(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.select_image_size_title))
            .setSingleChoiceItems(
                R.array.array_image_sizes_settings_names,
                Settings.get().main().uploadImageSizePref
            ) { dialogInterface: DialogInterface, j: Int ->
                Settings.get().main().uploadImageSize = j
                dialogInterface.dismiss()
            }
            .setCancelable(true)
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun makePhotoInternal() {
        try {
            val file = createImageFile()
            currentPhotoCameraUri = getExportedUriForFile(applicationContext, file)
            view?.startCamera(
                currentPhotoCameraUri ?: return
            )
        } catch (e: IOException) {
            view?.showError(
                e.message
            )
        }
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_CAMERA_FILE_URI, currentPhotoCameraUri)

        // сохраняем в outState только неПерсистентные данные
        val accompanying = ArrayList<AttachmentEntry>()
        for (entry in entries) {
            if (entry.isAccompanying) {
                accompanying.add(entry)
            }
        }
        outState.putParcelableArrayList(SAVE_ACCOMPANYING_ENTRIES, accompanying)
    }

    private fun syncAccompanyingWithParent() {
        val bundle = ModelsBundle()
        for (entry in entries) {
            if (entry.isAccompanying) {
                bundle.append(entry.attachment)
            }
        }
        view?.syncAccompanyingWithParent(
            bundle
        )
    }

    @Suppress("DEPRECATION")
    fun firePhotoMaked() {
        val uri = currentPhotoCameraUri
        currentPhotoCameraUri = null
        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        applicationContext.sendBroadcast(scanIntent)
        val makedPhoto = LocalPhoto().setFullImageUri(uri)
        doUploadPhotos(listOf(makedPhoto))
    }

    fun fireButtonVideoClick() {
        view?.startAddVideoActivity(
            accountId, messageOwnerId
        )
    }

    fun fireButtonAudioClick() {
        view?.startAddAudioActivity(
            accountId
        )
    }

    fun fireButtonDocClick() {
        view?.startAddDocumentActivity(
            accountId
        )
    }

    fun fireAttachmentsSelected(attachments: ArrayList<out AbsModel>) {
        subscribeOnIOAndIgnore(
            attachmentsRepository.attach(
                messageOwnerId,
                AttachToType.MESSAGE,
                messageId,
                attachments
            )
        )
    }

    companion object {
        private const val SAVE_CAMERA_FILE_URI = "save_camera_file_uri"
        private const val SAVE_ACCOMPANYING_ENTRIES = "save_accompanying_entries"
        internal fun entities2entries(pairs: List<Pair<Int, AbsModel>>): List<AttachmentEntry> {
            val entries: MutableList<AttachmentEntry> = ArrayList(pairs.size)
            for (pair in pairs) {
                entries.add(
                    AttachmentEntry(true, pair.second)
                        .setOptionalId(pair.first)
                )
            }
            return entries
        }
    }

    init {
        entries = ArrayList()
        attachmentsRepository = Includes.attachmentsRepository
        uploadManager = Includes.uploadManager
        if (savedInstanceState != null) {
            currentPhotoCameraUri = savedInstanceState.getParcelableCompat(SAVE_CAMERA_FILE_URI)
            val accompanying: ArrayList<AttachmentEntry>? =
                savedInstanceState.getParcelableArrayListCompat(
                    SAVE_ACCOMPANYING_ENTRIES
                )
            if (accompanying != null) {
                entries.addAll(accompanying)
            }
        } else {
            handleInputModels(bundle)
        }
        val predicate =
            io.reactivex.rxjava3.functions.Predicate { event: IBaseEvent -> event.attachToType == AttachToType.MESSAGE && event.attachToId == messageId && event.accountId == messageOwnerId }
        appendDisposable(attachmentsRepository
            .observeAdding()
            .filter(predicate)
            .observeOn(provideMainThreadScheduler())
            .subscribe { onAttachmentsAdded(it.attachments) })
        appendDisposable(attachmentsRepository
            .observeRemoving()
            .filter(predicate)
            .observeOn(provideMainThreadScheduler())
            .subscribe { onAttachmentRemoved(it.generatedId) })
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadsAdded(it) })
        appendDisposable(uploadManager.observeDeleting(true)
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadsRemoved(it) })
        appendDisposable(uploadManager.obseveStatus()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadStatusChanges(it) })
        appendDisposable(uploadManager.observeProgress()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadProgressUpdates(it) })
        loadData()
    }
}