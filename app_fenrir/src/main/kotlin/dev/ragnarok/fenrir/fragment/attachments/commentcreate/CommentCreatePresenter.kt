package dev.ragnarok.fenrir.fragment.attachments.commentcreate

import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.*
import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.AbsAttachmentsEditPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.removeIf
import dev.ragnarok.fenrir.util.rxutils.RxUtils.subscribeOnIOAndIgnore
import io.reactivex.rxjava3.core.Single

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

    private fun attachmentsSingle(): Single<List<AttachmentEntry>> {
        return attachmentsRepository
            .getAttachmentsWithIds(accountId, AttachToType.COMMENT, commentId)
            .map { createFrom(it, true) }
    }

    private fun uploadsSingle(): Single<List<AttachmentEntry>> {
        return uploadManager[accountId, destination]
            .flatMap { u ->
                Single.just(
                    createFrom(
                        u
                    )
                )
            }
    }

    private fun loadAttachments() {
        appendDisposable(attachmentsSingle()
            .zipWith(uploadsSingle()) { first: List<AttachmentEntry>, second: List<AttachmentEntry> ->
                combine(
                    first, second
                )
            }
            .fromIOToMain()
            .subscribe({ onAttachmentsRestored(it) }) { it.printStackTrace() })
    }

    private fun onAttachmentsRestored(entries: List<AttachmentEntry>) {
        data.addAll(entries)
        if (entries.nonNullNoEmpty()) {
            safeNotifyDataSetChanged()
        }
    }

    override fun onAttachmentRemoveClick(index: Int, attachment: AttachmentEntry) {
        if (attachment.optionalId != 0) {
            subscribeOnIOAndIgnore(
                attachmentsRepository.remove(
                    accountId,
                    AttachToType.COMMENT,
                    commentId,
                    attachment.optionalId
                )
            )
            // из списка не удаляем, так как удаление из репозитория "слушается"
            // (будет удалено асинхронно и после этого удалится из списка)
        } else {
            // такого в комментах в принципе быть не может !!!
            manuallyRemoveElement(index)
        }
    }

    override fun onModelsAdded(models: List<AbsModel>) {
        subscribeOnIOAndIgnore(
            attachmentsRepository.attach(
                accountId,
                AttachToType.COMMENT,
                commentId,
                models
            )
        )
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
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe { it -> onUploadQueueUpdates(it) { destination.compareTo(it.destination) } })
        appendDisposable(uploadManager.obseveStatus()
            .observeOn(provideMainThreadScheduler())
            .subscribe {
                onUploadStatusUpdate(
                    it
                )
            })
        appendDisposable(uploadManager.observeProgress()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadProgressUpdate(it) })
        appendDisposable(uploadManager.observeDeleting(true)
            .observeOn(provideMainThreadScheduler())
            .subscribe {
                onUploadObjectRemovedFromQueue(
                    it
                )
            })
        appendDisposable(attachmentsRepository.observeAdding()
            .filter { filterAttachEvents(it) }
            .observeOn(provideMainThreadScheduler())
            .subscribe { handleAttachmentsAdding(it) })
        appendDisposable(attachmentsRepository.observeRemoving()
            .filter { filterAttachEvents(it) }
            .observeOn(provideMainThreadScheduler())
            .subscribe { handleAttachmentRemoving(it) })
        loadAttachments()
    }
}