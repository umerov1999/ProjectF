package dev.ragnarok.fenrir.fragment.attachments.postedit

import android.os.Bundle
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.attachments.abspostedit.AbsPostEditPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Unixtime.now
import dev.ragnarok.fenrir.util.Utils.copyToArrayListWithPredicate
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.intValueIn

class PostEditPresenter(
    accountId: Int,
    post: Post,
    private val attrs: WallEditorAttrs,
    savedInstanceState: Bundle?
) : AbsPostEditPresenter<IPostEditView>(accountId, savedInstanceState) {
    private val post: Post
    private val uploadDestination: UploadDestination
    private val wallInteractor: IWallsRepository = walls
    private var editingNow = false
    private var canExit = false
    private val owner: Owner
        get() = attrs.getOwner()
    private val me: Owner
        get() = attrs.getEditor()

    override fun onShowAuthorChecked(checked: Boolean) {
        super.onShowAuthorChecked(checked)
        resolveSignerInfoVisibility()
    }

    private val isEditorOrHigher: Boolean
        get() {
            val owner = owner
            return owner is Community && owner.adminLevel >= VKApiCommunity.AdminLevel.EDITOR
        }

    private fun postIsSuggest(): Boolean {
        return post.postType == VKApiPost.Type.SUGGEST
    }

    private fun postIsMine(): Boolean {
        return if (post.creatorId > 0 && post.creatorId == accountId) {
            true
        } else post.signerId > 0 && post.signerId == accountId
    }

    private fun supportSignerInfoDisplaying(): Boolean {
        if (!isAddSignatureOptionAvailable()) {
            return false
        }

        // потому что она может быть недоступна (signer == null)
        return if (postIsSuggest() && !postIsMine()) {
            true
        } else post.creator != null
    }

    override fun onGuiCreated(viewHost: IPostEditView) {
        super.onGuiCreated(viewHost)
        @StringRes val titleRes =
            if (isPublishingSuggestPost) R.string.publication else R.string.editing
        viewHost.setToolbarTitle(getString(titleRes))
        viewHost.setToolbarSubtitle(owner.fullName)
        resolveSignerInfoVisibility()
        resolveProgressDialog()
        resolveSupportButtons()
    }

    private val displayedSigner: Owner?
        get() {
            if (postIsSuggest()) {
                return post.author
            }
            return if (post.creator != null) {
                post.creator
            } else me
        }

    private fun resolveSignerInfoVisibility() {
        val signer = displayedSigner
        view?.let {
            it.displaySignerInfo(signer?.fullName, signer?.get100photoOrSmaller())
            it.setSignerInfoVisible(supportSignerInfoDisplaying() && addSignature.get())
        }
    }

    override val needParcelSavingEntries: ArrayList<AttachmentEntry>
        get() = copyToArrayListWithPredicate(
            data
        ) {
            it.attachment !is Upload
        }

    private fun onUploadComplete(d: Pair<Upload, UploadResult<*>>) {
        val upload = d.first
        val result = d.second
        val index = findUploadIndexById(upload.getObjectId())
        if (index == -1) {
            return
        }
        if (result.result is Photo) {
            val photo = result.result
            data[index] = AttachmentEntry(true, photo)
            view?.notifyItemChanged(index)
        } else {
            data.removeAt(index)
            view?.notifyItemRemoved(index)
        }
    }

    private fun setEditingNow(editingNow: Boolean) {
        this.editingNow = editingNow
        resolveProgressDialog()
    }

    private fun resolveProgressDialog() {
        if (editingNow) {
            view?.displayProgressDialog(
                R.string.please_wait,
                R.string.publication,
                false
            )
        } else {
            view?.dismissProgressDialog()
        }
    }

    // если пост предложенный - постим, если нет - редактируем
    // если пост мой и он предложенный - редактируем
    private val isPublishingSuggestPost: Boolean
        get() =// если пост предложенный - постим, если нет - редактируем
            // если пост мой и он предложенный - редактируем
            postIsSuggest() && !postIsMine()

    private fun save() {
        d(TAG, "save, author: " + post.author + ", signer: " + post.creator)
        appendDisposable(uploadManager[accountId, uploadDestination]
            .fromIOToMain()
            .subscribe({ data ->
                if (data.isEmpty()) {
                    doCommitImpl()
                } else {
                    view?.customToast?.showToastSuccessBottom(
                        R.string.wait_until_file_upload_is_complete
                    )
                }
            }) { it.printStackTrace() })
    }

    private val isGroup: Boolean
        get() = owner is Community && (owner as Community).communityType == VKApiCommunity.Type.GROUP
    private val isCommunity: Boolean
        get() = owner is Community && (owner as Community).communityType == VKApiCommunity.Type.PAGE

    private fun canAddSignature(): Boolean {
        if (!isEditorOrHigher) {
            // только редакторы и выше могу указывать автора
            return false
        }
        return if (isGroup) {
            // если группа - то только, если пост от имени группы
            post.author is Community
        } else isCommunity

        // в публичных страницах всегда можно
    }

    private fun doCommitImpl() {
        if (isPublishingSuggestPost) {
            postImpl()
            return
        }
        val timerCancelled = post.postType == VKApiPost.Type.POSTPONE && getTimerValue() == null
        if (timerCancelled) {
            postImpl()
            return
        }
        saveImpl()
    }

    private fun saveImpl() {
        val publishDate = getTimerValue()
        setEditingNow(true)
        val signed = if (canAddSignature()) addSignature.get() else null
        val friendsOnly = if (isFriendsOnlyOptionAvailable()) super.friendsOnly.get() else null
        appendDisposable(wallInteractor
            .editPost(
                accountId, post.ownerId, post.vkid, friendsOnly,
                getTextBody(), attachmentTokens, null, signed, publishDate,
                null, null, null, null
            )
            .fromIOToMain()
            .subscribe({ onEditResponse() }) { throwable ->
                onEditError(
                    getCauseIfRuntime(throwable)
                )
            })
    }

    private fun postImpl() {
        val publishDate = getTimerValue()
        val body = getTextBody()
        val signed = if (isAddSignatureOptionAvailable()) addSignature.get() else null

        // Эта опция не может быть доступна (так как публикация - исключительно для PAGE)
        val fromGroup: Boolean? = null
        setEditingNow(true)
        appendDisposable(wallInteractor
            .post(
                accountId, post.ownerId, null, fromGroup, body, attachmentTokens, null,
                signed, publishDate, null, null, null, post.vkid, null, null, null
            )
            .fromIOToMain()
            .subscribe({ onEditResponse() }) { throwable ->
                onEditError(
                    getCauseIfRuntime(throwable)
                )
            })
    }

    private fun onEditError(throwable: Throwable) {
        setEditingNow(false)
        showError(throwable)
    }

    private fun onEditResponse() {
        setEditingNow(false)
        canExit = true
        view?.closeAsSuccess()
    }

    private val attachmentTokens: List<AbsModel>
        get() {
            val result: MutableList<AbsModel> = ArrayList()
            for (entry in data) {
                if (entry.attachment is Post) {
                    continue
                }
                result.add(entry.attachment)
            }
            return result
        }

    override fun onAttachmentRemoveClick(index: Int, attachment: AttachmentEntry) {
        manuallyRemoveElement(index)
        if (attachment.attachment is Poll) {
            // because only 1 poll is supported
            resolveSupportButtons()
        }
    }

    override fun doUploadPhotos(photos: List<LocalPhoto>, size: Int) {
        val intents = UploadUtils.createIntents(accountId, uploadDestination, photos, size, false)
        uploadManager.enqueue(intents)
    }

    override fun doUploadFile(file: String, size: Int) {
        val intents = UploadUtils.createIntents(accountId, uploadDestination, file, size, false)
        uploadManager.enqueue(intents)
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_POST, post)
    }

    private fun resolveSupportButtons() {
        if (post.hasCopyHierarchy()) {
            view?.setSupportedButtons(
                photo = false,
                audio = false,
                video = false,
                doc = false,
                poll = false,
                timer = false
            )
        } else {
            view?.setSupportedButtons(
                photo = true,
                audio = true,
                video = true,
                doc = true,
                poll = isPollSupported,
                timer = supportTimer()
            )
        }
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

    private fun supportTimer(): Boolean {
        return if (owner is Community && (owner as Community).adminLevel < VKApiCommunity.AdminLevel.EDITOR) {
            // если сообщество и я не одмен, то нет
            false
        } else intValueIn(
            post.postType,
            VKApiPost.Type.POSTPONE,
            VKApiPost.Type.SUGGEST
        )
    }

    fun fireReadyClick() {
        save()
    }

    fun onBackPressed(): Boolean {
        if (canExit) {
            return true
        }
        view?.showConfirmExitDialog()
        return false
    }

    override fun onTimerClick() {
        if (!supportTimer()) {
            return
        }
        if (getTimerValue() != null) {
            setTimerValue(null)
            return
        }
        val initialDate = now() + 24 * 60 * 60
        view?.showEnterTimeDialog(initialDate)
    }

    override fun fireTimerTimeSelected(unixtime: Long) {
        if (!validatePublishDate(unixtime)) {
            view?.showError(R.string.date_is_invalid)
            return
        }
        setTimerValue(unixtime)
    }

    override fun onPollCreateClick() {
        view?.openPollCreationWindow(
            accountId,
            post.ownerId
        )
    }

    fun fireExitWithSavingConfirmed() {
        save()
    }

    fun fireExitWithoutSavingClick() {
        canExit = true
        uploadManager.cancelAll(accountId, uploadDestination)
        view?.closeAsSuccess()
    }

    companion object {
        private val TAG = PostEditPresenter::class.java.simpleName
        private const val SAVE_POST = "save_post"
        internal fun safelyClone(post: Post): Post {
            return try {
                post.clone()
            } catch (e: CloneNotSupportedException) {
                throw IllegalArgumentException("Unable to clone post")
            }
        }

        internal fun validatePublishDate(unixtime: Long): Boolean {
            return now() < unixtime
        }
    }

    init {
        if (savedInstanceState?.getParcelableCompat<Post>(SAVE_POST) == null) {
            this.post = safelyClone(post)
            setTextBody(post.text)
            if (post.postType == VKApiPost.Type.POSTPONE) {
                setTimerValue(post.date)
            }
            post.attachments.requireNonNull {
                val list: List<AbsModel> = it.toList()
                for (model in list) {
                    data.add(AttachmentEntry(true, model))
                }
            }
            if (post.hasCopyHierarchy()) {
                post.getCopyHierarchy()?.get(0)?.let { AttachmentEntry(false, it) }
                    ?.let { data.add(0, it) }
            }
        } else {
            this.post = savedInstanceState.getParcelableCompat(SAVE_POST)!!
        }
        val owner = owner
        setFriendsOnlyOptionAvailable(owner.ownerId > 0 && owner.ownerId == accountId)
        checkFriendsOnly(post.isFriendsOnly)
        setAddSignatureOptionAvailable(canAddSignature())
        addSignature.setValue(post.signerId > 0)
        setFromGroupOptionAvailable(false) // only for publishing
        uploadDestination = UploadDestination.forPost(post.vkid, post.ownerId)
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe { it ->
                onUploadQueueUpdates(
                    it
                ) {
                    it.accountId == accountId
                            && it.destination.compareTo(uploadDestination)
                }
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
        appendDisposable(uploadManager.observeDeleting(false)
            .observeOn(provideMainThreadScheduler())
            .subscribe {
                onUploadObjectRemovedFromQueue(
                    it
                )
            })
        appendDisposable(uploadManager.observeResults()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadComplete(it) })
    }
}