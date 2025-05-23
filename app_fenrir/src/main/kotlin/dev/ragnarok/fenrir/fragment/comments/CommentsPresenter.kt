package dev.ragnarok.fenrir.fragment.comments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Includes.attachmentsRepository
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.R
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
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.CommentIntent
import dev.ragnarok.fenrir.model.CommentUpdate
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.model.CommentsBundle
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.WallReply
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayedFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.syncSingleSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

class CommentsPresenter(
    private var authorId: Long,
    private val commented: Commented,
    focusToComment: Int?,
    commentThread: Int?,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<ICommentsView>(
    authorId, savedInstanceState
) {
    private val ownersRepository: IOwnersRepository = owners
    private val interactor: ICommentsInteractor =
        CommentsInteractor(networkInterfaces, stores, owners)
    private val stickersInteractor: IStickersInteractor =
        InteractorFactory.createStickersInteractor()
    private val data: MutableList<Comment>
    private val CommentThread: Int?
    private val stickersWordsDisplayDisposable = CancelableJob()
    private val actualLoadingDisposable = CompositeJob()
    private val deepLookingHolder = CompositeJob()
    private val cacheLoadingDisposable = CompositeJob()
    private var focusToComment: Int?
    private var commentedState: CommentedState? = null
    private var author: Owner? = null
    private var directionDesc: Boolean
    private var loadingState = 0
    private var adminLevel = 0
    private var draftCommentText: String? = null
    private var draftCommentAttachmentsCount = 0
    private var draftCommentId: Int? = null
    private var replyTo: Comment? = null
    private var sendingNow = false
    private var topicPoll: Poll? = null
    private var loadingAvailableAuthorsNow = false
    private fun loadAuthorData() {
        val accountId = authorId
        appendJob(
            ownersRepository.getBaseOwnerInfo(
                accountId,
                authorId,
                IOwnersRepository.MODE_ANY
            )
                .fromIOToMain({ owner -> onAuthorDataReceived(owner) }) { t ->
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
            if (comment.getObjectId() == update.commentId) {
                applyUpdate(comment, update)
                view?.notifyItemChanged(i)
                return
            } else if (comment.hasThreads()) {
                for (s in comment.threads.orEmpty()) {
                    if (s.getObjectId() == update.commentId) {
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
        update.likeUpdate.requireNonNull {
            comment.setLikesCount(it.count)
            comment.setUserLikes(it.userLikes)
        }
        update.deleteUpdate.requireNonNull {
            comment.setDeleted(it.deleted)
        }
    }

    fun resetDraftMessage() {
        draftCommentAttachmentsCount = 0
        draftCommentText = null
        draftCommentId = null
        replyTo = null
        resolveAttachmentsCounter()
        resolveBodyView()
        resolveReplyViews()
        resolveSendButtonAvailability()
        resolveEmptyTextVisibility()
    }

    fun fireTextEdited(s: String?) {
        if (!Settings.get().main().isHint_stickers) {
            return
        }
        stickersWordsDisplayDisposable.cancel()
        if (s.isNullOrEmpty()) {
            view?.updateStickers(
                emptyList()
            )
            return
        }
        stickersWordsDisplayDisposable.set(
            stickersInteractor.getKeywordsStickers(
                authorId,
                s.trim()
            )
                .delayedFlow(500)
                .fromIOToMain({
                    view?.updateStickers(
                        it
                    )
                }) { u ->
                    showError(u)
                })
    }

    private fun onAttachmentRemoveEvent() {
        draftCommentAttachmentsCount--
        onAttachmentCountChanged()
    }

    private fun onAttachmentCountChanged() {
        resolveSendButtonAvailability()
        resolveAttachmentsCounter()
    }

    private fun onAttachmentAddEvent(event: IAddEvent) {
        draftCommentAttachmentsCount += event.attachments.size
        onAttachmentCountChanged()
    }

    private fun filterAttachmentEvent(event: IBaseEvent): Boolean {
        return draftCommentId != null && event.attachToType == AttachToType.COMMENT && event.accountId == authorId && event.attachToId == draftCommentId
    }

    private fun restoreDraftCommentSync() {
        val draft = interactor.restoreDraftComment(authorId, commented).syncSingleSafe()
        if (draft != null) {
            draftCommentText = draft.text
            draftCommentAttachmentsCount = draft.attachmentsCount
            draftCommentId = draft.id
        }
    }

    private fun requestInitialData() {
        val accountId = authorId
        val single: Flow<CommentsBundle> = when {
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
        actualLoadingDisposable.add(
            single
                .fromIOToMain({ bundle -> onInitialDataReceived(bundle) }) { throwable ->
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
        actualLoadingDisposable.add(
            interactor.getCommentsPortion(
                accountId,
                commented,
                1,
                COUNT,
                first.getObjectId(),
                CommentThread,
                false,
                "desc"
            )
                .fromIOToMain(
                    { bundle -> onCommentsPortionPortionReceived(bundle) }
                ) { throwable -> onCommentPortionError(getCauseIfRuntime(throwable)) })
    }

    private fun loadDown() {
        if (loadingState != LoadingState.NO) return
        val last = lastCommentInList ?: return
        val accountId = authorId
        setLoadingState(LoadingState.DOWN)
        actualLoadingDisposable.add(
            interactor.getCommentsPortion(
                accountId,
                commented,
                0,
                COUNT,
                last.getObjectId(),
                CommentThread,
                false,
                "asc"
            )
                .fromIOToMain(
                    { bundle -> onCommentsPortionPortionReceived(bundle) }
                ) { throwable -> onCommentPortionError(getCauseIfRuntime(throwable)) })
    }

    private fun onCommentPortionError(throwable: Throwable) {
        setLoadingState(LoadingState.NO)
        showError(throwable)
    }

    private fun onCommentsPortionPortionReceived(bundle: CommentsBundle) {
        cacheLoadingDisposable.clear()
        val comments = bundle.comments
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
        commentedState = CommentedState(bundle.firstCommentId, bundle.lastCommentId)
        updateAdminLevel(bundle.adminLevel.orZero())
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
        view?.displayBody(draftCommentText)
    }

    private fun canSendComment(): Boolean {
        return draftCommentAttachmentsCount > 0 || draftCommentText.trimmedNonNullNoEmpty()
    }

    private fun resolveSendButtonAvailability() {
        view?.setButtonSendAvailable(
            canSendComment()
        )
    }

    private fun saveSingle(): Flow<Int> {
        val accountId = authorId
        val replyToComment = replyTo?.getObjectId() ?: 0
        val replyToUser = replyTo?.fromId ?: 0
        return interactor.safeDraftComment(
            accountId,
            commented,
            draftCommentText,
            replyToComment,
            replyToUser
        )
    }

    private fun saveDraftSync(): Int? {
        return saveSingle().syncSingleSafe()
    }

    private fun resolveAttachmentsCounter() {
        view?.displayAttachmentsCount(
            draftCommentAttachmentsCount
        )
    }

    fun fireInputTextChanged(s: String?) {
        val canSend = canSendComment()
        draftCommentText = s
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
        deepLookingHolder.add(
            interactor.getAllCommentsRange(
                accountId,
                commented,
                older?.getObjectId() ?: 0,
                commentId
            )
                .fromIOToMain({ comments ->
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

    override fun onGuiDestroyed() {
        deepLookingHolder.clear()
        super.onGuiDestroyed()
    }

    fun fireDeepLookingCancelledByUser() {
        deepLookingHolder.clear()
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
            draftCommentText
        )
    }

    fun fireEditBodyResult(newText: String?) {
        draftCommentText = newText
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

    fun fireReport(context: Context, comment: Comment) {
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
            .setItems(items) { dialog, item ->
                appendJob(
                    interactor.reportComment(
                        authorId, comment.commented.sourceOwnerId, comment.getObjectId(), item
                    )
                        .fromIOToMain({ p ->
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

    fun fireReplyToChat(context: Context, comment: Comment) {
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
        if (intent.replyToComment == null && CommentThread != null) intent.setReplyToComment(
            CommentThread
        )
        appendJob(
            interactor.send(accountId, commented, CommentThread, intent)
                .fromIOToMain({ onNormalSendResponse() }) { t ->
                    onSendError(
                        t
                    )
                })
    }

    private fun sendQuickComment(intent: CommentIntent) {
        setSendingNow(true)
        if (intent.replyToComment == null && CommentThread != null) intent.setReplyToComment(
            CommentThread
        )
        val accountId = authorId
        appendJob(
            interactor.send(accountId, commented, CommentThread, intent)
                .fromIOToMain({ onQuickSendResponse() }) { t ->
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
        draftCommentText = null
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
        val body = draftCommentText
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
        appendJob(
            interactor.deleteRestore(accountId, commented, commentId, delete)
                .fromIOToMain(dummy()) { t ->
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
        if (Settings.get().main().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        val accountId = authorId
        appendJob(
            interactor.like(accountId, comment.commented, comment.getObjectId(), add)
                .fromIOToMain(dummy()) { t ->
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

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveOptionMenu()
    }

    private fun resolveOptionMenu() {
        val hasPoll = topicPoll != null
        val hasGotoSource = commented.sourceType != CommentedType.TOPIC
        @StringRes var gotoSourceText: Int? = null
        if (hasGotoSource) {
            gotoSourceText = when (commented.sourceType) {
                CommentedType.PHOTO -> R.string.go_to_photo
                CommentedType.VIDEO -> R.string.go_to_video
                CommentedType.POST -> R.string.go_to_post
                else -> null
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
        val single = if (canSendFromAnyGroup) {
            interactor.getAvailableAuthors(accountId)
        } else {
            val ids: MutableSet<Long> = HashSet()
            ids.add(accountId)
            ids.add(commented.sourceOwnerId)
            ownersRepository.findBaseOwnersDataAsList(accountId, ids, IOwnersRepository.MODE_ANY)
        }
        appendJob(
            single
                .fromIOToMain({ owners -> onAvailableAuthorsReceived(owners) }) {
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
        directionDesc = Settings.get().main().isCommentsDesc
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
        data.addAll(bundle.comments)
        commentedState = CommentedState(bundle.firstCommentId, bundle.lastCommentId)
        updateAdminLevel(bundle.adminLevel.orZero())

        // init poll once
        topicPoll = bundle.topicPoll
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
                .fromIOToMain({ onCachedDataReceived(it) }, {
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
        cacheLoadingDisposable.cancel()
        actualLoadingDisposable.cancel()
        deepLookingHolder.cancel()
        stickersWordsDisplayDisposable.cancel()

        // save draft async
        saveSingle().hiddenIO()
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
        this.focusToComment = focusToComment
        directionDesc = Settings.get().main().isCommentsDesc
        this.CommentThread = commentThread
        data = ArrayList()
        val attachmentsRepository = attachmentsRepository
        appendJob(
            attachmentsRepository
                .observeAdding()
                .filter { filterAttachmentEvent(it) }
                .sharedFlowToMain { onAttachmentAddEvent(it) })
        appendJob(
            attachmentsRepository
                .observeRemoving()
                .filter { filterAttachmentEvent(it) }
                .sharedFlowToMain { onAttachmentRemoveEvent() })
        appendJob(
            stores
                .comments()
                .observeMinorUpdates()
                .filter { it.commented == commented }
                .sharedFlowToMain { update -> onCommentMinorUpdate(update) })
        restoreDraftCommentSync()
        loadAuthorData()
        if (focusToComment == null && commentThread == null) {
            // если надо сфокусироваться на каком-то комментарии - не грузим из кэша
            loadCachedData()
        } else {
            requestInitialData()
        }
    }
}