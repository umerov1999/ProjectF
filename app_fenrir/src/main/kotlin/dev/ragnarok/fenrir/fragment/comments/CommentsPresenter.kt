package dev.ragnarok.fenrir.fragment.comments

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Includes.attachmentsRepository
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IAddEvent
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.IBaseEvent
import dev.ragnarok.fenrir.domain.ICommentsInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IStickersInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.CommentsInteractor
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fragment.comments.ICommentsView.ICommentContextView
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.DisposableHolder
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CommentsPresenter(
    private var authorId: Long,
    commented: Commented,
    focusToComment: Int?,
    context: Context,
    CommentThread: Int?,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<ICommentsView>(
    authorId, savedInstanceState
) {
    private val commented: Commented
    private val ownersRepository: IOwnersRepository = owners
    private val interactor: ICommentsInteractor
    private val stickersInteractor: IStickersInteractor
    private val data: MutableList<Comment>
    private val CommentThread: Int?
    private val context: Context
    private val stickersWordsDisplayDisposable = DisposableHolder<Void>()
    private val actualLoadingDisposable = CompositeDisposable()
    private val deepLookingHolder = DisposableHolder<Void>()
    private val cacheLoadingDisposable = CompositeDisposable()
    private var focusToComment: Int?
    private var commentedState: CommentedState? = null
    private var author: Owner? = null
    private var directionDesc: Boolean
    private var loadingState = 0
    private var adminLevel = 0
    private var draftCommentBody: String? = null
    private var draftCommentAttachmentsCount = 0
    private var draftCommentId: Int? = null
    private var replyTo: Comment? = null
    private var sendingNow = false
    private var topicPoll: Poll? = null
    private var loadingAvailableAuthorsNow = false
    private fun loadAuthorData() {
        val accountId = authorId
        appendDisposable(ownersRepository.getBaseOwnerInfo(
            accountId,
            authorId,
            IOwnersRepository.MODE_ANY
        )
            .fromIOToMain()
            .subscribe({ owner -> onAuthorDataReceived(owner) }) { t ->
                onAuthorDataGetError(
                    t
                )
            })
    }

    private fun resolveAuthorAvatarView() {
        val avatarUrl =
            if (author != null) if (author is User) (author as User).photo50 else (author as Community).photo50 else null
        view?.displayAuthorAvatar(avatarUrl)
    }

    private fun onAuthorDataGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun onAuthorDataReceived(owner: Owner) {
        author = owner
        resolveAuthorAvatarView()
    }

    private fun onCommentMinorUpdate(update: CommentUpdate) {
        for (i in data.indices) {
            val comment = data[i]
            if (comment.getObjectId() == update.getCommentId()) {
                applyUpdate(comment, update)
                view?.notifyItemChanged(i)
                return
            } else if (comment.hasThreads()) {
                for (s in comment.threads.orEmpty()) {
                    if (s.getObjectId() == update.getCommentId()) {
                        applyUpdate(s, update)
                        view?.notifyItemChanged(
                            i
                        )
                        return
                    }
                }
            }
        }
    }

    private fun applyUpdate(comment: Comment, update: CommentUpdate) {
        update.getLikeUpdate().requireNonNull {
            comment.setLikesCount(it.getCount())
            comment.setUserLikes(it.isUserLikes())
        }
        update.getDeleteUpdate().requireNonNull {
            comment.setDeleted(it.isDeleted())
        }
    }

    fun resetDraftMessage() {
        draftCommentAttachmentsCount = 0
        draftCommentBody = null
        draftCommentId = null
        replyTo = null
        resolveAttachmentsCounter()
        resolveBodyView()
        resolveReplyViews()
        resolveSendButtonAvailability()
        resolveEmptyTextVisibility()
    }

    fun fireTextEdited(s: String?) {
        if (!Settings.get().other().isHint_stickers) {
            return
        }
        stickersWordsDisplayDisposable.dispose()
        if (s.isNullOrEmpty()) {
            view?.updateStickers(
                emptyList()
            )
            return
        }
        stickersWordsDisplayDisposable.append(stickersInteractor.getKeywordsStickers(
            authorId,
            s.trim { it <= ' ' })
            .delay(500, TimeUnit.MILLISECONDS)
            .fromIOToMain()
            .subscribe({
                view?.updateStickers(
                    it
                )
            }) { u ->
                showError(u)
            })
    }

    private fun onAttchmentRemoveEvent() {
        draftCommentAttachmentsCount--
        onAttachmentCountChanged()
    }

    private fun onAttachmentCountChanged() {
        resolveSendButtonAvailability()
        resolveAttachmentsCounter()
    }

    private fun onAttchmentAddEvent(event: IAddEvent) {
        draftCommentAttachmentsCount += event.attachments.size
        onAttachmentCountChanged()
    }

    private fun filterAttachmentEvent(event: IBaseEvent): Boolean {
        return draftCommentId != null && event.attachToType == AttachToType.COMMENT && event.accountId == authorId && event.attachToId == draftCommentId
    }

    private fun restoreDraftCommentSync() {
        val draft = interactor.restoreDraftComment(authorId, commented)
            ?.blockingGet()
        if (draft != null) {
            draftCommentBody = draft.getBody()
            draftCommentAttachmentsCount = draft.getAttachmentsCount()
            draftCommentId = draft.getId()
        }
    }

    private fun requestInitialData() {
        val accountId = authorId
        val single: Single<CommentsBundle> = when {
            focusToComment != null -> {
                interactor.getCommentsPortion(
                    accountId,
                    commented,
                    -10,
                    COUNT,
                    focusToComment,
                    CommentThread,
                    true,
                    "asc"
                )
            }

            directionDesc -> {
                interactor.getCommentsPortion(
                    accountId,
                    commented,
                    0,
                    COUNT,
                    null,
                    CommentThread,
                    true,
                    "desc"
                )
            }

            else -> {
                interactor.getCommentsPortion(
                    accountId,
                    commented,
                    0,
                    COUNT,
                    null,
                    CommentThread,
                    true,
                    "asc"
                )
            }
        }
        setLoadingState(LoadingState.INITIAL)
        actualLoadingDisposable.add(single
            .fromIOToMain()
            .subscribe({ bundle -> onInitialDataReceived(bundle) }) { throwable ->
                onInitialDataError(
                    throwable
                )
            })
    }

    private fun onInitialDataError(throwable: Throwable) {
        setLoadingState(LoadingState.NO)
        showError(getCauseIfRuntime(throwable))
    }

    private fun loadUp() {
        if (loadingState != LoadingState.NO) return
        val first = firstCommentInList ?: return
        val accountId = authorId
        setLoadingState(LoadingState.UP)
        actualLoadingDisposable.add(interactor.getCommentsPortion(
            accountId,
            commented,
            1,
            COUNT,
            first.getObjectId(),
            CommentThread,
            false,
            "desc"
        )
            .fromIOToMain()
            .subscribe(
                { bundle: CommentsBundle -> onCommentsPortionPortionReceived(bundle) }
            ) { throwable -> onCommentPortionError(getCauseIfRuntime(throwable)) })
    }

    private fun loadDown() {
        if (loadingState != LoadingState.NO) return
        val last = lastCommentInList ?: return
        val accountId = authorId
        setLoadingState(LoadingState.DOWN)
        actualLoadingDisposable.add(interactor.getCommentsPortion(
            accountId,
            commented,
            0,
            COUNT,
            last.getObjectId(),
            CommentThread,
            false,
            "asc"
        )
            .fromIOToMain()
            .subscribe(
                { bundle: CommentsBundle -> onCommentsPortionPortionReceived(bundle) }
            ) { throwable -> onCommentPortionError(getCauseIfRuntime(throwable)) })
    }

    private fun onCommentPortionError(throwable: Throwable) {
        setLoadingState(LoadingState.NO)
        showError(throwable)
    }

    private fun onCommentsPortionPortionReceived(bundle: CommentsBundle) {
        cacheLoadingDisposable.clear()
        val comments = bundle.getComments()
        when (loadingState) {
            LoadingState.UP -> {
                data.addAll(comments)
                view?.notifyDataAddedToTop(
                    comments.size
                )
            }

            LoadingState.DOWN -> {
                if (comments.nonNullNoEmpty()) {
                    comments.removeAt(comments.size - 1) // последним комментарием приходит комментарий к кодом, который был передан в startCommentId
                }
                if (comments.nonNullNoEmpty()) {
                    data.addAll(0, comments)
                    view?.notifyDataAddedToBottom(
                        comments.size
                    )
                }
            }
        }
        commentedState = CommentedState(bundle.getFirstCommentId(), bundle.getLastCommentId())
        updateAdminLevel(bundle.getAdminLevel().orZero())
        setLoadingState(LoadingState.NO)
    }

    private fun updateAdminLevel(newValue: Int) {
        adminLevel = newValue
        resolveCanSendAsAdminView()
    }

    private fun canDelete(comment: Comment): Boolean {
        val currentSessionUserId = authorId
        val author = comment.author

        // если комментарий от имени сообщества и я админ или модератор, то могу удалить
        return if (author is Community && author.adminLevel >= VKApiCommunity.AdminLevel.MODERATOR) {
            true
        } else comment.fromId == currentSessionUserId || commented.sourceOwnerId == currentSessionUserId || adminLevel >= VKApiCommunity.AdminLevel.MODERATOR
    }

    private fun canEdit(comment: Comment): Boolean {
        // нельзя редактировать комментарий со стикером
        return !comment.hasStickerOnly() && comment.isCanEdit

        /*int myUserId = getAccountId();

        if (isTopic()) {
            // если я одмен или автор коммента в топике - я могу
            // редактировать в любое время
            return myUserId == comment.getFromId() || adminLevel == VKApiCommunity.AdminLevel.ADMIN;
        } else {
            // в обратном случае у меня есть только 24 часа
            // и я должен быть автором либо админом
            boolean canEditAsAdmin = ownerIsCommunity() && comment.getFromId() == commented.getSourceOwnerId() && adminLevel == VKApiCommunity.AdminLevel.ADMIN;
            boolean canPotencialEdit = myUserId == comment.getFromId() || canEditAsAdmin;

            long currentUnixtime = new Date().getTime() / 1000;
            long max24 = 24 * 60 * 60;
            return canPotencialEdit && (currentUnixtime - comment.getDate()) < max24;
        }*/
    }

    private fun setLoadingState(loadingState: Int) {
        this.loadingState = loadingState
        resolveEmptyTextVisibility()
        resolveHeaderFooterViews()
        resolveCenterProgressView()
    }

    private fun resolveEmptyTextVisibility() {
        view?.setEpmtyTextVisible(loadingState == LoadingState.NO && data.isEmpty())
    }

    private fun resolveHeaderFooterViews() {
        if (data.isEmpty()) {
            // если комментариев к этому обьекту нет, то делать хидеры невидимыми
            view?.setupLoadUpHeader(
                LoadMoreState.INVISIBLE
            )
            view?.setupLoadDownFooter(
                LoadMoreState.INVISIBLE
            )
            return
        }
        val lastResponseAvailable = commentedState != null
        if (!lastResponseAvailable) {
            // если мы еще не получили с сервера информацию о количестве комеентов, то делать хидеры невидимыми
            view?.setupLoadUpHeader(
                LoadMoreState.END_OF_LIST
            )
            view?.setupLoadDownFooter(
                LoadMoreState.END_OF_LIST
            )
            return
        }
        when (loadingState) {
            LoadingState.NO -> {
                view?.setupLoadUpHeader(if (isCommentsAvailableUp) LoadMoreState.CAN_LOAD_MORE else LoadMoreState.END_OF_LIST)
                view?.setupLoadDownFooter(if (isCommentsAvailableDown) LoadMoreState.CAN_LOAD_MORE else LoadMoreState.END_OF_LIST)
            }

            LoadingState.DOWN -> {
                view?.setupLoadDownFooter(
                    LoadMoreState.LOADING
                )
                view?.setupLoadUpHeader(
                    LoadMoreState.END_OF_LIST
                )
            }

            LoadingState.UP -> {
                view?.setupLoadDownFooter(
                    LoadMoreState.END_OF_LIST
                )
                view?.setupLoadUpHeader(
                    LoadMoreState.LOADING
                )
            }

            LoadingState.INITIAL -> {
                view?.setupLoadDownFooter(
                    LoadMoreState.END_OF_LIST
                )
                view?.setupLoadUpHeader(
                    LoadMoreState.END_OF_LIST
                )
            }
        }
    }

    private val isCommentsAvailableUp: Boolean
        get() {
            if (commentedState?.firstCommentId == null) {
                return false
            }
            val fisrt = firstCommentInList
            return fisrt?.getObjectId().orZero() > commentedState?.firstCommentId.orZero()
        }
    private val isCommentsAvailableDown: Boolean
        get() {
            if (commentedState?.lastCommentId == null) {
                return false
            }
            val last = lastCommentInList
            return last?.getObjectId().orZero() < commentedState?.lastCommentId.orZero()
        }
    private val firstCommentInList: Comment?
        get() = if (data.nonNullNoEmpty()) data[data.size - 1] else null

    private fun resolveCenterProgressView() {
        view?.setCenterProgressVisible(
            loadingState == LoadingState.INITIAL && data.isEmpty()
        )
    }

    private val lastCommentInList: Comment?
        get() = if (data.nonNullNoEmpty()) data[0] else null

    private fun resolveBodyView() {
        view?.displayBody(draftCommentBody)
    }

    private fun canSendComment(): Boolean {
        return draftCommentAttachmentsCount > 0 || draftCommentBody.trimmedNonNullNoEmpty()
    }

    private fun resolveSendButtonAvailability() {
        view?.setButtonSendAvailable(
            canSendComment()
        )
    }

    private fun saveSingle(): Single<Int> {
        val accountId = authorId
        val replyToComment = replyTo?.getObjectId() ?: 0
        val replyToUser = replyTo?.fromId ?: 0
        return interactor.safeDraftComment(
            accountId,
            commented,
            draftCommentBody,
            replyToComment,
            replyToUser
        )
    }

    private fun saveDraftSync(): Int {
        return saveSingle().blockingGet()
    }

    private fun resolveAttachmentsCounter() {
        view?.displayAttachmentsCount(
            draftCommentAttachmentsCount
        )
    }

    fun fireInputTextChanged(s: String?) {
        val canSend = canSendComment()
        draftCommentBody = s
        if (canSend != canSendComment()) {
            resolveSendButtonAvailability()
        }
    }

    fun fireReplyToOwnerClick(commentId: Int) {
        for (y in data.indices) {
            val comment = data[y]
            if (comment.getObjectId() == commentId) {
                comment.setAnimationNow(true)
                view?.let {
                    it.notifyItemChanged(y)
                    it.moveFocusTo(
                        y,
                        true
                    )
                }
                return
            } else if (comment.hasThreads()) {
                for (s in comment.threads.orEmpty()) {
                    if (s.getObjectId() == commentId) {
                        s.setAnimationNow(true)
                        view?.let {
                            it.notifyItemChanged(y)
                            it.moveFocusTo(y, true)
                        }
                        return
                    }
                }
            }
        }

        //safeShowToast(getView(), R.string.the_comment_is_not_in_the_list, false);
        startDeepCommentFinding(commentId)
    }

    private fun startDeepCommentFinding(commentId: Int) {
        if (loadingState != LoadingState.NO) {
            // не грузить, если сейчас что-то грузится
            return
        }
        val older = firstCommentInList
        val accountId = authorId
        view?.displayDeepLookingCommentProgress()
        deepLookingHolder.append(interactor.getAllCommentsRange(
            accountId,
            commented,
            older?.getObjectId() ?: 0,
            commentId
        )
            .fromIOToMain()
            .subscribe({ comments ->
                onDeepCommentLoadingResponse(
                    commentId,
                    comments
                )
            }) { throwable -> onDeepCommentLoadingError(throwable) })
    }

    private fun onDeepCommentLoadingError(throwable: Throwable) {
        view?.dismissDeepLookingCommentProgress()
        if (throwable is NotFoundException) {
            view?.showError(
                R.string.the_comment_is_not_in_the_list
            )
        } else {
            showError(throwable)
        }
    }

    public override fun onGuiDestroyed() {
        deepLookingHolder.dispose()
        super.onGuiDestroyed()
    }

    fun fireDeepLookingCancelledByUser() {
        deepLookingHolder.dispose()
    }

    private fun onDeepCommentLoadingResponse(commentId: Int, comments: List<Comment>) {
        view?.dismissDeepLookingCommentProgress()
        data.addAll(comments)
        var index = -1
        for (i in data.indices) {
            val comment = data[i]
            if (comment.getObjectId() == commentId) {
                index = i
                comment.setAnimationNow(true)
                break
            }
        }
        if (index == -1) {
            return
        }
        view?.notifyDataAddedToTop(
            comments.size
        )
        val finalIndex = index
        view?.moveFocusTo(
            finalIndex,
            false
        )
    }

    fun fireAttachClick() {
        if (draftCommentId == null) {
            draftCommentId = saveDraftSync()
        }
        val accountId = authorId
        view?.openAttachmentsManager(
            accountId,
            draftCommentId ?: return,
            commented.sourceOwnerId,
            draftCommentBody
        )
    }

    fun fireEditBodyResult(newBody: String?) {
        draftCommentBody = newBody
        resolveSendButtonAvailability()
        resolveBodyView()
    }

    fun fireReplyToCommentClick(comment: Comment) {
        if (commented.sourceType == CommentedType.TOPIC) {
            // в топиках механизм ответа отличается
            val replyText = buildReplyTextFor(comment)
            view?.replaceBodySelectionTextTo(
                replyText
            )
        } else {
            replyTo = comment
            resolveReplyViews()
        }
    }

    fun fireReplyToCommentClick(index: Int) {
        if (data.size <= index || index < 0) {
            return
        }
        val comment = data[index]
        if (commented.sourceType == CommentedType.TOPIC) {
            // в топиках механизм ответа отличается
            val replyText = buildReplyTextFor(comment)
            view?.replaceBodySelectionTextTo(
                replyText
            )
        } else {
            replyTo = comment
            resolveReplyViews()
        }
    }

    fun fireReport(comment: Comment) {
        val items = arrayOf<CharSequence>(
            "Спам",
            "Детская порнография",
            "Экстремизм",
            "Насилие",
            "Пропаганда наркотиков",
            "Материал для взрослых",
            "Оскорбление",
            "Призывы к суициду"
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.report)
            .setItems(items) { dialog: DialogInterface, item: Int ->
                appendDisposable(interactor.reportComment(
                    authorId, comment.fromId, comment.getObjectId(), item
                )
                    .fromIOToMain()
                    .subscribe({ p ->
                        if (p == 1) view?.customToast?.showToast(
                            R.string.success
                        )
                        else view?.customToast?.showToast(
                            R.string.error
                        )
                    }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
                dialog.dismiss()
            }
            .show()
    }

    fun fireWhoLikesClick(comment: Comment) {
        view?.goToLikes(
            authorId,
            getApiCommentType(comment),
            commented.sourceOwnerId,
            comment.getObjectId()
        )
    }

    fun fireReplyToChat(comment: Comment) {
        startForSendAttachments(
            context, authorId, WallReply().buildFromComment(
                comment, commented
            )
        )
    }

    private fun getApiCommentType(comment: Comment): String {
        return when (comment.commented.sourceType) {
            CommentedType.PHOTO -> "photo_comment"
            CommentedType.POST -> "comment"
            CommentedType.VIDEO -> "video_comment"
            CommentedType.TOPIC -> "topic_comment"
            else -> throw IllegalArgumentException()
        }
    }

    fun fireSendClick() {
        sendNormalComment()
    }

    private fun sendNormalComment() {
        setSendingNow(true)
        val accountId = authorId
        val intent = createCommentIntent()
        if (intent.getReplyToComment() == null && CommentThread != null) intent.setReplyToComment(
            CommentThread
        )
        appendDisposable(interactor.send(accountId, commented, CommentThread, intent)
            .fromIOToMain()
            .subscribe({ onNormalSendResponse() }) { t ->
                onSendError(
                    t
                )
            })
    }

    private fun sendQuickComment(intent: CommentIntent) {
        setSendingNow(true)
        if (intent.getReplyToComment() == null && CommentThread != null) intent.setReplyToComment(
            CommentThread
        )
        val accountId = authorId
        appendDisposable(interactor.send(accountId, commented, CommentThread, intent)
            .fromIOToMain()
            .subscribe({ onQuickSendResponse() }) { t ->
                onSendError(
                    t
                )
            })
    }

    private fun onSendError(t: Throwable) {
        setSendingNow(false)
        showError(getCauseIfRuntime(t))
    }

    private fun onQuickSendResponse() {
        setSendingNow(false)
        handleCommentAdded()
        replyTo = null
        resolveReplyViews()
        resolveEmptyTextVisibility()
    }

    private fun handleCommentAdded() {
        view?.showCommentSentToast()
        fireRefreshClick()
    }

    private fun onNormalSendResponse() {
        setSendingNow(false)
        handleCommentAdded()
        draftCommentAttachmentsCount = 0
        draftCommentBody = null
        draftCommentId = null
        replyTo = null
        resolveAttachmentsCounter()
        resolveBodyView()
        resolveReplyViews()
        resolveSendButtonAvailability()
        resolveEmptyTextVisibility()
    }

    private fun createCommentIntent(): CommentIntent {
        val replyToComment = replyTo?.getObjectId()
        val body = draftCommentBody
        return CommentIntent(authorId)
            .setMessage(body)
            .setReplyToComment(replyToComment)
            .setDraftMessageId(draftCommentId)
    }

    private fun setSendingNow(sendingNow: Boolean) {
        this.sendingNow = sendingNow
        resolveProgressDialog()
    }

    private fun resolveProgressDialog() {
        when {
            sendingNow -> {
                view?.displayProgressDialog(
                    R.string.please_wait,
                    R.string.publication,
                    false
                )
            }

            loadingAvailableAuthorsNow -> {
                view?.displayProgressDialog(
                    R.string.please_wait,
                    R.string.getting_list_loading_message,
                    false
                )
            }

            else -> {
                view?.dismissProgressDialog()
            }
        }
    }

    fun fireCommentContextViewCreated(view: ICommentContextView, comment: Comment) {
        view.setCanDelete(canDelete(comment))
        view.setCanEdit(canEdit(comment))
        view.setCanBan(canBanAuthor(comment))
    }

    private fun canBanAuthor(comment: Comment): Boolean {
        return comment.fromId > 0 // только пользователей
                && comment.fromId != authorId // не блокируем себя
                && adminLevel >= VKApiCommunity.AdminLevel.MODERATOR // только если я модератор и выше
    }

    fun fireCommentDeleteClick(comment: Comment) {
        deleteRestoreInternal(comment.getObjectId(), true)
    }

    private fun deleteRestoreInternal(commentId: Int, delete: Boolean) {
        val accountId = authorId
        appendDisposable(interactor.deleteRestore(accountId, commented, commentId, delete)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    fun fireCommentEditClick(comment: Comment) {
        val accountId = authorId
        view?.goToCommentEdit(
            accountId,
            comment,
            CommentThread
        )
    }

    fun fireCommentLikeClick(comment: Comment, add: Boolean) {
        likeInternal(add, comment)
    }

    private fun likeInternal(add: Boolean, comment: Comment) {
        if (Settings.get().other().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        val accountId = authorId
        appendDisposable(interactor.like(accountId, comment.commented, comment.getObjectId(), add)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    fun fireCommentRestoreClick(commentId: Int) {
        deleteRestoreInternal(commentId, false)
    }

    fun fireStickerClick(sticker: Sticker) {
        val intent = CommentIntent(authorId)
            .setReplyToComment(replyTo?.getObjectId())
            .setStickerId(sticker.id)
        sendQuickComment(intent)
    }

    fun fireGotoSourceClick() {
        when (commented.sourceType) {
            CommentedType.PHOTO -> {
                val photo = Photo()
                    .setOwnerId(commented.sourceOwnerId)
                    .setId(commented.sourceId)
                    .setAccessKey(commented.accessKey)
                firePhotoClick(singletonArrayList(photo), 0, true)
            }

            CommentedType.POST -> view?.goToWallPost(
                authorId, commented.sourceId, commented.sourceOwnerId
            )

            CommentedType.VIDEO -> view?.goToVideoPreview(
                authorId, commented.sourceId, commented.sourceOwnerId
            )

            CommentedType.TOPIC -> {}
        }
    }

    fun fireTopicPollClick() {
        topicPoll?.let { firePollClick(it) }
    }

    fun fireRefreshClick() {
        if (loadingState != LoadingState.INITIAL) {
            actualLoadingDisposable.clear()
            requestInitialData()
        }
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveOptionMenu()
    }

    private fun resolveOptionMenu() {
        val hasPoll = topicPoll != null
        val hasGotoSource = commented.sourceType != CommentedType.TOPIC
        @StringRes var gotoSourceText: Int? = null
        if (hasGotoSource) {
            when (commented.sourceType) {
                CommentedType.PHOTO -> gotoSourceText = R.string.go_to_photo
                CommentedType.VIDEO -> gotoSourceText = R.string.go_to_video
                CommentedType.POST -> gotoSourceText = R.string.go_to_post
                CommentedType.TOPIC -> {}
            }
        }
        val finalGotoSourceText = gotoSourceText
        resumedView?.setupOptionMenu(
            hasPoll,
            hasGotoSource,
            finalGotoSourceText
        )
    }

    fun fireCommentEditResult(comment: Comment) {
        if (commented == comment.commented) {
            for (i in data.indices) {
                if (data[i].getObjectId() == comment.getObjectId()) {
                    data[i] = comment
                    view?.notifyItemChanged(i)
                    break
                }
            }
        }
    }

    fun fireBanClick(comment: Comment) {
        val user = (comment.author as User?) ?: return
        val groupId = abs(commented.sourceOwnerId)
        view?.banUser(
            authorId,
            groupId,
            user
        )
    }

    private fun setLoadingAvailableAuthorsNow(loadingAvailableAuthorsNow: Boolean) {
        this.loadingAvailableAuthorsNow = loadingAvailableAuthorsNow
        resolveProgressDialog()
    }

    fun fireSendLongClick() {
        setLoadingAvailableAuthorsNow(true)
        val accountId = authorId
        val canSendFromAnyGroup = commented.sourceType == CommentedType.POST
        val single: Single<List<Owner>> = if (canSendFromAnyGroup) {
            interactor.getAvailableAuthors(accountId)
        } else {
            val ids: MutableSet<Long> = HashSet()
            ids.add(accountId)
            ids.add(commented.sourceOwnerId)
            ownersRepository.findBaseOwnersDataAsList(accountId, ids, IOwnersRepository.MODE_ANY)
        }
        appendDisposable(single
            .fromIOToMain()
            .subscribe({ owners -> onAvailableAuthorsReceived(owners) }) {
                onAvailableAuthorsGetError()
            })
    }

    private fun onAvailableAuthorsGetError() {
        setLoadingAvailableAuthorsNow(false)
    }

    private fun onAvailableAuthorsReceived(owners: List<Owner>) {
        setLoadingAvailableAuthorsNow(false)
        view?.showAuthorSelectDialog(
            owners
        )
    }

    fun fireAuthorSelected(owner: Owner) {
        author = owner
        authorId = owner.ownerId
        resolveAuthorAvatarView()
    }

    fun fireDirectionChanged() {
        data.clear()
        view?.notifyDataSetChanged()
        directionDesc = Settings.get().other().isCommentsDesc
        requestInitialData()
    }

    private fun checkFocusToCommentDone() {
        if (focusToComment != null) {
            for (i in data.indices) {
                val comment = data[i]
                if (comment.getObjectId() == focusToComment) {
                    comment.setAnimationNow(true)
                    focusToComment = null
                    view?.moveFocusTo(
                        i,
                        false
                    )
                    break
                }
            }
        }
    }

    private fun onInitialDataReceived(bundle: CommentsBundle) {
        // отменяем загрузку из БД если активна
        cacheLoadingDisposable.clear()
        data.clear()
        data.addAll(bundle.getComments())
        commentedState = CommentedState(bundle.getFirstCommentId(), bundle.getLastCommentId())
        updateAdminLevel(bundle.getAdminLevel().orZero())

        // init poll once
        topicPoll = bundle.getTopicPoll()
        setLoadingState(LoadingState.NO)
        view?.notifyDataSetChanged()
        if (focusToComment != null) {
            checkFocusToCommentDone()
        } else if (!directionDesc) {
            view?.scrollToPosition(data.size - 1)
        }
        resolveOptionMenu()
        resolveHeaderFooterViews()
    }

    private fun loadCachedData() {
        val accountId = authorId
        cacheLoadingDisposable.add(
            interactor.getAllCachedData(accountId, commented)
                .fromIOToMain()
                .subscribe({ onCachedDataReceived(it) }, {
                    requestInitialData()
                })
        )
    }

    private fun resolveCanSendAsAdminView() {
        view?.setCanSendSelectAuthor(commented.sourceType == CommentedType.POST || adminLevel >= VKApiCommunity.AdminLevel.MODERATOR)
    }

    override fun onGuiCreated(viewHost: ICommentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
        viewHost.setToolbarTitle(getString(R.string.comments))
        when (commented.sourceType) {
            CommentedType.POST -> viewHost.setToolbarSubtitle(getString(R.string.for_wall_post))
            CommentedType.PHOTO -> viewHost.setToolbarSubtitle(getString(R.string.for_photo))
            CommentedType.VIDEO -> viewHost.setToolbarSubtitle(getString(R.string.for_video))
            CommentedType.TOPIC -> viewHost.setToolbarSubtitle(getString(R.string.for_topic))
        }
        resolveCanSendAsAdminView()
        resolveReplyViews()
        checkFocusToCommentDone()
        resolveEmptyTextVisibility()
        resolveProgressDialog()
        resolveAttachmentsCounter()
        resolveSendButtonAvailability()
        resolveAuthorAvatarView()
        resolveBodyView()
        resolveHeaderFooterViews()
        resolveCenterProgressView()
    }

    private fun onCachedDataReceived(comments: List<Comment>) {
        data.clear()
        data.addAll(comments)
        resolveHeaderFooterViews()
        resolveEmptyTextVisibility()
        resolveCenterProgressView()
        view?.notifyDataSetChanged()
        if (!directionDesc) {
            view?.scrollToPosition(data.size - 1)
        }
        requestInitialData()
    }

    @SuppressLint("CheckResult")
    override fun onDestroyed() {
        cacheLoadingDisposable.dispose()
        actualLoadingDisposable.dispose()
        deepLookingHolder.dispose()
        stickersWordsDisplayDisposable.dispose()

        // save draft async
        saveSingle().subscribeOn(Schedulers.io()).subscribe(ignore(), ignore())
        super.onDestroyed()
    }

    private fun resolveReplyViews() {
        view?.setupReplyViews(replyTo?.fullAuthorName)
    }

    fun fireReplyCancelClick() {
        replyTo = null
        resolveReplyViews()
    }

    fun fireUpLoadMoreClick() {
        loadUp()
    }

    fun fireDownLoadMoreClick() {
        loadDown()
    }

    fun fireScrollToTop() {
        if (isCommentsAvailableUp) {
            loadUp()
        }
    }

    private class CommentedState(
        val firstCommentId: Int?,
        var lastCommentId: Int?
    )

    private object LoadingState {
        const val NO = 0
        const val INITIAL = 1
        const val UP = 2
        const val DOWN = 3
    }

    companion object {
        private const val COUNT = 20
        private const val REPLY_PATTERN = "[post%s|%s], "
        internal fun buildReplyTextFor(comment: Comment): String {
            val name =
                if (comment.fromId > 0) (comment.author as User).firstName else (comment.author as Community).fullName
            return String.format(REPLY_PATTERN, comment.getObjectId(), name)
        }
    }

    init {
        interactor = CommentsInteractor(networkInterfaces, stores, owners)
        stickersInteractor = InteractorFactory.createStickersInteractor()
        this.commented = commented
        this.focusToComment = focusToComment
        this.context = context
        directionDesc = Settings.get().other().isCommentsDesc
        this.CommentThread = CommentThread
        data = ArrayList()
        val attachmentsRepository = attachmentsRepository
        appendDisposable(attachmentsRepository
            .observeAdding()
            .filter { filterAttachmentEvent(it) }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onAttchmentAddEvent(it) })
        appendDisposable(attachmentsRepository
            .observeRemoving()
            .filter { filterAttachmentEvent(it) }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onAttchmentRemoveEvent() })
        appendDisposable(stores
            .comments()
            .observeMinorUpdates()
            .filter { it.getCommented() == commented }
            .observeOn(provideMainThreadScheduler())
            .subscribe({ update -> onCommentMinorUpdate(update) }) { it.printStackTrace() })
        restoreDraftCommentSync()
        loadAuthorData()
        if (focusToComment == null && CommentThread == null) {
            // если надо сфокусироваться на каком-то комментарии - не грузим из кэша
            loadCachedData()
        } else {
            requestInitialData()
        }
    }
}