package dev.ragnarok.fenrir.fragment.attachments.commentedit

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.ICommentsInteractor
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.CommentsInteractor
import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.AbsAttachmentsEditPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.copyToArrayListWithPredicate

class CommentEditPresenter(
    comment: Comment,
    accountId: Long,
    CommentThread: Int?,
    savedInstanceState: Bundle?
) : AbsAttachmentsEditPresenter<ICommentEditView>(accountId, savedInstanceState) {
    private val orig: Comment
    private val destination: UploadDestination
    private val commentsInteractor: ICommentsInteractor
    private val CommentThread: Int?
    private var editingNow = false
    private var canGoBack = false
    private fun onUploadsQueueChanged(pair: Pair<Upload, UploadResult<*>>) {
        val upload = pair.first
        val result = pair.second
        val index = findUploadIndexById(upload.getObjectId())
        val entry: AttachmentEntry = if (result.result is Photo) {
            AttachmentEntry(true, result.result)
        } else {
            // not supported!!!
            return
        }
        if (index != -1) {
            data[index] = entry
        } else {
            data.add(0, entry)
        }
        safeNotifyDataSetChanged()
    }

    override fun onAttachmentRemoveClick(index: Int, attachment: AttachmentEntry) {
        manuallyRemoveElement(index)
    }

    override fun doUploadPhotos(photos: List<LocalPhoto>, size: Int) {
        val intents = UploadUtils.createIntents(accountId, destination, photos, size, false)
        uploadManager.enqueue(intents)
    }

    override fun doUploadFile(file: String, size: Int) {
        val intents = UploadUtils.createIntents(accountId, destination, file, size, false)
        uploadManager.enqueue(intents)
    }

    private fun onUploadsReceived(uploads: List<Upload>) {
        data.addAll(createFrom(uploads))
        safeNotifyDataSetChanged()
    }

    // сохраняем все, кроме аплоада
    override val needParcelSavingEntries: ArrayList<AttachmentEntry>
        get() =// сохраняем все, кроме аплоада
            copyToArrayListWithPredicate(
                data
            ) {
                it.attachment !is Upload
            }

    override fun onGuiCreated(viewHost: ICommentEditView) {
        super.onGuiCreated(viewHost)
        resolveButtonsAvailability()
        resolveProgressDialog()
    }

    private fun resolveButtonsAvailability() {
        view?.setSupportedButtons(
            photo = true,
            audio = true,
            video = true,
            doc = true,
            poll = false,
            timer = false
        )
    }

    private fun initialPopulateEntries() {
        orig.attachments.requireNonNull {
            val models: List<AbsModel> = it.toList()
            for (m in models) {
                data.add(AttachmentEntry(true, m))
            }
        }
    }

    fun fireReadyClick() {
        if (hasUploads()) {
            view?.showError(R.string.upload_not_resolved_exception_message)
            return
        }
        val models: MutableList<AbsModel> = ArrayList()
        for (entry in data) {
            models.add(entry.attachment)
        }
        setEditingNow(true)
        val commented = orig.commented
        val commentId = orig.getObjectId()
        val body = getTextBody()
        appendDisposable(commentsInteractor.edit(
            accountId,
            commented,
            commentId,
            body,
            CommentThread,
            models
        )
            .fromIOToMain()
            .subscribe({ comment -> onEditComplete(comment) }) { t ->
                onEditError(
                    t
                )
            })
    }

    private fun onEditError(t: Throwable) {
        setEditingNow(false)
        showError(t)
    }

    private fun onEditComplete(comment: Comment) {
        setEditingNow(false)
        canGoBack = true
        view?.goBackWithResult(
            comment
        )
    }

    private fun setEditingNow(editingNow: Boolean) {
        this.editingNow = editingNow
        resolveProgressDialog()
    }

    private fun resolveProgressDialog() {
        if (editingNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.saving,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    fun onBackPressed(): Boolean {
        if (canGoBack) {
            return true
        }
        view?.showConfirmWithoutSavingDialog()
        return false
    }

    fun fireSavingCancelClick() {
        uploadManager.cancelAll(accountId, destination)
        canGoBack = true
        view?.goBack()
    }

    init {
        commentsInteractor = CommentsInteractor(networkInterfaces, stores, owners)
        orig = comment
        destination =
            UploadDestination.forComment(comment.getObjectId(), comment.commented.sourceOwnerId)
        this.CommentThread = CommentThread
        if (savedInstanceState == null) {
            setTextBody(orig.text)
            initialPopulateEntries()
        }
        appendDisposable(uploadManager[accountId, destination]
            .fromIOToMain()
            .subscribe { uploads -> onUploadsReceived(uploads) })
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe { it ->
                onUploadQueueUpdates(
                    it
                ) { it.accountId == accountId && destination.compareTo(it.destination) }
            })
        appendDisposable(uploadManager.observeDeleting(false)
            .observeOn(provideMainThreadScheduler())
            .subscribe {
                onUploadObjectRemovedFromQueue(
                    it
                )
            })
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
        appendDisposable(uploadManager.observeResults()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadsQueueChanged(it) })
    }
}