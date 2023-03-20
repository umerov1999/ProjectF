package dev.ragnarok.fenrir.fragment.messages.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.api.model.AttachmentTokens
import dev.ragnarok.fenrir.api.model.AttachmentsTokenCreator
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.crypt.KeyExchangeService
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.crypt.KeyPairDoesNotExistException
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.domain.*
import dev.ragnarok.fenrir.domain.IOwnersRepository.Companion.MODE_NET
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.exception.UploadNotResolvedException
import dev.ragnarok.fenrir.fragment.messages.AbsMessageListPresenter
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.longpoll.ILongpollManager
import dev.ragnarok.fenrir.longpoll.LongpollInstance
import dev.ragnarok.fenrir.media.record.AudioRecordException
import dev.ragnarok.fenrir.media.record.AudioRecordWrapper
import dev.ragnarok.fenrir.media.record.Recorder
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.module.encoder.ToMp4Audio
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.realtime.Processors
import dev.ragnarok.fenrir.service.ChatDownloadWorker
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.*
import dev.ragnarok.fenrir.util.*
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import dev.ragnarok.fenrir.util.Utils.addElementToList
import dev.ragnarok.fenrir.util.Utils.countOfSelection
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.getSelected
import dev.ragnarok.fenrir.util.Utils.hasFlag
import dev.ragnarok.fenrir.util.Utils.hasMarshmallow
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import dev.ragnarok.fenrir.util.rxutils.RxUtils.subscribeOnIOAndIgnore
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.functions.Predicate
import java.io.*
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class ChatPresenter(
    accountId: Long, private val messagesOwnerId: Long,
    initialPeer: Peer, config: ChatConfig, savedInstanceState: Bundle?
) : AbsMessageListPresenter<IChatView>(accountId, savedInstanceState) {

    private var peer: Peer
    private var subtitle: String? = null
    private val audioRecordWrapper: AudioRecordWrapper = AudioRecordWrapper.Builder(App.instance)
        .build()
    private var endOfContent: Boolean = false
    private var outConfig: ChatConfig
    private var draftMessageText: String? = null
    private var draftMessageId: Int? = null
    private var textingNotifier: TextingNotifier
    private var toolbarSubtitleHandler: ToolbarSubtitleHandler = ToolbarSubtitleHandler(this)
    private var draftMessageDbAttachmentsCount: Int = 0

    private var recordingLookup: Lookup

    private val messagesRepository: IMessagesRepository = Repository.messages
    private val stickersInteractor = InteractorFactory.createStickersInteractor()
    private val utilsInteractor = InteractorFactory.createUtilsInteractor()
    private val longpollManager: ILongpollManager = LongpollInstance.longpollManager
    private val uploadManager: IUploadManager = Includes.uploadManager

    private var stickersWordsDisplayDisposable = Disposable.disposed()
    private var cacheLoadingDisposable = Disposable.disposed()
    private var netLoadingDisposable = Disposable.disposed()
    private var fetchConversationDisposable = Disposable.disposed()

    private var conversation: Conversation? = null
    private var chatAdminsIds: ArrayList<Long>? = null

    fun getConversation(): Conversation? {
        return conversation
    }

    private var chronologyInvert = false

    private var isLoadingFromDbNow = false
    private var isLoadingFromNetNow = false

    private val isLoadingNow: Boolean
        get() = isLoadingFromDbNow || isLoadingFromNetNow

    private val isRecordingNow: Boolean
        get() {
            val status = audioRecordWrapper.recorderStatus
            return status == Recorder.Status.PAUSED || status == Recorder.Status.RECORDING_NOW
        }

    private val isGroupChat: Boolean
        get() = Peer.isGroupChat(peerId)

    private val peerId: Long
        get() = peer.id

    val isChronologyInverted: Boolean
        get() = chronologyInvert

    private val isEncryptionSupport: Boolean
        get() = Peer.isUser(peerId) && peerId != messagesOwnerId && !Settings.get()
            .other().isDisabled_encryption

    private val isEncryptionEnabled: Boolean
        get() = Settings.get()
            .security()
            .isMessageEncryptionEnabled(messagesOwnerId, peerId)

    private var currentPhotoCameraUri: Uri? = null

    init {

        if (savedInstanceState == null) {
            peer = initialPeer
            outConfig = config

            if (config.getInitialText().nonNullNoEmpty()) {
                draftMessageText = config.getInitialText()
            }
        } else {
            peer = savedInstanceState.getParcelableCompat(SAVE_PEER)!!
            outConfig = savedInstanceState.getParcelableCompat(SAVE_CONFIG)!!
            currentPhotoCameraUri = savedInstanceState.getParcelableCompat(SAVE_CAMERA_FILE_URI)
            restoreFromInstanceState(savedInstanceState)
        }

        fetchConversationThenCachedThenActual(false)

        if (savedInstanceState == null) {
            tryToRestoreDraftMessage(!draftMessageText.isNullOrEmpty())
        }

        resolveAccountHotSwapSupport()
        textingNotifier = TextingNotifier(messagesOwnerId)

        val predicate = Predicate<IAttachmentsRepository.IBaseEvent> { event ->
            draftMessageId != null
                    && event.accountId == messagesOwnerId
                    && event.attachToId == draftMessageId
        }

        val attachmentsRepository = Includes.attachmentsRepository

        appendDisposable(attachmentsRepository
            .observeAdding()
            .filter(predicate)
            .toMainThread()
            .subscribe { event -> onRepositoryAttachmentsAdded(event.attachments.size) })

        appendDisposable(attachmentsRepository
            .observeRemoving()
            .filter(predicate)
            .toMainThread()
            .subscribe { onRepositoryAttachmentsRemoved() })

        appendDisposable(messagesRepository
            .observeMessageUpdates()
            .toMainThread()
            .subscribe { onMessagesUpdate(it) })

        recordingLookup = Lookup(1000)
            .also {
                it.setCallback(object : Lookup.Callback {
                    override fun onIterated() {
                        resolveRecordingTimeView()
                    }
                })
            }

        appendDisposable(
            longpollManager.observeKeepAlive()
                .toMainThread()
                .subscribe({ onLongpollKeepAliveRequest() }, ignore())
        )

        appendDisposable(Processors.realtimeMessages
            .observeResults()
            .filter { result -> result.accountId == messagesOwnerId }
            .toMainThread()
            .subscribe { result ->
                for (msg in result.data) {
                    val m = msg.message

                    if (m != null && peerId == m.peerId) {
                        onRealtimeMessageReceived(m)
                    }
                }
            })

        appendDisposable(
            uploadManager.observeAdding()
                .toMainThread()
                .subscribe({ onUploadAdded(it) }, ignore())
        )

        appendDisposable(
            uploadManager.observeDeleting(true)
                .toMainThread()
                .subscribe({ onUploadRemoved(it) }, ignore())
        )

        appendDisposable(
            uploadManager.observeResults()
                .toMainThread()
                .subscribe({ onUploadResult(it) }, ignore())
        )

        appendDisposable(
            uploadManager.observeStatus()
                .toMainThread()
                .subscribe({ onUploadStatusChange(it) }, ignore())
        )

        appendDisposable(
            uploadManager.observeProgress()
                .toMainThread()
                .subscribe({ onUploadProgressUpdate(it) }, ignore())
        )

        appendDisposable(
            messagesRepository.observePeerUpdates()
                .toMainThread()
                .subscribe({ onPeerUpdate(it) }, ignore())
        )

        appendDisposable(Repository.owners.observeUpdates()
            .toMainThread()
            .subscribe { onUserUpdates(it) })

        appendDisposable(messagesRepository.observeTextWrite()
            .flatMap { list -> Flowable.fromIterable(list) }
            .toMainThread()
            .subscribe { onUserWriteInDialog(it) })

        updateSubtitle()
    }

    fun invertChronology() {
        chronologyInvert = true
        resolveOptionMenu()
    }

    fun resetChronology() {
        chronologyInvert = false
        resolveOptionMenu()
    }

    private fun onUserWriteInDialog(writeText: WriteText) {
        if (peerId == writeText.peerId) {
            displayUserTextingInToolbar(writeText)
        }
    }

    private fun onUserUpdates(updates: List<UserUpdate>) {
        for (update in updates) {
            if (update.accountId == accountId && isChatWithUser(update.userId)) {
                update.online?.run {
                    subtitle = if (isOnline) {
                        getString(R.string.online)
                    } else {
                        getString(R.string.offline) + ", " + getString(
                            R.string.last_seen_sex_unknown,
                            AppTextUtils.getDateFromUnixTime(lastSeen)
                        )
                    }

                    resolveToolbarSubtitle()
                }
            }
        }
    }

    fun fireResendSwipe(position: Int) {
        if (position < 0 || data.size <= position) {
            return
        }
        val message = data[position]
        when (message.status) {
            MessageStatus.SENDING, MessageStatus.QUEUE, MessageStatus.WAITING_FOR_UPLOAD -> {
                val index = indexOf(message.getObjectId())
                if (index != -1) {
                    data.removeAt(index)
                    view?.notifyItemRemoved(index)
                }
                deleteMessageFromDbAsync(message)
                return
            }

            MessageStatus.ERROR -> {
                view?.showErrorSendDialog(message)
                return
            }

            MessageStatus.EDITING -> {

            }

            MessageStatus.SENT -> {

            }
        }
        fireForwardToHereClick(arrayListOf(data[position]))
    }

    fun fireTranscript(voiceMessageId: String, messageId: Int) {
        appendDisposable(
            messagesRepository.recogniseAudioMessage(
                accountId,
                messageId,
                voiceMessageId
            )
                .fromIOToMain()
                .subscribe({ }, { t -> showError(view, t) })
        )
    }

    fun removeDialog() {
        appendDisposable(
            messagesRepository.deleteDialog(accountId, peerId)
                .fromIOToMain()
                .subscribe(
                    { onDialogRemovedSuccessfully(accountId) },
                    { t -> showError(view, t) })
        )
    }

    private fun onDialogRemovedSuccessfully(oldaccountId: Long) {
        view?.showSnackbar(R.string.deleted, true)
        if (accountId != oldaccountId) {
            return
        }
        data.clear()
        view?.notifyDataChanged()
    }

    private fun onPeerUpdate(updates: List<PeerUpdate>) {
        var requireListUpdate = false

        for (update in updates) {
            if (update.accountId != messagesOwnerId || update.peerId != peerId) continue

            update.readIn?.run {
                conversation?.setInRead(messageId)
                lastReadId.setIncoming(messageId)
            }

            update.unread?.run {
                conversation?.setUnreadCount(count)
                requireListUpdate = true
            }

            update.readOut?.run {
                conversation?.setOutRead(messageId)
                lastReadId.setOutgoing(messageId)
                requireListUpdate = true
            }

            update.pin?.run {
                conversation?.setPinned(pinned)
                resolvePinnedMessageView()
            }

            update.title?.run {
                conversation?.setTitle(title)
                peer.setTitle(title)
                resolveToolbarTitle()
            }
        }

        if (requireListUpdate) {
            view?.notifyDataChanged()
        }
    }

    private fun fetchConversationThenCachedThenActual(refresh: Boolean) {
        fetchConversationDisposable = messagesRepository.getConversationSingle(
            messagesOwnerId,
            peer.id,
            if (refresh) Mode.NET else Mode.ANY
        )
            .fromIOToMain()
            .subscribe({ onConversationFetched(refresh, it) }, { onConversationFetchFail(it) })
    }

    private fun onConversationFetchFail(throwable: Throwable) {
        showError(view, throwable)
    }

    private fun onConversationFetched(refresh: Boolean, data: Conversation) {
        conversation = data
        var avaReload = false
        if (peer.getTitle().isNullOrEmpty()) {
            peer.setTitle(data.getDisplayTitle())
            avaReload = true
        }
        if (peer.avaUrl.isNullOrEmpty()) {
            peer.setAvaUrl(data.imageUrl)
            avaReload = true
        }
        if (avaReload) {
            resolveToolbarAvatar()
            resolveToolbarSubtitle()
            resolveToolbarTitle()
        }
        view?.convert_to_keyboard(data.getCurrentKeyboard())

        resolvePinnedMessageView()
        resolveInputView()

        lastReadId.setIncoming(data.getInRead())
        lastReadId.setOutgoing(data.getOutRead())

        if (!refresh) {
            loadAllCachedData()
        } else {
            requestAtStart()
        }
    }

    private fun onUploadProgressUpdate(data: List<IUploadManager.IProgressUpdate>) {
        edited?.run {
            for (update in data) {
                val index = attachments.indexOfFirst {
                    it.attachment is Upload && it.attachment.getObjectId() == update.id
                }

                if (index != -1) {
                    val upload = attachments[index].attachment as Upload
                    val upId = attachments[index].id
                    if (upload.status == Upload.STATUS_UPLOADING) {
                        upload.progress = update.progress
                        view?.notifyEditUploadProgressUpdate(upId, update.progress)
                    }
                }
            }
        }
    }

    private fun onUploadStatusChange(upload: Upload) {
        edited?.run {
            val index = attachments.indexOfFirst {
                it.attachment is Upload && it.attachment.getObjectId() == upload.getObjectId()
            }

            if (index != -1) {
                (attachments[index].attachment as Upload).apply {
                    status = upload.status
                    errorText = upload.errorText
                }

                view?.notifyEditAttachmentChanged(index)
            }
        }
    }

    fun fireNewChatPhotoSelected(file: String) {
        val intent = UploadIntent(accountId, UploadDestination.forChatPhoto(Peer.toChatId(peerId)))
            .setAutoCommit(true)
            .setFileUri(Uri.parse(file))
            .setSize(Upload.IMAGE_SIZE_FULL)
        uploadManager.enqueue(listOf(intent))
    }

    private fun onUploadResult(pair: Pair<Upload, UploadResult<*>>) {
        val destination = pair.first.destination
        if (destination.method == Method.PHOTO_TO_CHAT && Peer.toChatId(peerId) == destination.ownerId) {
            val res = pair.second.result as String?
            peer.setAvaUrl(res)
            view?.displayToolbarAvatar(peer)
        } else {
            edited?.run {
                if (message.getObjectId() == destination.id && destination.method == Method.TO_MESSAGE) {
                    val photo: AbsModel = pair.second.result as AbsModel
                    val sizeBefore = attachments.size

                    attachments.add(AttachmentEntry(true, photo))
                    view?.notifyEditAttachmentsAdded(sizeBefore, 1)
                    resolveAttachmentsCounter()
                    resolvePrimaryButton()
                }
            }
        }
    }

    private fun onUploadRemoved(ids: IntArray) {
        edited?.run {
            for (id in ids) {
                val index = attachments.indexOfFirst {
                    it.attachment is Upload && it.attachment.getObjectId() == id
                }

                if (index != -1) {
                    attachments.removeAt(index)
                    view?.notifyEditAttachmentRemoved(index)
                }
            }
        }
    }

    private fun onUploadAdded(uploads: List<Upload>) {
        edited?.run {
            val filtered = uploads
                .asSequence()
                .filter { u ->
                    u.destination.id == message.getObjectId() && u.destination.method == Method.TO_MESSAGE
                }.map {
                    AttachmentEntry(true, it)
                }.toList()

            if (filtered.isNotEmpty()) {
                attachments.addAll(0, filtered)
                view?.notifyEditAttachmentsAdded(0, filtered.size)
            }
        }
    }

    override fun onGuiCreated(viewHost: IChatView) {
        super.onGuiCreated(viewHost)
        resolvePinnedMessageView()
        resolveEditedMessageViews()
        resolveLoadUpHeaderView()
        resolveEmptyTextVisibility()
        resolveAttachmentsCounter()
        resolveDraftMessageText()
        resolveToolbarTitle()
        resolveToolbarAvatar()
        resolvePrimaryButton()
        resolveRecordPauseButton()
        resolveRecordingTimeView()
        resolveActionMode()
        resolveToolbarSubtitle()
        hideWriting()
        resolveResumePeer()
        resolveInputImagesUploading()
        resolveOptionMenu()
    }

    private fun resolvePinnedMessageView() {
        conversation?.run {
            view?.displayPinnedMessage(
                getPinned(),
                hasFlag(getAcl(), Conversation.AclFlags.CAN_CHANGE_PIN)
            )
        } ?: run {
            view?.displayPinnedMessage(null, false)
        }
    }

    private fun resolveInputView() {
        conversation?.run {
            if (isGroupChannel()) view?.hideInputView()
        }
    }

    private fun resolveEditedMessageViews() {
        view?.displayEditingMessage(edited?.message)
    }

    private fun onLongpollKeepAliveRequest() {
        checkLongpoll()
    }

    private fun onRepositoryAttachmentsRemoved() {
        draftMessageDbAttachmentsCount--
        resolveAttachmentsCounter()
        resolvePrimaryButton()
    }

    private fun onRepositoryAttachmentsAdded(count: Int) {
        draftMessageDbAttachmentsCount += count
        resolveAttachmentsCounter()
        resolvePrimaryButton()
    }

    private fun loadAllCachedData() {
        setCacheLoadingNow(true)
        cacheLoadingDisposable = messagesRepository.getCachedPeerMessages(messagesOwnerId, peer.id)
            .flatMap { t ->
                run {
                    val list = t.toMutableList()
                    val iterator = list.iterator()
                    val delete: ArrayList<Int> = ArrayList()
                    while (iterator.hasNext()) {
                        val kk = iterator.next()
                        if (kk.status == MessageStatus.SENDING) {
                            delete.add(kk.getObjectId())
                            iterator.remove()
                        }
                    }
                    if (delete.isNotEmpty()) {
                        Stores.instance
                            .messages()
                            .deleteMessages(messagesOwnerId, delete)
                            .blockingSubscribe(ignore(), ignore())
                    }
                    Single.just(list)
                }
            }
            .fromIOToMain()
            .subscribe(
                { onCachedDataReceived(it) },
                { onCachedDataReceived(emptyList()) })
    }

    private fun onCachedDataReceived(data: List<Message>) {
        setCacheLoadingNow(false)
        onAllDataLoaded(data, appendToList = false, isCache = true)
        requestAtStart()
    }

    private fun onNetDataReceived(messages: List<Message>, startMessageId: Int?) {
        cacheLoadingDisposable.dispose()

        isLoadingFromDbNow = false
        endOfContent = messages.isEmpty()
        if (chronologyInvert && startMessageId == null)
            endOfContent = true

        setNetLoadingNow(false)
        onAllDataLoaded(messages, startMessageId != null, isCache = false)
    }

    fun fireScrollToUnread() {
        conversation?.getUnreadCount()?.let { view?.scrollToUnread(it, false) }
    }

    fun fireDeleteChatPhoto() {
        appendDisposable(
            messagesRepository.deleteChatPhoto(accountId, Peer.toChatId(peerId))
                .fromIOToMain()
                .subscribe(
                    { peer.setAvaUrl(null); view?.displayToolbarAvatar(peer) },
                    { t -> showError(view, t) })
        )
    }

    @SuppressLint("CheckResult")
    private fun onAllDataLoaded(messages: List<Message>, appendToList: Boolean, isCache: Boolean) {
        val all = !appendToList

        //сохранение выделенных сообщений
        if (all) {
            val selectedList = data.filter {
                it.isSelected
            }

            for (selected in selectedList) {
                for (item in messages) {
                    if (item.getObjectId() == selected.getObjectId()) {
                        item.isSelected = true
                        break
                    }
                }
            }
        }
        var requestLookMessage = false
        if (all && data.isNotEmpty()) {
            data.clear()
            data.addAll(messages)
            view?.notifyDataChanged()
            if (!chronologyInvert && !isCache) {
                conversation?.getUnreadCount()?.let {
                    if (conversation?.getUnreadCount().orZero() <= messages.size) {
                        view?.scrollToUnread(it, true)
                    } else {
                        view?.goToUnreadMessages(
                            accountId,
                            conversation?.getInRead().orZero(),
                            lastReadId.getIncoming(),
                            lastReadId.getOutgoing(),
                            conversation?.getUnreadCount().orZero(),
                            peer
                        )
                        requestLookMessage = true
                    }
                }
            }
        } else {
            val startSize = data.size
            data.addAll(messages)
            view?.notifyMessagesUpAdded(startSize, messages.size)
        }

        resolveEmptyTextVisibility()
        if (!requestLookMessage && all && data.isNotEmpty() && !chronologyInvert && !isCache) {
            fireCheckMessages()
        }
    }

    private fun fireCheckMessages() {
        if (Settings.get().other().isAuto_read) {
            appendDisposable(
                checkErrorMessages().fromIOToMain()
                    .subscribe({ t -> if (t) startSendService() else readAllUnreadMessagesIfExists() }) { })
        }
    }

    fun fireNetworkChanged() {
        if (!isHiddenAccount(accountId)) {
            appendDisposable(
                checkErrorMessages().fromIOToMain()
                    .subscribe({ t -> if (t) startSendService() else readAllUnreadMessagesIfExists() }) { })
        }
    }

    fun fireCheckMessages(incoming: Int, outgoing: Int) {
        if (incoming != -1 && outgoing != -1) {
            lastReadId.setIncoming(incoming)
            lastReadId.setOutgoing(outgoing)
            view?.notifyDataChanged()
        }
        if (Settings.get().other().isAuto_read) {
            appendDisposable(
                checkErrorMessages().fromIOToMain()
                    .subscribe({ t -> if (t) startSendService() else readAllUnreadMessagesIfExists() }) { })
        }
    }

    @SuppressLint("CheckResult")
    private fun checkErrorMessages(): Single<Boolean> {
        if (isHiddenAccount(accountId)) return Single.just(false)
        val list: ArrayList<Int> = ArrayList()
        for (i: Message in data) {
            if (i.status == MessageStatus.ERROR) {
                list.add(i.getObjectId())
            }
        }
        if (list.isNotEmpty()) {
            messagesRepository.enqueueAgainList(messagesOwnerId, list)
                .blockingSubscribe(dummy(), ignore())
        }
        return Single.just(list.isNotEmpty())
    }

    private fun setCacheLoadingNow(cacheLoadingNow: Boolean) {
        this.isLoadingFromDbNow = cacheLoadingNow
        resolveLoadUpHeaderView()
    }

    fun fireLoadUpButtonClick() {
        if (canLoadMore()) {
            requestMore()
        }
    }

    private fun canLoadMore(): Boolean {
        return data.isNotEmpty() && !isLoadingFromDbNow && !isLoadingFromNetNow && !chronologyInvert && !endOfContent
    }

    private fun resolveLoadUpHeaderView() {
        val loading = isLoadingNow
        view?.setupLoadUpHeaderState(if (loading) LoadMoreState.LOADING else if (endOfContent) LoadMoreState.END_OF_LIST else LoadMoreState.CAN_LOAD_MORE)
    }

    private fun requestAtStart() {
        requestFromNet(null)
    }

    private fun setNetLoadingNow(netLoadingNow: Boolean) {
        this.isLoadingFromNetNow = netLoadingNow
        resolveLoadUpHeaderView()
    }

    private fun requestFromNet(startMessageId: Int?) {
        setNetLoadingNow(true)

        val peerId = this.peerId
        netLoadingDisposable = messagesRepository.getPeerMessages(
            messagesOwnerId,
            peerId,
            COUNT,
            null,
            startMessageId,
            !chronologyInvert,
            chronologyInvert
        )
            .fromIOToMain()
            .subscribe(
                { messages -> onNetDataReceived(messages, startMessageId) },
                { this.onMessagesGetError(it) })
    }

    private fun onMessagesGetError(t: Throwable) {
        setNetLoadingNow(false)
        logThrowable("Chat issues", getCauseIfRuntime(t))
        showError(view, getCauseIfRuntime(t))
    }

    private fun requestMore() {
        val lastId = if (data.size > 0) data[data.size - 1].getObjectId() else null
        requestFromNet(lastId)
    }

    private fun onMessagesRestoredSuccessfully(id: Int) {
        data.find {
            it.getObjectId() == id
        }?.run {
            setDeleted(false)
            setDeletedForAll(false)
            view?.notifyDataChanged()
        }
    }

    fun fireTextEdited(s: String?) {
        if (!Settings.get().other().isHint_stickers) {
            return
        }
        stickersWordsDisplayDisposable.dispose()
        if (s.isNullOrEmpty()) {
            view?.updateStickers(emptyList())
            return
        }
        stickersWordsDisplayDisposable =
            stickersInteractor.getKeywordsStickers(accountId, s.trim())
                .delay(500, TimeUnit.MILLISECONDS)
                .fromIOToMain()
                .subscribe({ stickers -> view?.updateStickers(stickers) }, ignore())
    }

    fun fireDraftMessageTextEdited(s: String?) {
        s ?: run {
            draftMessageText = null
            return
        }
        if (Peer.isGroupChat(peerId)) {
            if (s.nonNullNoEmpty() && s.length == 1 && s[0] == '@') {
                view?.showChatMembers(accountId, Peer.toChatId(peerId))
            }
        }
        edited?.run {
            val wasEmpty = body.isNullOrBlank()
            body = s
            if (wasEmpty != body.isNullOrBlank()) {
                resolvePrimaryButton()
            }
            return
        }

        val oldState = canSendNormalMessage()
        draftMessageText = s
        val newState = canSendNormalMessage()

        if (oldState != newState) {
            resolvePrimaryButton()
        }

        if (!isHiddenAccount(accountId) && !Settings.get().main().isDont_write) {
            readAllUnreadMessagesIfExists()
            textingNotifier.notifyAboutTyping(peerId)
        }
    }

    fun fireSendClick() {
        if (canSendNormalMessage()) {
            sendImpl()
        }
    }

    private fun sendImpl() {
        val securitySettings = Settings.get().security()

        val trimmedBody = AppTextUtils.safeTrim(draftMessageText, null)
        val encryptionEnabled = securitySettings.isMessageEncryptionEnabled(messagesOwnerId, peerId)

        @KeyLocationPolicy
        var keyLocationPolicy = KeyLocationPolicy.PERSIST
        if (encryptionEnabled) {
            keyLocationPolicy =
                securitySettings.getEncryptionLocationPolicy(messagesOwnerId, peerId)
        }

        val builder = SaveMessageBuilder(messagesOwnerId, peer.id)
            .also {
                it.setBody(trimmedBody)
                it.setDraftMessageId(draftMessageId)
                it.setRequireEncryption(encryptionEnabled)
                it.setKeyLocationPolicy(keyLocationPolicy)
            }

        val fwds = ArrayList<Message>()

        for (model in outConfig.getModels()) {
            if (model is FwdMessages) {
                fwds.addAll(model.fwds)
            } else {
                builder.attach(model)
            }
        }

        builder.setForwardMessages(fwds)

        outConfig.getModels().clear()
        outConfig.setInitialText(null)

        draftMessageId = null
        draftMessageText = null
        draftMessageDbAttachmentsCount = 0

        view?.resetInputAttachments()

        resolveAttachmentsCounter()
        resolveDraftMessageText()
        resolvePrimaryButton()

        sendMessage(builder)

        if (outConfig.isCloseOnSend()) {
            view?.doCloseAfterSend()
        }
    }

    @SuppressLint("CheckResult")
    private fun sendMessage(builder: SaveMessageBuilder) {
        messagesRepository.put(builder)
            .fromIOToMain()
            .doOnSuccess {
                if (Settings.get()
                        .main().isOver_ten_attach && it.isHasAttachments && it.attachments?.size()
                        .orZero() > 10
                ) {
                    val temp = it.attachments?.toList()
                    val att: ArrayList<AbsModel> = ArrayList()
                    for (i in 10 until temp?.size.orZero()) {
                        att.add(temp?.get(i) ?: continue)
                    }
                    outConfig.appendAll(att)
                    resolveAttachmentsCounter()
                    resolvePrimaryButton()
                }
                startSendService()
            }.subscribe(WeakConsumer(messageSavedConsumer), WeakConsumer(messageSaveFailConsumer))
    }

    private val messageSavedConsumer: Consumer<Message> = Consumer { onMessageSaveSuccess(it) }
    private val messageSaveFailConsumer: Consumer<Throwable> = Consumer { onMessageSaveError(it) }

    private fun onMessageSaveError(throwable: Throwable) {
        view?.run {
            when (throwable) {
                is KeyPairDoesNotExistException -> showError(R.string.no_encryption_keys)
                is UploadNotResolvedException -> showError(R.string.upload_not_resolved_exception_message)
                else -> showError(throwable.message)
            }
        }
    }

    private fun onMessageSaveSuccess(message: Message) {
        addMessageToList(message)
        view?.notifyDataChanged()
        view?.scrollTo(0)
    }

    private fun startSendService() {
        messagesRepository.runSendingQueue()
    }

    fun fireAttachButtonClick() {
        edited?.run {
            view?.showEditAttachmentsDialog(attachments)
            return
        }

        if (draftMessageId == null) {
            draftMessageId = Stores.instance
                .messages()
                .saveDraftMessageBody(messagesOwnerId, peerId, draftMessageText)
                .blockingGet()
        }

        val destination = UploadDestination.forMessage(draftMessageId ?: return)
        view?.goToMessageAttachmentsEditor(
            accountId,
            messagesOwnerId,
            destination,
            draftMessageText,
            outConfig.getModels()
        ) // TODO: 15.08.2017
    }

    private fun canSendNormalMessage(): Boolean {
        return (calculateAttachmentsCount() > 0 && !isReplyMessageCanVoice()) || draftMessageText.trimmedNonNullNoEmpty() || nowUploadingToEditingMessage()
    }

    private fun resolveEmptyTextVisibility() {
        view?.setEmptyTextVisible(data.isEmpty() && !isLoadingNow)
    }

    private fun nowUploadingToEditingMessage(): Boolean {
        val messageId = draftMessageId ?: return false

        val current = uploadManager.getCurrent()
        return current.nonEmpty() && current.get()?.destination?.compareTo(
            messageId,
            UploadDestination.WITHOUT_OWNER,
            Method.TO_MESSAGE
        ) == true
    }

    private fun resolveAttachmentsCounter() {
        edited?.run {
            view?.displayDraftMessageAttachmentsCount(calculateAttachmentsCount(this))
        } ?: run {
            view?.displayDraftMessageAttachmentsCount(calculateAttachmentsCount())
        }
    }

    private fun resolveDraftMessageText() {
        edited?.run {
            view?.displayDraftMessageText(body)
        } ?: run {
            view?.displayDraftMessageText(draftMessageText)
        }
    }

    private fun resolveToolbarTitle() {
        view?.displayToolbarTitle(peer.getTitle())
    }

    private fun resolveToolbarAvatar() {
        view?.displayToolbarAvatar(peer)
    }

    fun fireRecordCancelClick() {
        audioRecordWrapper.stopRecording()
        onRecordingStateChanged()
        resolveRecordPauseButton()
    }

    private fun onRecordingStateChanged() {
        resolvePrimaryButton()
        syncRecordingLookupState()
    }

    fun fireRecordingButtonClick() {
        if (!hasAudioRecordPermissions()) {
            view?.requestRecordPermissions()
            return
        }

        startRecordImpl()
    }

    fun sendRecordingMessageImpl(file: File) {
        view?.scrollTo(0)
        val builder = SaveMessageBuilder(messagesOwnerId, peerId).setVoiceMessageFile(file)
        if (isReplyMessageCanVoice()) {
            val fwds = ArrayList<Message>()
            for (model in outConfig.getModels()) {
                if (model is FwdMessages) {
                    fwds.addAll(model.fwds)
                } else {
                    builder.attach(model)
                }
            }
            builder.setForwardMessages(fwds)
            outConfig.getModels().clear()
            resolveAttachmentsCounter()
        }
        sendMessage(builder)
    }

    fun sendRecordingCustomMessageImpl(context: Context, file: String) {
        val to = File(AudioRecordWrapper.getRecordingDirectory(context), "converted.mp3")
        to.delete()
        view?.customToast?.showToastInfo(R.string.do_convert)
        appendDisposable(
            Single.create {
                it.onSuccess(ToMp4Audio.encodeToMp4Audio(file, to.absolutePath))
            }.fromIOToMain()
                .subscribe({ o ->
                    if (o) {
                        view?.customToast?.showToastInfo(R.string.success)
                        sendRecordingMessageImpl(to)
                    } else {
                        view?.customToast?.showToastError(R.string.error)
                        sendRecordingMessageImpl(File(file))
                    }
                }, {
                    run {
                        view?.customToast?.showToastError(R.string.error)
                        sendRecordingMessageImpl(File(file))
                    }
                })
        )
    }

    fun fireRecordSendClick() {
        try {
            val file = audioRecordWrapper.stopRecordingAndReceiveFile()
            sendRecordingMessageImpl(file)
        } catch (e: AudioRecordException) {
            e.printStackTrace()
        }

        onRecordingStateChanged()
        resolveRecordPauseButton()
    }

    fun fireRecordResumePauseClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val isRecorderPaused = audioRecordWrapper.recorderStatus == Recorder.Status.PAUSED
                if (!isRecorderPaused) {
                    audioRecordWrapper.pause()
                } else {
                    audioRecordWrapper.doRecord()
                }

                resolveRecordPauseButton()
            } catch (e: AudioRecordException) {
                e.printStackTrace()
            }

        } else {
            view?.showError(R.string.pause_is_not_supported)
        }
    }

    private fun resolvePrimaryButton() {
        if (isRecordingNow) {
            view?.setupPrimaryButtonAsRecording()
        } else {
            edited?.run {
                view?.setupPrimaryButtonAsEditing(canSave)
            } ?: run {
                view?.setupPrimaryButtonAsRegular(
                    canSendNormalMessage(),
                    !isHiddenAccount(accountId)
                )
            }
        }
    }

    private fun resolveRecordPauseButton() {
        val paused = audioRecordWrapper.recorderStatus == Recorder.Status.PAUSED
        val available = audioRecordWrapper.isPauseSupported
        view?.setupRecordPauseButton(available, !paused)
    }

    fun fireRecordPermissionsResolved() {
        if (hasAudioRecordPermissions()) {
            startRecordImpl()
        }
    }

    private fun startRecordImpl() {
        try {
            audioRecordWrapper.doRecord()
        } catch (e: AudioRecordException) {
            e.printStackTrace()
        }

        onRecordingStateChanged()
        resolveRecordingTimeView()
    }

    private fun hasAudioRecordPermissions(): Boolean {
        if (!hasMarshmallow()) return true
        val app = applicationContext

        val recordPermission =
            ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO)
        return recordPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun syncRecordingLookupState() {
        if (isRecordingNow) {
            recordingLookup.start()
        } else {
            recordingLookup.stop()
        }
    }

    internal fun resolveRecordingTimeView() {
        if (isRecordingNow) {
            view?.displayRecordingDuration(audioRecordWrapper.currentRecordDuration)
        }
    }

    private fun addMessageToList(message: Message) {
        addElementToList(message, data, MESSAGES_COMPARATOR)
        resolveEmptyTextVisibility()
    }

    private fun onMessagesUpdate(updates: List<MessageUpdate>) {
        for (update in updates) {
            val targetIndex = indexOf(update.getMessageId())

            update.getStatusUpdate()?.run {
                if (getVkid() != null) {
                    // message was sent
                    val alreadyExist = indexOf(getVkid().orZero()) != -1

                    if (alreadyExist) {
                        if (targetIndex != -1) {
                            data.removeAt(targetIndex)
                        }
                    } else {
                        if (targetIndex != -1) {
                            val message = data[targetIndex]
                            message.setStatus(getStatus())
                            message.setId(getVkid().orZero())

                            data.removeAt(targetIndex)
                            addMessageToList(message)
                        }
                    }
                } else {
                    //message not sent
                    if (targetIndex != -1) {
                        val message = data[targetIndex]
                        message.setStatus(getStatus())
                    }
                }
            } ?: run {
                if (targetIndex != -1) {
                    update.getDeleteUpdate()?.run {
                        data[targetIndex].setDeleted(isDeleted())
                        data[targetIndex].setDeletedForAll(isDeletedForAll())
                    }

                    update.getImportantUpdate()?.run {
                        data[targetIndex].setImportant(isImportant())
                    }
                }
            }
        }

        view?.notifyDataChanged()
    }

    private fun onRealtimeMessageReceived(message: Message) {
        if (message.peerId != peer.id || messagesOwnerId != message.accountId) {
            return
        }

        if (!Settings.get().main().isExpand_voice_transcript) {
            message.attachments?.voiceMessages?.let {
                for (i: VoiceMessage in it) {
                    i.setShowTranscript(true)
                }
            }
        }

        if (message.isChatTitleUpdate) {
            peer.setTitle(message.actionText)
            resolveToolbarTitle()
        }

        val index = data.indexOfFirst {
            it.getObjectId() == message.getObjectId()
        }

        if (index != -1) {
            data.removeAt(index)
        }

        if (message.isOut && message.randomId > 0) {
            val unsentIndex = data.indexOfFirst {
                it.randomId == message.randomId && !it.isSent
            }

            if (unsentIndex != -1) {
                data.removeAt(unsentIndex)
            }
        }

        addMessageToList(message)
        view?.notifyDataChanged()
        if (!message.isOut && message.keyboard != null && !(message.keyboard
                ?: return).inline && message.keyboard?.buttons.nonNullNoEmpty()
        ) {
            conversation?.setCurrentKeyboard(message.keyboard)
            fetchConversationDisposable = Repository.messages
                .updateDialogKeyboard(accountId, peerId, message.keyboard)
                .fromIOToMain()
                .subscribe(dummy(), ignore())
            view?.convert_to_keyboard(message.keyboard)
        }
        if (Settings.get().other().isAuto_read && !Processors.realtimeMessages
                .isNotificationIntercepted(accountId, peerId)
        ) {
            readAllUnreadMessagesIfExists()
        }
    }

    private fun isChatWithUser(userId: Long): Boolean {
        return !isGroupChat && Peer.toUserId(peerId) == userId
    }

    private fun displayUserTextingInToolbar(writeText: WriteText) {
        if (!Settings.get().ui().isDisplay_writing)
            return

        view?.displayWriting(writeText)
        toolbarSubtitleHandler.restoreToolbarWithDelay()
    }

    fun ResolveWritingInfo(context: Context, writeText: WriteText) {
        appendDisposable(
            OwnerInfo.getRx(context, accountId, writeText.getFrom_ids()[0])
                .fromIOToMain()
                .subscribe({ t ->
                    view?.displayWriting(
                        t.owner,
                        writeText.getFrom_ids().size,
                        writeText.isText
                    )
                }, { run {} })
        )
    }

    private fun updateSubtitle() {
        subtitle = null

        when (Peer.getType(peerId)) {
            Peer.GROUP -> {
                subtitle = null
                resolveToolbarSubtitle()
            }

            Peer.CHAT -> appendDisposable(
                messagesRepository.getChatUsers(
                    accountId,
                    Peer.toChatId(peerId)
                )
                    .fromIOToMain()
                    .subscribe({
                        chatAdminsIds = ArrayList()
                        for (i in it) {
                            if (i.isAdmin() || i.isOwner()) {
                                chatAdminsIds?.add(i.getOwnerObjectId())
                            }
                        }
                        subtitle = getString(R.string.chat_users_count, it.size)
                        resolveToolbarSubtitle()
                    }, { resolveToolbarSubtitle() })
            )


            Peer.USER -> appendDisposable(
                Stores.instance
                    .owners()
                    .getLocalizedUserActivity(messagesOwnerId, Peer.toUserId(peerId))
                    .fromIOToMain()
                    .subscribe({ s ->
                        subtitle = s
                        resolveToolbarSubtitle()
                    }, { logThrowable("ChatPresenter", it) }, { resolveToolbarSubtitle() })
            )
        }
    }

    private fun canEdit(message: Message): Boolean {
        return message.isOut && Unixtime.now() - message.date < 24 * 60 * 60
                && !message.isSticker && !message.isVoiceMessage && !message.isGraffiti && !message.isCall
    }

    private fun canChangePin(): Boolean {
        return conversation?.run {
            hasFlag(getAcl(), Conversation.AclFlags.CAN_CHANGE_PIN)
        } ?: run {
            false
        }
    }

    private fun doStar(): Boolean {
        val selectionCount = countOfSelection(data)
        if (selectionCount <= 0)
            return false
        return !data.find {
            it.isSelected
        }!!.isImportant
    }

    private fun canStar(): Boolean {
        val selectionCount = countOfSelection(data)
        if (selectionCount <= 0)
            return false

        val iterator = data.iterator()
        var status = false
        var has = false
        while (iterator.hasNext()) {
            val message = iterator.next()
            if (!message.isSelected) {
                continue
            }
            if (!has) {
                has = true
                status = message.isImportant
            } else {
                if (message.isImportant != status || message.status != MessageStatus.SENT)
                    return false
            }
        }
        return true
    }

    override fun resolveActionMode() {
        val selectionCount = countOfSelection(data)
        if (selectionCount > 0) {
            if (selectionCount == 1) {
                val message = data.find {
                    it.isSelected
                } ?: return

                view?.showActionMode(
                    selectionCount.toString(),
                    canEdit(message),
                    canChangePin(),
                    canStar(),
                    doStar(),
                    !message.isOut
                )
            } else {
                view?.showActionMode(
                    selectionCount.toString(),
                    canEdit = false,
                    canPin = false,
                    canStar = canStar(),
                    doStar = doStar(),
                    canSpam = false
                )
            }
        } else {
            view?.finishActionMode()
        }
    }

    private fun resolveToolbarSubtitle() {
        view?.displayToolbarSubtitle(subtitle)
    }

    internal fun hideWriting() {
        view?.hideWriting()
    }

    private fun checkLongpoll() {
        if (accountId != ISettings.IAccountsSettings.INVALID_ID) {
            longpollManager.keepAlive(accountId)
        }
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        checkLongpoll()
        Processors.realtimeMessages
            .registerNotificationsInterceptor(id, Pair.create(messagesOwnerId, peerId))
    }

    public override fun onGuiPaused() {
        super.onGuiPaused()
        checkLongpoll()
        Processors.realtimeMessages.unregisterNotificationsInterceptor(id)
    }

    private fun tryToRestoreDraftMessage(ignoreBody: Boolean) {
        appendDisposable(
            Stores.instance
                .messages()
                .findDraftMessage(messagesOwnerId, peerId)
                .fromIOToMain()
                .subscribe(
                    { draft -> onDraftMessageRestored(draft, ignoreBody) },
                    { logThrowable("ChatPresenter", it) })
        )
    }

    private fun calculateAttachmentsCount(message: EditedMessage): Int {
        var count = 0

        for (entry in message.attachments) {
            if (entry.attachment is FwdMessages) {
                if (entry.attachment.fwds.nonNullNoEmpty()) {
                    count += entry.attachment.fwds.size
                }
            } else if (entry.attachment !is Upload) {
                count++
            }
        }

        return count
    }

    private fun isReplyMessageCanVoice(): Boolean {
        if (draftMessageDbAttachmentsCount > 0) {
            return false
        }
        var outConfigCount = 0
        for (model in outConfig.getModels()) {
            if (model is FwdMessages) {
                outConfigCount += model.fwds.size
                for (i in model.fwds) {
                    if (i.peerId != peerId) {
                        return false
                    }
                }
            } else {
                return false
            }
        }

        return outConfigCount == 1
    }

    private fun calculateAttachmentsCount(): Int {
        var outConfigCount = 0
        for (model in outConfig.getModels()) {
            if (model is FwdMessages) {
                outConfigCount += model.fwds.size
            } else {
                outConfigCount++
            }
        }

        return outConfigCount + draftMessageDbAttachmentsCount
    }

    private fun onDraftMessageRestored(message: DraftMessage, ignoreBody: Boolean) {
        if (draftMessageText.isNullOrEmpty()) {
            draftMessageDbAttachmentsCount = message.getAttachmentsCount()
            draftMessageId = message.getId()

            if (!ignoreBody) {
                draftMessageText = message.getBody()
            }
        }

        resolveAttachmentsCounter()
        resolvePrimaryButton()
        resolveDraftMessageText()
    }

    fun resetDraftMessage() {
        draftMessageText = null
        resolvePrimaryButton()
        resolveDraftMessageText()
    }

    private fun resolveAccountHotSwapSupport() {
        if (!Peer.isGroupChat(peerId)) {
            toggleSupportAccountHotSwap()
        }
    }

    override fun onDestroyed() {
        stickersWordsDisplayDisposable.dispose()
        cacheLoadingDisposable.dispose()
        netLoadingDisposable.dispose()
        fetchConversationDisposable.dispose()

        saveDraftMessageBody()

        toolbarSubtitleHandler.release()

        recordingLookup.stop()
        recordingLookup.setCallback(null)

        textingNotifier.shutdown()
        super.onDestroyed()
    }

    fun saveDraftMessageBody() {
        Stores.instance
            .messages()
            .saveDraftMessageBody(messagesOwnerId, peerId, draftMessageText)
            .subscribeIOAndIgnoreResults()
    }

    override fun onMessageClick(message: Message) {
        if (message.status == MessageStatus.ERROR) {
            view?.showErrorSendDialog(message)
        } else {
            readUnreadMessagesUpIfExists(message)
        }
    }

    private fun readUnreadMessagesUpIfExists(message: Message) {
        if (isHiddenAccount(accountId)) return

        if (!message.isOut && message.originalId > lastReadId.getIncoming()) {
            lastReadId.setIncoming(message.originalId)

            view?.notifyDataChanged()

            appendDisposable(
                messagesRepository.markAsRead(messagesOwnerId, peer.id, message.originalId)
                    .fromIOToMain()
                    .subscribe(dummy()) { t -> showError(view, t) }
            )
        }
    }

    private fun readAllUnreadMessagesIfExists() {
        if (isHiddenAccount(accountId)) return
        val last = if (data.nonNullNoEmpty()) data[0] else return

        if (!last.isOut && last.originalId > lastReadId.getIncoming()) {
            lastReadId.setIncoming(last.originalId)

            view?.notifyDataChanged()

            appendDisposable(
                messagesRepository.markAsRead(messagesOwnerId, peer.id, last.originalId)
                    .fromIOToMain()
                    .subscribe(dummy()) { t -> showError(view, t) }
            )
        }
    }

    fun fireMessageRestoreClick(message: Message) {
        restoreMessage(message.getObjectId())
    }

    private fun restoreMessage(messageId: Int) {
        appendDisposable(
            messagesRepository.restoreMessage(messagesOwnerId, peerId, messageId)
                .fromIOToMain()
                .subscribe(
                    { onMessagesRestoredSuccessfully(messageId) },
                    { t -> showError(view, t) })
        )
    }

    fun fireEditMessageResult(accompanyingModels: ModelsBundle) {
        outConfig.setModels(accompanyingModels)

        resolveAttachmentsCounter()
        resolvePrimaryButton()
    }

    override fun onActionModeDeleteClick() {
        super.onActionModeDeleteClick()
        deleteSelectedMessages()
    }

    override fun onActionModeSpamClick() {
        super.onActionModeSpamClick()
        spamDelete()
    }

    fun fireActionModeStarClick() {
        val sent = ArrayList<Int>(0)
        val iterator = data.iterator()
        var hasChanged = false
        var isImportant = false
        while (iterator.hasNext()) {
            val message = iterator.next()
            if (!message.isSelected) {
                continue
            }
            if (!hasChanged) {
                hasChanged = true
                isImportant = message.isImportant
            }
            sent.add(message.getObjectId())
        }
        if (sent.nonNullNoEmpty()) {
            appendDisposable(
                messagesRepository.markAsImportant(
                    messagesOwnerId,
                    peer.id,
                    sent,
                    if (!isImportant) 1 else 0
                ).fromIOToMain().subscribe(dummy()) { t -> showError(view, t) }
            )
        }
    }

    /**
     * Удаление отмеченных сообщений
     * можно удалять сообщения в статусе
     * STATUS_SENT - отправляем запрос на сервис, удаление из списка произойдет через longpoll
     * STATUS_QUEUE || STATUS_ERROR - просто удаляем из БД и списка
     * STATUS_WAITING_FOR_UPLOAD - отменяем "аплоад", удаляем из БД и списка
     */
    private fun deleteSelectedMessages() {
        val sent = ArrayList<Message>(0)
        val canDeleteForAll = ArrayList<Message>(0)
        val canEditAndDeleteForAll = ArrayList<Message>(0)

        var hasChanged = false
        val iterator = data.iterator()

        while (iterator.hasNext()) {
            val message = iterator.next()

            if (!message.isSelected) {
                continue
            }

            when (message.status) {
                MessageStatus.SENT -> {
                    if (canDeleteForAll(message)) {
                        canDeleteForAll.add(message)
                        if (canEdit(message)) {
                            canEditAndDeleteForAll.add(message)
                        }
                    } else {
                        sent.add(message)
                    }
                }

                MessageStatus.QUEUE, MessageStatus.ERROR, MessageStatus.SENDING -> {
                    deleteMessageFromDbAsync(message)
                    iterator.remove()
                    hasChanged = true
                }

                MessageStatus.WAITING_FOR_UPLOAD -> {
                    cancelWaitingForUploadMessage(message.getObjectId())
                    deleteMessageFromDbAsync(message)
                    iterator.remove()
                    hasChanged = true
                }

                MessageStatus.EDITING -> {

                }
            }
        }

        if (sent.nonNullNoEmpty()) {
            deleteSentImpl(sent, 0)
        }

        if (hasChanged) {
            view?.notifyDataChanged()
        }

        if (canDeleteForAll.isNotEmpty()) {
            view?.showDeleteForAllDialog(canDeleteForAll, canEditAndDeleteForAll)
        }
    }

    private fun spamDelete() {
        val sent = ArrayList<Int>(0)

        var hasChanged = false
        val iterator = data.iterator()

        while (iterator.hasNext()) {
            val message = iterator.next()

            if (!message.isSelected) {
                continue
            }

            when (message.status) {
                MessageStatus.SENT -> {
                    if (!message.isOut) {
                        sent.add(message.getObjectId())
                    }
                }

                MessageStatus.QUEUE, MessageStatus.ERROR, MessageStatus.SENDING -> {
                    deleteMessageFromDbAsync(message)
                    iterator.remove()
                    hasChanged = true
                }

                MessageStatus.WAITING_FOR_UPLOAD -> {
                    cancelWaitingForUploadMessage(message.getObjectId())
                    deleteMessageFromDbAsync(message)
                    iterator.remove()
                    hasChanged = true
                }

                MessageStatus.EDITING -> {

                }
            }
        }

        if (sent.nonNullNoEmpty()) {
            appendDisposable(
                messagesRepository.deleteMessages(
                    messagesOwnerId, peerId, sent,
                    forAll = false,
                    spam = true
                )
                    .fromIOToMain()
                    .subscribe(dummy()) { t -> showError(view, t) }
            )
        }

        if (hasChanged) {
            view?.notifyDataChanged()
        }
    }

    private fun normalDelete(ids: Collection<Int>, forAll: Boolean) {
        appendDisposable(
            messagesRepository.deleteMessages(messagesOwnerId, peerId, ids, forAll, false)
                .fromIOToMain()
                .subscribe(dummy()) { t -> showError(view, t) }
        )
    }

    private fun deleteSentImpl(ids: MutableCollection<Message>, forAll: Int) {
        if (forAll == 2) {
            superDeleteSentImpl(ids)
            return
        }

        val messages: ArrayList<Int> = ArrayList(ids.size)
        for (tmp in ids) {
            messages.add(tmp.getObjectId())
        }
        normalDelete(messages, forAll == 1)
    }

    private fun superDeleteEditRecursive(messages: ArrayList<Message>, result: ArrayList<Int>) {
        if (messages.isEmpty()) {
            if (result.nonNullNoEmpty()) {
                normalDelete(result, true)
            }
            return
        }
        val message = messages.removeAt(0)
        appendDisposable(
            messagesRepository.edit(
                messagesOwnerId, message, "Ragnarök",
                emptyList(), false
            )
                .fromIOToMain()
                .subscribe({
                    run {
                        result.add(it.getObjectId())
                        onMessageEdited(it)
                        superDeleteEditRecursive(messages, result)
                    }
                }, { onMessageEditFail(it) })
        )
    }

    private fun superDeleteSentImpl(messages: MutableCollection<Message>) {
        val tmp = ArrayList(messages)
        val result = ArrayList<Int>()
        superDeleteEditRecursive(tmp, result)
    }

    private fun canDeleteForAll(message: Message): Boolean {
        return chatAdminsIds?.contains(accountId) == true || (message.isOut && Unixtime.now() - message.date < 24 * 60 * 60 && peerId != accountId)
    }

    private fun cancelWaitingForUploadMessage(messageId: Int) {
        val destination = UploadDestination.forMessage(messageId)
        uploadManager.cancelAll(messagesOwnerId, destination)
    }

    fun fireSendAgainClick(message: Message) {
        appendDisposable(
            messagesRepository.enqueueAgain(messagesOwnerId, message.getObjectId())
                .fromIOToMain()
                .subscribe({ startSendService() }, { logThrowable("ChatPresenter", it) })
        )
    }

    private fun deleteMessageFromDbAsync(message: Message) {
        subscribeOnIOAndIgnore(
            Stores.instance
                .messages()
                .deleteMessage(messagesOwnerId, message.getObjectId())
        )
    }

    fun fireErrorMessageDeleteClick(message: Message) {
        val index = indexOf(message.getObjectId())
        if (index != -1) {
            data.removeAt(index)
            view?.notifyItemRemoved(index)
        }

        deleteMessageFromDbAsync(message)
    }

    fun fireRefreshClick() {
        fetchConversationThenCachedThenActual(true)
    }

    fun fireShortLinkClick(context: Context) {
        PlaceFactory.getShortLinks(accountId).tryOpenWith(context)
    }

    fun fireShow_Profile() {
        view?.showUserWall(accountId, peerId)
    }

    fun fireLeaveChatClick() {
        val chatId = Peer.toChatId(peerId)
        val accountId = super.accountId

        appendDisposable(
            messagesRepository.removeChatMember(accountId, chatId, accountId)
                .fromIOToMain()
                .subscribe(dummy()) { t -> showError(view, t) }
        )
    }

    fun fireChatTitleClick() {
        view?.showChatTitleChangeDialog(peer.getTitle())
    }

    fun fireChatMembersClick() {
        view?.goToChatMembers(accountId, Peer.toChatId(peerId))
    }

    fun fireChatDownloadClick(context: Context, action: String) {
        val downloadWork = OneTimeWorkRequest.Builder(ChatDownloadWorker::class.java)
        val data = Data.Builder()
        data.putLong(Extra.OWNER_ID, peerId)
        data.putLong(Extra.ACCOUNT_ID, accountId)
        data.putString(Extra.TITLE, conversation?.getDisplayTitle(context))
        data.putString(Extra.ACTION, action)
        downloadWork.setInputData(data.build())
        WorkManager.getInstance(context).enqueue(downloadWork.build())
    }

    fun fireDialogAttachmentsClick() {
        view?.goToConversationAttachments(accountId, peerId) // TODO: 15.08.2017
    }

    fun fireSearchClick() {
        view?.goToSearchMessage(accountId, peer) // TODO: 15.08.2017
    }

    fun fireImageUploadSizeSelected(streams: List<Uri>, size: Int) {
        uploadStreamsImpl(streams, size, false)
    }

    private fun uploadStreams(streams: List<Uri>, mime: String?) {
        if (streams.isEmpty() || mime.isNullOrEmpty()) return

        val size = Settings.get()
            .main()
            .uploadImageSize

        val isVideo = ActivityUtils.isMimeVideo(mime)

        if (size == null && !isVideo) {
            view?.showImageSizeSelectDialog(streams)
        } else {
            uploadStreamsImpl(streams, size, isVideo)
        }
    }

    private fun resolveResumePeer() {
        view?.notifyChatResume(accountId, peerId, peer.getTitle(), peer.avaUrl)
        view?.convert_to_keyboard(conversation?.getCurrentKeyboard())
    }

    private fun uploadStreamsImpl(streams: List<Uri>, size: Int?, is_video: Boolean) {
        outConfig.setUploadFiles(null)
        outConfig.setUploadFilesMimeType(null)

        view?.resetUploadImages()

        if (draftMessageId == null) {
            draftMessageId = Stores.instance
                .messages()
                .saveDraftMessageBody(messagesOwnerId, peerId, draftMessageText)
                .blockingGet()
        }

        val destination = if (is_video) UploadDestination.forMessage(
            draftMessageId ?: return,
            MessageMethod.VIDEO
        ) else UploadDestination.forMessage(draftMessageId ?: return)
        val intents = ArrayList<UploadIntent>(streams.size)

        if (!is_video) {
            for (uri in streams) {
                intents.add(
                    UploadIntent(messagesOwnerId, destination)
                        .setAutoCommit(true)
                        .setFileUri(uri)
                        .setSize(size ?: return)
                )
            }
        } else {
            for (uri in streams) {
                intents.add(
                    UploadIntent(messagesOwnerId, destination)
                        .setAutoCommit(true)
                        .setFileUri(uri)
                )
            }
        }

        uploadManager.enqueue(intents)
    }

    fun fireUploadCancelClick() {
        outConfig.setUploadFiles(null)
        outConfig.setUploadFilesMimeType(null)
    }

    private fun resolveInputImagesUploading() {
        outConfig.getUploadFiles().nonNullNoEmpty {
            outConfig.getUploadFilesMimeType().nonNullNoEmpty { s ->
                uploadStreams(it, s)
            }
        }
    }

    fun fireMessagesLookup(message: Message) {
        view?.goToMessagesLookup(accountId, message.peerId, message.getObjectId(), message)
    }

    fun fireChatTitleTyped(newValue: String) {
        val chatId = Peer.toChatId(peerId)

        appendDisposable(
            messagesRepository.editChat(messagesOwnerId, chatId, newValue)
                .fromIOToMain()
                .subscribe(dummy()) { t -> showError(view, t) }
        )
    }

    fun fireForwardToHereClick(messages: ArrayList<Message>) {
        for (i in outConfig.getModels()) {
            if (i is FwdMessages) {
                if (i.fwds.nonNullNoEmpty()) {
                    for (p in i.fwds) {
                        if (messages.contains(p)) {
                            messages.remove(p)
                        }
                    }
                }
            }
        }
        if (messages.nonNullNoEmpty()) {
            outConfig.getModels().append(FwdMessages(messages))
        }

        resolveAttachmentsCounter()
        resolvePrimaryButton()
    }

    fun fireForwardToAnotherClick(messages: ArrayList<Message>) {
        view?.forwardMessagesToAnotherConversation(messages, messagesOwnerId) // TODO: 15.08.2017
    }

    public override fun onActionModeForwardClick() {
        val selected = getSelected(data)
        if (selected.isNotEmpty()) {
            view?.diplayForwardTypeSelectDialog(selected)
        }
    }

    private fun resolveOptionMenu() {
        val chat = isGroupChat

        var isPlusEncryption = false
        if (isEncryptionEnabled) {
            isPlusEncryption = Settings.get()
                .security()
                .getEncryptionLocationPolicy(messagesOwnerId, peerId) == KeyLocationPolicy.RAM
        }

        view?.configOptionMenu(
            chat,
            chat,
            chat,
            isEncryptionSupport,
            isEncryptionEnabled,
            isPlusEncryption,
            isEncryptionSupport,
            !chronologyInvert,
            peerId < VKApiMessage.CHAT_PEER,
            chat
        )
    }

    fun fireEncriptionStatusClick() {
        if (!isEncryptionEnabled && !Settings.get().security().isKeyEncryptionPolicyAccepted) {
            view?.showEncryptionDisclaimerDialog(REQUEST_CODE_ENABLE_ENCRYPTION)
            return
        }

        onEncryptionToggleClick()
    }

    private fun onEncryptionToggleClick() {
        if (isEncryptionEnabled) {
            Settings.get().security().disableMessageEncryption(messagesOwnerId, peerId)
            resolveOptionMenu()
        } else {
            view?.showEncryptionKeysPolicyChooseDialog(REQUEST_CODE_ENABLE_ENCRYPTION)
        }
    }

    private fun fireKeyStoreSelected(requestCode: Int, @KeyLocationPolicy policy: Int) {
        when (requestCode) {
            REQUEST_CODE_ENABLE_ENCRYPTION -> onEnableEncryptionKeyStoreSelected(policy)
            REQUEST_CODE_KEY_EXCHANGE -> KeyExchangeService.iniciateKeyExchangeSession(
                applicationContext,
                messagesOwnerId,
                peerId,
                policy
            )
        }
    }

    fun fireDiskKeyStoreSelected(requestCode: Int) {
        fireKeyStoreSelected(requestCode, KeyLocationPolicy.PERSIST)
    }

    fun fireRamKeyStoreSelected(requestCode: Int) {
        fireKeyStoreSelected(requestCode, KeyLocationPolicy.RAM)
    }

    private fun onEnableEncryptionKeyStoreSelected(@KeyLocationPolicy policy: Int) {
        appendDisposable(
            Stores.instance
                .keys(policy)
                .getKeys(messagesOwnerId, peerId)
                .fromIOToMain()
                .subscribe(
                    { aesKeyPairs -> fireEncriptionEnableClick(policy, aesKeyPairs) },
                    { logThrowable("ChatPresenter", it) })
        )
    }

    private fun fireEncriptionEnableClick(@KeyLocationPolicy policy: Int, pairs: List<AesKeyPair>) {
        if (pairs.isEmpty()) {
            view?.displayIniciateKeyExchangeQuestion(policy)
        } else {
            Settings.get().security().enableMessageEncryption(messagesOwnerId, peerId, policy)
            resolveOptionMenu()
        }
    }

    fun fireIniciateKeyExchangeClick(@KeyLocationPolicy policy: Int) {
        KeyExchangeService.iniciateKeyExchangeSession(App.instance, messagesOwnerId, peerId, policy)
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_PEER, peer)
        outState.putString(SAVE_DRAFT_MESSAGE_TEXT, draftMessageText)
        outState.putInt(SAVE_DRAFT_MESSAGE_ATTACHMENTS_COUNT, draftMessageDbAttachmentsCount)
        outState.putParcelable(SAVE_CONFIG, outConfig)
        outState.putParcelable(SAVE_CAMERA_FILE_URI, currentPhotoCameraUri)

        draftMessageId?.run {
            outState.putInt(SAVE_DRAFT_MESSAGE_ID, this)
        }
    }

    private fun restoreFromInstanceState(state: Bundle) {
        draftMessageText = state.getString(SAVE_DRAFT_MESSAGE_TEXT)
        draftMessageDbAttachmentsCount = state.getInt(SAVE_DRAFT_MESSAGE_ATTACHMENTS_COUNT)

        if (state.containsKey(SAVE_DRAFT_MESSAGE_ID)) {
            draftMessageId = state.getInt(SAVE_DRAFT_MESSAGE_ID)
        }
    }

    private fun checkGraffitiMessage(filePath: Sticker.LocalSticker): Single<Optional<IAttachmentToken>> {
        if (filePath.path.nonNullNoEmpty()) {
            val docsApi = Includes.networkInterfaces.vkDefault(accountId).docs()
            return docsApi.getMessagesUploadServer(
                peerId,
                if (filePath.isAnimated) "doc" else "graffiti"
            )
                .flatMap { server ->
                    val file = File(filePath.path)
                    val sInput = arrayOfNulls<InputStream>(1)
                    try {
                        sInput[0] = FileInputStream(file)
                        return@flatMap Includes.networkInterfaces.uploads()
                            .uploadDocumentRx(
                                server.url ?: throw NotFoundException("upload url empty"),
                                if (filePath.isAnimated) filePath.animationName else file.name,
                                sInput[0]!!,
                                null
                            )
                            .doFinally(safelyCloseAction(sInput[0]))
                            .flatMap { uploadDto ->
                                docsApi
                                    .save(uploadDto.file, null, null)
                                    .map { dtos ->
                                        if (dtos.type.isEmpty()) {
                                            throw NotFoundException("Unable to save graffiti message")
                                        }
                                        val dto = dtos.doc
                                        val token = AttachmentsTokenCreator.ofDocument(
                                            dto.id,
                                            dto.ownerId,
                                            dto.accessKey
                                        )
                                        Optional.wrap(token)
                                    }
                            }
                    } catch (e: FileNotFoundException) {
                        safelyClose(sInput[0])
                        return@flatMap Single.error(e)
                    }
                }
        }
        return Single.just(Optional.empty())
    }

    fun fireSendMyStickerClick(file: Sticker.LocalSticker) {
        netLoadingDisposable = checkGraffitiMessage(file)
            .fromIOToMain()
            .subscribe({
                if (it.nonEmpty()) {
                    val kk = it.get() as AttachmentTokens.AttachmentToken
                    if (!file.isAnimated) {
                        val graffiti = Graffiti().setId(kk.id).setOwner_id(kk.ownerId)
                            .setAccess_key(kk.accessKey)
                        val builder = SaveMessageBuilder(messagesOwnerId, peerId).attach(graffiti)
                        sendMessage(builder)
                    } else {
                        val doc = Document(kk.id, kk.ownerId).setAccessKey(kk.accessKey)
                        val builder = SaveMessageBuilder(messagesOwnerId, peerId).attach(doc)
                        sendMessage(builder)
                    }
                }
            }, { onConversationFetchFail(it) })
    }

    fun fireStickerSendClick(sticker: Sticker) {
        view?.scrollTo(0)
        val builder = SaveMessageBuilder(messagesOwnerId, peerId).attach(sticker)

        val fwds = ArrayList<Message>()
        for (model in outConfig.getModels()) {
            if (model is FwdMessages) {
                fwds.addAll(model.fwds)
            }
        }
        if (fwds.size == 1) {
            builder.setForwardMessages(fwds)
            outConfig.getModels().clear()
            view?.resetInputAttachments()
            resolveAttachmentsCounter()
        }
        sendMessage(builder)
    }

    fun fireBotSendClick(item: Keyboard.Button, context: Context) {
        if (item.type == "open_link") {
            LinkHelper.openLinkInBrowser(context, item.link)
            return
        }
        view?.scrollTo(0)
        val builder =
            SaveMessageBuilder(messagesOwnerId, peerId).setPayload(item.payload).setBody(item.label)
        sendMessage(builder)
    }

    fun fireKeyExchangeClick() {
        if (!Settings.get().security().isKeyEncryptionPolicyAccepted) {
            view?.showEncryptionDisclaimerDialog(REQUEST_CODE_KEY_EXCHANGE)
            return
        }

        if (isEncryptionSupport) {
            view?.showEncryptionKeysPolicyChooseDialog(REQUEST_CODE_KEY_EXCHANGE)
        }
    }

    fun fireGenerateInviteLink() {
        netLoadingDisposable = utilsInteractor.getInviteLink(accountId, peerId, 0)
            .fromIOToMain()
            .subscribe({
                it.link?.let { it1 -> view?.copyToClipBoard(it1) }
            }, { onConversationFetchFail(it) })
    }

    fun fireTermsOfUseAcceptClick(requestCode: Int) {
        Settings.get().security().isKeyEncryptionPolicyAccepted = true

        when (requestCode) {
            REQUEST_CODE_KEY_EXCHANGE -> if (isEncryptionSupport) {
                view?.showEncryptionKeysPolicyChooseDialog(REQUEST_CODE_KEY_EXCHANGE)
            }

            REQUEST_CODE_ENABLE_ENCRYPTION -> onEncryptionToggleClick()
        }
    }

    fun fireSendClickFromAttachments() {
        fireSendClick()
    }

    private var edited: EditedMessage? = null

    fun fireActionModeEditClick() {
        val m = data.find {
            it.isSelected
        }

        edited = if (m != null) EditedMessage(m) else null

        resolveDraftMessageText()
        resolveAttachmentsCounter()
        resolveEditedMessageViews()
        resolvePrimaryButton()
    }

    private fun cancelMessageEditing(): Boolean {
        edited?.run {
            val destination = UploadDestination.forMessage(message.getObjectId())

            edited = null
            resolveDraftMessageText()
            resolveAttachmentsCounter()
            resolveEditedMessageViews()
            resolvePrimaryButton()

            uploadManager.cancelAll(accountId, destination)
            return true
        }

        return false
    }

    fun fireCancelEditingClick() {
        cancelMessageEditing()
    }

    fun onBackPressed(): Boolean {
        return !cancelMessageEditing()
    }

    fun fireEditMessageSaveClick() {
        edited?.run {
            val models = ArrayList<AbsModel>()
            var keepForward = false

            for (entry in attachments) {
                when (entry.attachment) {
                    is FwdMessages -> keepForward = true
                    is Upload -> {
                        view?.showError(R.string.upload_not_resolved_exception_message)
                        return
                    }

                    else -> models.add(entry.attachment)
                }
            }

            appendDisposable(
                messagesRepository.edit(accountId, message, body, models, keepForward)
                    .fromIOToMain()
                    .subscribe({ onMessageEdited(it) }, { t -> onMessageEditFail(t) })
            )
        }
    }

    private fun onMessageEditFail(throwable: Throwable) {
        showError(view, throwable)
    }

    private fun onMessageEdited(message: Message) {
        edited = null
        resolveAttachmentsCounter()
        resolveDraftMessageText()
        resolveEditedMessageViews()
        resolvePrimaryButton()

        val index = data.indexOfFirst {
            it.getObjectId() == message.getObjectId()
        }

        if (index != -1) {
            data[index] = message
            view?.notifyDataChanged()
        }
    }

    fun fireEditAttachmentRetry(entry: AttachmentEntry) {
        fireEditAttachmentRemoved(entry)
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

    fun fireEditAttachmentRemoved(entry: AttachmentEntry) {
        if (entry.attachment is Upload) {
            uploadManager.cancel(entry.attachment.getObjectId())
            return
        }

        edited?.run {
            val index = attachments.indexOf(entry)
            if (index != -1) {
                attachments.removeAt(index)
                view?.notifyEditAttachmentRemoved(index)
                resolveAttachmentsCounter()
                resolvePrimaryButton()
            }
        }
    }

    fun fireEditAddImageClick() {
        view?.startImagesSelection(accountId, messagesOwnerId)
    }

    fun fireShowChatMembers() {
        view?.showChatMembers(accountId, Peer.toChatId(peerId))
    }

    fun fireLongAvatarClick(Id: Long) {
        if (Id > 0) {
            netLoadingDisposable = Repository.owners.getFullUserInfo(accountId, Id, MODE_NET)
                .fromIOToMain()
                .subscribe({ info ->
                    run {
                        val Dmn: String = if (info.first?.domain == null)
                            "@id$Id,"
                        else
                            "@" + info.first.domain + ","
                        view?.appendMessageText(Dmn)
                    }
                }, { })
        }
    }

    fun fireFilePhotoForUploadSelected(file: String?, imageSize: Int) {
        edited?.run {
            val destination = UploadDestination.forMessage(message.getObjectId())
            val intent = UploadIntent(accountId, destination)
                .setAutoCommit(false)
                .setFileUri(Uri.parse(file)).setSize(imageSize)
            uploadManager.enqueue(listOf(intent))
        }
    }

    fun fireFileVideoForUploadSelected(file: String?) {
        edited?.run {
            val destination =
                UploadDestination.forMessage(message.getObjectId(), MessageMethod.VIDEO)
            val intent = UploadIntent(accountId, destination)
                .setAutoCommit(false)
                .setFileUri(Uri.parse(file))
            uploadManager.enqueue(listOf(intent))
        }
    }

    fun fireFileAudioForUploadSelected(file: String?) {
        edited?.run {
            val destination =
                UploadDestination.forMessage(message.getObjectId(), MessageMethod.AUDIO)
            val intent = UploadIntent(accountId, destination)
                .setAutoCommit(false)
                .setFileUri(Uri.parse(file))
            uploadManager.enqueue(listOf(intent))
        }
    }

    fun fireEditLocalPhotosSelected(localPhotos: List<LocalPhoto>, imageSize: Int) {
        edited?.run {
            if (localPhotos.isNotEmpty()) {
                val destination = UploadDestination.forMessage(message.getObjectId())

                val intents = localPhotos.map {
                    UploadIntent(accountId, destination).apply {
                        pAutoCommit = false
                        fileId = it.getImageId()
                        pFileUri = it.getFullImageUri()
                        size = imageSize
                    }
                }

                uploadManager.enqueue(intents)
            }
        }
    }

    fun fireEditLocalVideoSelected(video: LocalVideo) {
        edited?.run {
            val destination =
                UploadDestination.forMessage(message.getObjectId(), MessageMethod.VIDEO)

            val intents = UploadIntent(accountId, destination).apply {
                pAutoCommit = false
                pFileUri = Uri.parse(video.getData().toString())
            }

            uploadManager.enqueue(listOf(intents))
        }
    }

    fun fireEditAttachmentsSelected(models: List<AbsModel>) {
        edited?.run {
            if (models.isNotEmpty()) {
                val additional = models.map {
                    AttachmentEntry(true, it)
                }

                val sizeBefore = attachments.size
                attachments.addAll(additional)
                view?.notifyEditAttachmentsAdded(sizeBefore, additional.size)
                resolveAttachmentsCounter()
                resolvePrimaryButton()
            }
        }
    }

    fun fireUnpinClick() {
        doPin(null)
    }

    private fun doPin(message: Message?) {
        appendDisposable(
            messagesRepository.pin(accountId, peerId, message)
                .fromIOToMain()
                .subscribe(dummy()) { onPinFail(it) }
        )
    }

    fun fireActionModePinClick() {
        val message = data.find { it.isSelected }
        doPin(message)
    }

    private fun onPinFail(throwable: Throwable) {
        showError(view, throwable)
    }

    fun onEditAddVideoClick() {
        view?.startVideoSelection(accountId, messagesOwnerId)
    }

    fun onEditAddAudioClick() {
        view?.startAudioSelection(accountId)
    }

    fun onEditAddDocClick() {
        view?.startDocSelection(accountId)
    }

    fun fireEditCameraClick() {
        try {
            val file = FileUtil.createImageFile()
            currentPhotoCameraUri =
                file.let { FileUtil.getExportedUriForFile(applicationContext, it) }
            currentPhotoCameraUri?.run {
                view?.startCamera(this)
            }
        } catch (e: IOException) {
            view?.showError(e.message)
        }
    }

    @Suppress("DEPRECATION")
    fun fireEditPhotoMaked(size: Int) {
        val uri = currentPhotoCameraUri
        currentPhotoCameraUri = null

        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        applicationContext.sendBroadcast(scanIntent)

        val makedPhoto = LocalPhoto().setFullImageUri(uri)
        fireEditLocalPhotosSelected(listOf(makedPhoto), size)
    }

    fun fireDeleteForAllClick(ids: ArrayList<Message>) {
        deleteSentImpl(ids, 1)
    }

    fun fireDeleteSuper(ids: ArrayList<Message>) {
        deleteSentImpl(ids, 2)
    }

    fun fireDeleteForMeClick(ids: ArrayList<Message>) {
        deleteSentImpl(ids, 0)
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestMore()
        }
    }

    private class ToolbarSubtitleHandler(presenter: ChatPresenter) :
        Handler(Looper.getMainLooper()) {

        var reference: WeakReference<ChatPresenter> = WeakReference(presenter)

        override fun handleMessage(msg: android.os.Message) {
            reference.get()?.run {
                when (msg.what) {
                    RESTORE_TOLLBAR -> hideWriting()
                }
            }
        }

        fun release() {
            removeMessages(RESTORE_TOLLBAR)
        }

        fun restoreToolbarWithDelay() {
            sendEmptyMessageDelayed(RESTORE_TOLLBAR, 3000)
        }

        companion object {
            const val RESTORE_TOLLBAR = 12
        }
    }

    companion object {

        private const val COUNT = 30

        private const val SAVE_PEER = "save_peer"
        private const val SAVE_DRAFT_MESSAGE_TEXT = "save_draft_message_text"
        private const val SAVE_DRAFT_MESSAGE_ATTACHMENTS_COUNT =
            "save_draft_message_attachments_count"
        private const val SAVE_DRAFT_MESSAGE_ID = "save_draft_message_id"
        private const val SAVE_CONFIG = "save_config"
        private const val SAVE_CAMERA_FILE_URI = "save_camera_file_uri"

        private const val REQUEST_CODE_ENABLE_ENCRYPTION = 1
        private const val REQUEST_CODE_KEY_EXCHANGE = 2

        private val MESSAGES_COMPARATOR = Comparator<Message> { rhs, lhs ->
            // соблюдаем сортировку как при запросе в бд

            if (lhs.status == rhs.status) {
                return@Comparator lhs.getObjectId().compareTo(rhs.getObjectId())
            }

            lhs.status.compareTo(rhs.status)
        }
    }
}
