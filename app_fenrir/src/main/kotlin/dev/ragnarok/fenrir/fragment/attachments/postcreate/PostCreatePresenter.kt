package dev.ragnarok.fenrir.fragment.attachments.postcreate

import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils.isMimeVideo
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IBaseEvent
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IRemoveEvent
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.attachments.abspostedit.AbsPostEditPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.*
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.copyToArrayListWithPredicate
import dev.ragnarok.fenrir.util.Utils.findInfoByPredicate
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import dev.ragnarok.fenrir.util.rxutils.RxUtils.subscribeOnIOAndIgnore
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class PostCreatePresenter(
    accountId: Int,
    ownerId: Int,
    @EditingPostType editingType: Int,
    bundle: ModelsBundle?,
    private val attrs: WallEditorAttrs,
    streams: ArrayList<Uri>?,
    links: String?,
    mime: String?,
    savedInstanceState: Bundle?
) : AbsPostEditPresenter<IPostCreateView>(accountId, savedInstanceState) {
    private val ownerId: Int

    @EditingPostType
    private val editingType: Int
    private val attachmentsRepository: IAttachmentsRepository
    private val walls: IWallsRepository
    private val mime: String?
    private var post: Post? = null
    private var postPublished = false
    private var upload: Optional<ArrayList<Uri>?>
    private var publishingNow = false
    private var links: String? = null
    override fun onGuiCreated(viewHost: IPostCreateView) {
        super.onGuiCreated(viewHost)
        @StringRes val toolbarTitleRes =
            if (isCommunity && !isEditorOrHigher) R.string.title_suggest_news else R.string.title_activity_create_post
        viewHost.setToolbarTitle(getString(toolbarTitleRes))
        viewHost.setToolbarSubtitle(owner.fullName)
        resolveSignerInfo()
        resolveSupportButtons()
        resolvePublishDialogVisibility()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        checkUploadUris()
    }

    private fun checkUploadUris() {
        if (post != null && upload.nonEmpty()) {
            val uris: List<Uri> = upload.get() ?: return
            val size = Settings.get()
                .main()
                .uploadImageSize
            val isVideo = isMimeVideo(mime)
            if (size == null && !isVideo) {
                resumedView?.displayUploadUriSizeDialog(
                    uris
                )
            } else {
                uploadStreamsImpl(uris, size ?: 0, isVideo)
            }
        }
    }

    private fun uploadStreamsImpl(streams: List<Uri>, size: Int, isVideo: Boolean) {
        val tPost = post ?: return
        upload = empty()
        val destination = if (isVideo) UploadDestination.forPost(
            tPost.dbid,
            ownerId,
            MessageMethod.VIDEO
        ) else UploadDestination.forPost(
            tPost.dbid, ownerId
        )
        val intents: MutableList<UploadIntent> = ArrayList(streams.size)
        if (!isVideo) {
            for (uri in streams) {
                intents.add(
                    UploadIntent(accountId, destination)
                        .setAutoCommit(true)
                        .setFileUri(uri)
                        .setSize(size)
                )
            }
        } else {
            for (uri in streams) {
                intents.add(
                    UploadIntent(accountId, destination)
                        .setAutoCommit(true)
                        .setFileUri(uri)
                )
            }
        }
        uploadManager.enqueue(intents)
    }

    override fun onFromGroupChecked(checked: Boolean) {
        super.onFromGroupChecked(checked)
        setAddSignatureOptionAvailable(checked)
        resolveSignerInfo()
    }

    override fun onShowAuthorChecked(checked: Boolean) {
        resolveSignerInfo()
    }

    private fun resolveSignerInfo() {
        var visible = isGroup && !isEditorOrHigher || !fromGroup.get() || addSignature.get()
        if (isCommunity && isEditorOrHigher) {
            visible = addSignature.get()
        }
        val author = author
        val finalVisible = visible
        view?.let {
            it.displaySignerInfo(author.fullName, author.get100photoOrSmaller())
            it.setSignerInfoVisible(finalVisible)
        }
    }

    private val author: Owner
        get() = attrs.getEditor()
    private val isEditorOrHigher: Boolean
        get() {
            val owner = owner
            return owner is Community && owner.adminLevel >= VKApiCommunity.AdminLevel.EDITOR
        }
    private val isGroup: Boolean
        get() {
            val owner = owner
            return owner is Community && owner.communityType == VKApiCommunity.Type.GROUP
        }
    private val isCommunity: Boolean
        get() {
            val owner = owner
            return owner is Community && owner.communityType == VKApiCommunity.Type.PAGE
        }

    // сохраняем только те, что не лежат в базе
    override val needParcelSavingEntries: ArrayList<AttachmentEntry>
        get() {
            return copyToArrayListWithPredicate(data) {
                // сохраняем только те, что не лежат в базе
                val model = it.attachment
                model !is Upload && it.optionalId == 0
            }
        }

    private fun setupAttachmentsListening() {
        appendDisposable(attachmentsRepository.observeAdding()
            .filter { filterAttachmentEvents(it) }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onRepositoryAttachmentsAdded(it.attachments) })
        appendDisposable(attachmentsRepository.observeRemoving()
            .filter { filterAttachmentEvents(it) }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onRepositoryAttachmentsRemoved(it) })
    }

    private fun setupUploadListening() {
        appendDisposable(uploadManager.observeDeleting(true)
            .observeOn(provideMainThreadScheduler())
            .subscribe {
                onUploadObjectRemovedFromQueue(
                    it
                )
            })
        appendDisposable(uploadManager.observeProgress()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadProgressUpdate(it) })
        appendDisposable(uploadManager.obseveStatus()
            .observeOn(provideMainThreadScheduler())
            .subscribe {
                onUploadStatusUpdate(
                    it
                )
            })
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe {
                onUploadQueueUpdates(
                    it
                ) { t ->
                    isUploadToThis(t)
                }
            })
    }

    private fun isUploadToThis(upload: Upload): Boolean {
        val tPost = post ?: return false
        val dest = upload.destination
        return dest.method == Method.TO_WALL && dest.ownerId == ownerId && dest.id == tPost.dbid
    }

    private fun restoreEditingWallPostFromDbAsync() {
        appendDisposable(walls
            .getEditingPost(accountId, ownerId, editingType, false)
            .fromIOToMain()
            .subscribe({ post -> onPostRestored(post) }) { obj -> obj.printStackTrace() })
    }

    private fun restoreEditingAttachmentsAsync(postDbid: Int) {
        appendDisposable(attachmentsSingle(postDbid)
            .zipWith(
                uploadsSingle(postDbid)
            ) { first: List<AttachmentEntry>, second: List<AttachmentEntry> ->
                combine(
                    first,
                    second
                )
            }
            .fromIOToMain()
            .subscribe({ onAttachmentsRestored(it) }) { obj -> obj.printStackTrace() })
    }

    private fun onPostRestored(post: Post) {
        this.post = post
        checkFriendsOnly(post.isFriendsOnly)
        val postpone = post.postType == VKApiPost.Type.POSTPONE
        setTimerValue(if (postpone) post.date else null)
        setTextBody(post.text)
        if (!links.isNullOrEmpty()) {
            setTextBody(links)
            links = null
        }
        restoreEditingAttachmentsAsync(post.dbid)
    }

    private fun attachmentsSingle(postDbid: Int): Single<List<AttachmentEntry>> {
        return attachmentsRepository
            .getAttachmentsWithIds(accountId, AttachToType.POST, postDbid)
            .map { createFrom(it, true) }
    }

    private fun uploadsSingle(postDbid: Int): Single<List<AttachmentEntry>> {
        val destination = UploadDestination.forPost(postDbid, ownerId)
        return uploadManager[accountId, destination]
            .flatMap {
                Single.just(
                    createFrom(
                        it
                    )
                )
            }
    }

    private fun onAttachmentsRestored(d: List<AttachmentEntry>) {
        if (d.nonNullNoEmpty()) {
            val size = data.size
            data.addAll(d)
            safelyNotifyItemsAdded(size, d.size)
        }
        checkUploadUris()
    }

    private fun onRepositoryAttachmentsRemoved(event: IRemoveEvent) {
        val info = findInfoByPredicate(
            data
        ) {
            it.optionalId == event.generatedId
        }
        if (info != null) {
            val entry = info.second
            val index = info.first
            data.removeAt(index)
            safelyNotifyItemRemoved(index)
            if (entry.attachment is Poll) {
                resolveSupportButtons()
            }
        }
    }

    override fun doUploadPhotos(photos: List<LocalPhoto>, size: Int) {
        val pPost = post ?: return
        val destination = UploadDestination.forPost(pPost.dbid, ownerId)
        uploadManager.enqueue(UploadUtils.createIntents(accountId, destination, photos, size, true))
    }

    override fun doUploadFile(file: String, size: Int) {
        val pPost = post ?: return
        val destination = UploadDestination.forPost(pPost.dbid, ownerId)
        uploadManager.enqueue(UploadUtils.createIntents(accountId, destination, file, size, true))
    }

    private fun filterAttachmentEvents(event: IBaseEvent): Boolean {
        val pPost = post ?: return false
        return event.attachToType == AttachToType.POST && event.accountId == accountId && event.attachToId == pPost.dbid
    }

    private fun onRepositoryAttachmentsAdded(d: List<Pair<Int, AbsModel>>) {
        var pollAdded = false
        val size = data.size
        for (pair in d) {
            val model = pair.second
            if (model is Poll) {
                pollAdded = true
            }
            data.add(AttachmentEntry(true, model).setOptionalId(pair.first))
        }
        safelyNotifyItemsAdded(size, d.size)
        if (pollAdded) {
            resolveSupportButtons()
        }
    }

    override fun onPollCreateClick() {
        view?.openPollCreationWindow(
            accountId,
            ownerId
        )
    }

    override fun onModelsAdded(models: List<AbsModel>) {
        val pPost = post ?: return
        appendDisposable(
            attachmentsRepository.attach(accountId, AttachToType.POST, pPost.dbid, models)
                .subscribeOn(Schedulers.io())
                .subscribe(dummy(), ignore())
        )
    }

    override fun onAttachmentRemoveClick(index: Int, attachment: AttachmentEntry) {
        val pPost = post ?: return
        if (attachment.optionalId != 0) {
            appendDisposable(
                attachmentsRepository.remove(
                    accountId,
                    AttachToType.POST,
                    pPost.dbid,
                    attachment.optionalId
                )
                    .subscribeOn(Schedulers.io())
                    .subscribe(dummy(), ignore())
            )
        } else {
            manuallyRemoveElement(index)
        }
    }

    override fun onTimerClick() {
        val pPost = post ?: return
        if (pPost.postType == VKApiPost.Type.POSTPONE) {
            pPost.setPostType(VKApiPost.Type.POST)
            setTimerValue(null)
            resolveTimerInfoView()
            return
        }
        val initialTime =
            if (pPost.date == 0L) System.currentTimeMillis() / 1000 + 2 * 60 * 60 else pPost.date
        view?.showEnterTimeDialog(
            initialTime
        )
    }

    override fun fireTimerTimeSelected(unixtime: Long) {
        val pPost = post ?: return
        pPost.setPostType(VKApiPost.Type.POSTPONE)
        pPost.setDate(unixtime)
        setTimerValue(unixtime)
    }

    private fun resolveSupportButtons() {
        view?.setSupportedButtons(
            photo = true,
            audio = true,
            video = true,
            doc = true,
            poll = isPollSupported,
            timer = isSupportTimer
        )
    }

    private val isPollSupported: Boolean
        get() {
            for (entry in data) {
                if (entry.attachment is Poll) {
                    return false
                }
            }
            return true
        }
    private val owner: Owner
        get() = attrs.getOwner()
    private val isSupportTimer: Boolean
        get() = if (ownerId > 0) {
            accountId == ownerId
        } else {
            isEditorOrHigher
        }

    private fun changePublishingNowState(publishing: Boolean) {
        publishingNow = publishing
        resolvePublishDialogVisibility()
    }

    private fun resolvePublishDialogVisibility() {
        if (publishingNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.publication,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    private fun commitDataToPost() {
        val pPost = post ?: return
        if (pPost.attachments == null) {
            pPost.setAttachments(Attachments())
        }
        for (entry in data) {
            pPost.attachments?.add(entry.attachment)
        }
        pPost.setText(getTextBody())
        pPost.setFriendsOnly(friendsOnly.get())
    }

    fun fireReadyClick() {
        val pPost = post ?: return
        val destination = UploadDestination.forPost(pPost.dbid, ownerId)
        appendDisposable(uploadManager[accountId, destination]
            .fromIOToMain()
            .subscribe({
                if (it.isNotEmpty()) {
                    view?.showError(R.string.wait_until_file_upload_is_complete)
                } else {
                    doPost()
                }
            }) { it.printStackTrace() })
    }

    private fun doPost() {
        val pPost = post ?: return
        commitDataToPost()
        changePublishingNowState(true)
        val fromGroup = super.fromGroup.get()
        val showSigner = addSignature.get()
        appendDisposable(walls
            .post(accountId, pPost, fromGroup, showSigner)
            .fromIOToMain()
            .subscribe({ onPostPublishSuccess() }) { t ->
                onPostPublishError(
                    t
                )
            })
    }

    private fun onPostPublishSuccess() {
        changePublishingNowState(false)
        releasePostDataAsync()
        postPublished = true
        view?.goBack()
    }

    private fun onPostPublishError(t: Throwable) {
        changePublishingNowState(false)
        showError(getCauseIfRuntime(t))
    }

    private fun releasePostDataAsync() {
        val pPost = post ?: return
        val destination = UploadDestination.forPost(pPost.dbid, ownerId)
        uploadManager.cancelAll(accountId, destination)
        subscribeOnIOAndIgnore(walls.deleteFromCache(accountId, pPost.dbid))
    }

    private fun safeDraftAsync() {
        val pPost = post ?: return
        commitDataToPost()
        subscribeOnIOAndIgnore(walls.cachePostWithIdSaving(accountId, pPost))
    }

    fun onBackPresed(): Boolean {
        if (postPublished) {
            return true
        }
        if (EditingPostType.TEMP == editingType) {
            releasePostDataAsync()
        } else {
            safeDraftAsync()
        }
        return true
    }

    fun fireUriUploadSizeSelected(uris: List<Uri>, size: Int) {
        uploadStreamsImpl(uris, size, false)
    }

    fun fireUriUploadCancelClick() {
        upload = empty()
    }

    init {
        upload = wrap(streams)
        this.mime = mime
        attachmentsRepository = Includes.attachmentsRepository
        walls = Repository.walls
        this.ownerId = ownerId
        this.editingType = editingType
        if (!links.isNullOrEmpty()) this.links = links
        if (savedInstanceState == null && bundle != null) {
            for (i in bundle) {
                data.add(AttachmentEntry(false, i))
            }
        }
        setupAttachmentsListening()
        setupUploadListening()
        restoreEditingWallPostFromDbAsync()

        // только на моей стене
        setFriendsOnlyOptionAvailable(ownerId > 0 && ownerId == accountId)

        // доступно только в группах и только для редакторов и выше
        setFromGroupOptionAvailable(isGroup && isEditorOrHigher)

        // доступно только для публичных страниц(и я одмен) или если нажат "От имени группы"
        setAddSignatureOptionAvailable(isCommunity && isEditorOrHigher || fromGroup.get())
    }
}