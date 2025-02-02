package dev.ragnarok.fenrir.fragment.attachments.commentcreate

import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IAddEvent
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IBaseEvent
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IRemoveEvent
import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.AbsAttachmentsEditPresenter
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.removeIf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip

class CommentCreatePresenter(
    accountId: Long,
    commentDbid: Int,
    sourceOwnerId: Long,
    body: String?,
    savedInstanceState: Bundle?
) : AbsAttachmentsEditPresenter<ICreateCommentView>(accountId, savedInstanceState) {
    private val commentId: Int = commentDbid
    private val destination: UploadDestination =
        UploadDestination.forComment(commentId, sourceOwnerId)
    private val attachmentsRepository: IAttachmentsRepository = Includes.attachmentsRepository
    private fun filterAttachEvents(event: IBaseEvent): Boolean {
        return event.accountId == accountId && event.attachToId == commentId && event.attachToType == AttachToType.COMMENT
    }

    private fun handleAttachmentRemoving(event: IRemoveEvent) {
        if (removeIf(
                data
            ) {
                it.optionalId == event.generatedId
            }
        ) {
            safeNotifyDataSetChanged()
        }
    }

    private fun handleAttachmentsAdding(event: IAddEvent) {
        addAll(event.attachments)
    }

    private fun addAll(d: List<Pair<Int, AbsModel>>) {
        val size = data.size
        for (pair in d) {
            data.add(AttachmentEntry(true, pair.second).setOptionalId(pair.first))
        }
        safelyNotifyItemsAdded(size, d.size)
    }

    private fun attachmentsSingle(): Flow<List<AttachmentEntry>> {
        return attachmentsRepository
            .getAttachmentsWithIds(accountId, AttachToType.COMMENT, commentId)
            .map { createFrom(it, true) }
    }

    private fun uploadsSingle(): Flow<List<AttachmentEntry>> {
        return uploadManager[accountId, destination]
            .map { u ->
                createFrom(
                    u
                )
            }
    }

    private fun loadAttachments() {
        appendJob(
            attachmentsSingle()
                .zip(uploadsSingle()) { first, second ->
                    combine(
                        first, second
                    )
                }
                .fromIOToMain({ onAttachmentsRestored(it) }) { it.printStackTrace() })
    }

    private fun onAttachmentsRestored(entries: List<AttachmentEntry>) {
        data.addAll(entries)
        if (entries.nonNullNoEmpty()) {
            safeNotifyDataSetChanged()
        }
    }

    override fun onAttachmentRemoveClick(index: Int, attachment: AttachmentEntry) {
        if (attachment.optionalId != 0) {
            attachmentsRepository.remove(
                accountId,
                AttachToType.COMMENT,
                commentId,
                attachment.optionalId
            ).hiddenIO()
            // из списка не удаляем, так как удаление из репозитория "слушается"
            // (будет удалено асинхронно и после этого удалится из списка)
        } else {
            // такого в комментах в принципе быть не может !!!
            manuallyRemoveElement(index)
        }
    }

    override fun onModelsAdded(models: List<AbsModel>) {
        attachmentsRepository.attach(
            accountId,
            AttachToType.COMMENT,
            commentId,
            models
        ).hiddenIO()
    }

    override fun doUploadPhotos(photos: List<LocalPhoto>, size: Int) {
        uploadManager.enqueue(UploadUtils.createIntents(accountId, destination, photos, size, true))
    }

    override fun doUploadFile(file: String, size: Int) {
        uploadManager.enqueue(UploadUtils.createIntents(accountId, destination, file, size, true))
    }

    override fun onGuiCreated(viewHost: ICreateCommentView) {
        super.onGuiCreated(viewHost)
        resolveButtonsVisibility()
    }

    private fun resolveButtonsVisibility() {
        view?.setSupportedButtons(
            photo = true,
            audio = true,
            video = true,
            doc = true,
            poll = false,
            timer = false
        )
    }

    private fun returnDataToParent() {
        view?.returnDataToParent(
            getTextBody()
        )
    }

    fun fireReadyClick() {
        view?.goBack()
    }

    fun onBackPressed(): Boolean {
        returnDataToParent()
        return true
    }

    init {
        if (savedInstanceState == null) {
            setTextBody(body)
        }
        appendJob(
            uploadManager.observeAdding()
                .sharedFlowToMain { it -> onUploadQueueUpdates(it) { destination.compareTo(it.destination) } })
        appendJob(
            uploadManager.observeStatus()
                .sharedFlowToMain {
                    onUploadStatusUpdate(
                        it
                    )
                })
        appendJob(
            uploadManager.observeProgress()
                .sharedFlowToMain { onUploadProgressUpdate(it) })
        appendJob(
            uploadManager.observeDeleting(true)
                .sharedFlowToMain {
                    onUploadObjectRemovedFromQueue(
                        it
                    )
                })
        appendJob(
            attachmentsRepository.observeAdding()
                .filter { filterAttachEvents(it) }
                .sharedFlowToMain { handleAttachmentsAdding(it) })
        appendJob(
            attachmentsRepository.observeRemoving()
                .filter { filterAttachEvents(it) }
                .sharedFlowToMain { handleAttachmentRemoving(it) })
        loadAttachments()
    }
}