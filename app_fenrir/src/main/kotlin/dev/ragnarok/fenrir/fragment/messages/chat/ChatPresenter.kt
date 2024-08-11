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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.App
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
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
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.IOwnersRepository.Companion.MODE_NET
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Mode
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.domain.mappers.Entity2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.exception.UploadNotResolvedException
import dev.ragnarok.fenrir.fragment.messages.AbsMessageListPresenter
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.longpoll.ILongpollManager
import dev.ragnarok.fenrir.longpoll.LongpollInstance
import dev.ragnarok.fenrir.media.record.AudioRecordException
import dev.ragnarok.fenrir.media.record.AudioRecordWrapper
import dev.ragnarok.fenrir.media.record.Recorder
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.ChatConfig
import dev.ragnarok.fenrir.model.Conversation
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.DraftMessage
import dev.ragnarok.fenrir.model.EditedMessage
import dev.ragnarok.fenrir.model.FwdMessages
import dev.ragnarok.fenrir.model.Graffiti
import dev.ragnarok.fenrir.model.Keyboard
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.MessageStatus
import dev.ragnarok.fenrir.model.MessageUpdate
import dev.ragnarok.fenrir.model.ModelsBundle
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.model.PeerUpdate
import dev.ragnarok.fenrir.model.Reaction
import dev.ragnarok.fenrir.model.SaveMessageBuilder
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.UserUpdate
import dev.ragnarok.fenrir.model.VoiceMessage
import dev.ragnarok.fenrir.model.WriteText
import dev.ragnarok.fenrir.module.encoder.ToMp4Audio
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.realtime.Processors
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.service.ChatDownloadWorker
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.MessageMethod
import dev.ragnarok.fenrir.upload.Method
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadIntent
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.FileUtil
import dev.ragnarok.fenrir.util.Lookup
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import dev.ragnarok.fenrir.util.TextingNotifier
import dev.ragnarok.fenrir.util.Unixtime
import dev.ragnarok.fenrir.util.Utils.addElementToList
import dev.ragnarok.fenrir.util.Utils.countOfSelection
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.getSelected
import dev.ragnarok.fenrir.util.Utils.hasFlag
import dev.ragnarok.fenrir.util.Utils.hasMarshmallow
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayedFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.syncSingleSafe
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong

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

    private var stickersWordsDisplayDisposable = CompositeJob()
    private var cacheLoadingDisposable = CompositeJob()
    private var netLoadingDisposable = CompositeJob()
    private var fetchConversationDisposable = CompositeJob()
    private var waitReactionUpdateDisposable = CompositeJob()

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
            .main().isDisabled_encryption

    private val isEncryptionEnabled: Boolean
        get() = Settings.get()
            .security()
            .isMessageEncryptionEnabled(messagesOwnerId, peerId)

    private var currentPhotoCameraUri: Uri? = null

    private var generatedId: Long

    init {
        if (savedInstanceState == null) {
            generatedId = IDGEN.incrementAndGet()

            peer = initialPeer
            outConfig = config

            if (config.initialText.nonNullNoEmpty()) {
                draftMessageText = config.initialText
            }
        } else {
            generatedId = savedInstanceState.getLong(SAVE_ID)
            if (generatedId >= IDGEN.get()) {
                IDGEN.set(generatedId + 1)
            }
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

        val predicate: suspend (IAttachmentsRepository.IBaseEvent) -> Boolean = {
            draftMessageId != null
                    && it.accountId == messagesOwnerId
                    && it.attachToId == draftMessageId
        }

        val attachmentsRepository = Includes.attachmentsRepository

        appendJob(attachmentsRepository
            .observeAdding()
            .filter(predicate)
            .sharedFlowToMain { onRepositoryAttachmentsAdded(it.attachments.size) })

        appendJob(attachmentsRepository
            .observeRemoving()
            .filter(predicate)
            .sharedFlowToMain { onRepositoryAttachmentsRemoved() })

        appendJob(messagesRepository
            .observeMessageUpdates()
            .sharedFlowToMain { onMessagesUpdate(it) })

        recordingLookup = Lookup(1000)
            .also {
                it.setCallback(object : Lookup.Callback {
                    override fun onIterated() {
                        resolveRecordingTimeView()
                    }
                })
            }

        appendJob(
            longpollManager.observeKeepAlive()
                .sharedFlowToMain { onLongpollKeepAliveRequest() }
        )

        appendJob(Processors.realtimeMessages
            .observeResults()
            .filter { result -> result.accountId == messagesOwnerId }
            .sharedFlowToMain { result ->
                for (msg in result.data) {
                    val m = msg.message

                    if (m != null && peerId == m.peerId) {
                        onRealtimeMessageReceived(m)
                    }
                }
            })

        appendJob(
            uploadManager.observeAdding()
                .sharedFlowToMain { onUploadAdded(it) }
        )

        appendJob(
            uploadManager.observeDeleting(true)
                .sharedFlowToMain { onUploadRemoved(it) }
        )

        appendJob(
            uploadManager.observeResults()
                .sharedFlowToMain { onUploadResult(it) }
        )

        appendJob(
            uploadManager.observeStatus()
                .sharedFlowToMain { onUploadStatusChange(it) }
        )

        appendJob(
            uploadManager.observeProgress()
                .sharedFlowToMain { onUploadProgressUpdate(it) }
        )

        appendJob(
            messagesRepository.observePeerUpdates()
                .sharedFlowToMain { onPeerUpdate(it) }
        )

        appendJob(Repository.owners.observeUpdates()
            .sharedFlowToMain { onUserUpdates(it) })

        appendJob(messagesRepository.observeTextWrite()
            .flatMapConcat { it.asFlow() }
            .sharedFlowToMain { onUserWriteInDialog(it) })

        updateSubtitle()
    }

    override fun resolveListView() {
        view?.displayMessages(messagesOwnerId, data, lastReadId)
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
        appendJob(
            messagesRepository.recogniseAudioMessage(
                accountId,
                messageId,
                voiceMessageId
            )
                .fromIOToMain({ }, { t -> showError(view, t) })
        )
    }

    fun removeDialog() {
        appendJob(
            messagesRepository.deleteDialog(accountId, peerId)
                .fromIOToMain({ onDialogRemovedSuccessfully(accountId) },
                    { t -> showError(view, t) })
        )
    }

    private fun onDialogRemovedSuccessfully(oldAccountId: Long) {
        view?.showSnackbar(R.string.deleted, true)
        if (accountId != oldAccountId) {
            return
        }
        data.clear()
        view?.notifyDataChanged()
    }

    override fun fireReactionModeClick(position: Int) {
        if (isHiddenAccount(messagesOwnerId) || isHiddenAccount(accountId)) {
            return
        }
        if (position >= 0 && data.size > position) {
            data[position].setReactionEditMode(!data[position].reactionEditMode)
            safeNotifyItemChanged(position)
        }
    }

    private fun onPeerUpdate(updates: List<PeerUpdate>) {
        var requireListUpdate = false

        for (update in updates) {
            if (update.accountId != messagesOwnerId || update.peerId != peerId) continue

            update.readIn?.run {
                conversation?.setInRead(messageId)
                lastReadId.incoming = messageId
            }

            update.unread?.run {
                conversation?.setUnreadCount(count)
                requireListUpdate = true
            }

            update.readOut?.run {
                conversation?.setOutRead(messageId)
                lastReadId.outgoing = messageId
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
        fetchConversationDisposable += messagesRepository.getConversationSingle(
            messagesOwnerId,
            peer.id,
            if (refresh) Mode.NET else Mode.ANY
        )
            .fromIOToMain({ onConversationFetched(refresh, it) }, { onConversationFetchFail(it) })
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
        view?.convertToKeyboard(data.currentKeyboard)

        resolvePinnedMessageView()
        resolveInputView()

        lastReadId.incoming = data.inRead
        lastReadId.outgoing = data.outRead

        if (!refresh) {
            loadAllCachedData()
        } else {
            requestAtStart()
        }
    }

    private fun onUploadProgressUpdate(data: IUploadManager.IProgressUpdate?) {
        edited?.run {
            data?.let { update ->
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
                pinned,
                hasFlag(acl, Conversation.AclFlags.CAN_CHANGE_PIN)
            )
        } ?: run {
            view?.displayPinnedMessage(null, false)
        }
    }

    private fun resolveInputView() {
        conversation?.run {
            if (isGroupChannel) view?.hideInputView()
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
        cacheLoadingDisposable += messagesRepository.getCachedPeerMessages(messagesOwnerId, peer.id)
            .flatMapConcat { t ->
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
                val st = emptyTaskFlow()
                if (delete.isNotEmpty()) {
                    st.andThen(
                        Stores.instance
                            .messages()
                            .deleteMessages(messagesOwnerId, delete)
                    )
                }
                st.map {
                    list
                }
            }
            .fromIOToMain(
                { onCachedDataReceived(it) },
                { onCachedDataReceived(emptyList()) })
    }

    private fun onCachedDataReceived(data: List<Message>) {
        setCacheLoadingNow(false)
        onAllDataLoaded(data, appendToList = false, isCache = true)
        requestAtStart()
    }

    private fun onNetDataReceived(messages: List<Message>, startMessageId: Int?) {
        cacheLoadingDisposable.cancel()

        isLoadingFromDbNow = false
        endOfContent = messages.isEmpty()
        if (chronologyInvert && startMessageId == null)
            endOfContent = true

        setNetLoadingNow(false)
        onAllDataLoaded(messages, startMessageId != null, isCache = false)
    }

    fun fireScrollToUnread() {
        conversation?.unreadCount?.let { view?.scrollToUnread(it, false) }
    }

    fun fireDeleteChatPhoto() {
        appendJob(
            messagesRepository.deleteChatPhoto(accountId, Peer.toChatId(peerId))
                .fromIOToMain(
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
                conversation?.unreadCount?.let {
                    if (conversation?.unreadCount.orZero() <= messages.size) {
                        view?.scrollToUnread(it, true)
                    } else {
                        view?.goToUnreadMessages(
                            accountId,
                            conversation?.inRead.orZero(),
                            lastReadId.incoming,
                            lastReadId.outgoing,
                            conversation?.unreadCount.orZero(),
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
        if (Settings.get().main().isAuto_read) {
            appendJob(
                checkErrorMessages().fromIOToMain({ t -> if (t) startSendService() else readAllUnreadMessagesIfExists() }) { })
        }
    }

    fun fireNetworkChanged() {
        if (!isHiddenAccount(messagesOwnerId) && !isHiddenAccount(messagesOwnerId)) {
            appendJob(
                checkErrorMessages().fromIOToMain({ t -> if (t) startSendService() else readAllUnreadMessagesIfExists() }) { })
        }
    }

    fun fireCheckMessages(incoming: Int, outgoing: Int) {
        if (incoming != -1 && outgoing != -1) {
            lastReadId.incoming = incoming
            lastReadId.outgoing = outgoing
            view?.notifyDataChanged()
        }
        if (Settings.get().main().isAuto_read) {
            appendJob(
                checkErrorMessages().fromIOToMain({ t -> if (t) startSendService() else readAllUnreadMessagesIfExists() }) { })
        }
    }

    @SuppressLint("CheckResult")
    private fun checkErrorMessages(): Flow<Boolean> {
        if (isHiddenAccount(messagesOwnerId) || isHiddenAccount(accountId)) return toFlow(false)
        val list: ArrayList<Int> = ArrayList()
        for (i: Message in data) {
            if (i.status == MessageStatus.ERROR) {
                list.add(i.getObjectId())
            }
        }
        val st = emptyTaskFlow()
        if (list.isNotEmpty()) {
            st.andThen(messagesRepository.enqueueAgainList(messagesOwnerId, list))
        }
        return st.map {
            list.isNotEmpty()
        }
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
        netLoadingDisposable += messagesRepository.getPeerMessages(
            messagesOwnerId,
            peerId,
            COUNT,
            null,
            startMessageId,
            !chronologyInvert,
            chronologyInvert
        )
            .fromIOToMain(
                { messages -> onNetDataReceived(messages, startMessageId) },
                { onMessagesGetError(it) })
    }

    private fun onMessagesGetError(t: Throwable) {
        setNetLoadingNow(false)
        logThrowable("Chat issues", getCauseIfRuntime(t))
        showError(view, getCauseIfRuntime(t))
    }

    private fun requestMore() {
        val lastId = if (data.isNotEmpty()) data[data.size - 1].getObjectId() else null
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
        if (!Settings.get().main().isHint_stickers) {
            return
        }
        stickersWordsDisplayDisposable.cancel()
        if (s.isNullOrEmpty()) {
            view?.updateStickers(emptyList())
            return
        }
        stickersWordsDisplayDisposable +=
            stickersInteractor.getKeywordsStickers(accountId, s.trim())
                .delayedFlow(500)
                .fromIOToMain { stickers -> view?.updateStickers(stickers) }
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
            val wasEmpty = text.isNullOrBlank()
            text = s
            if (wasEmpty != text.isNullOrBlank()) {
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

        if (!isHiddenAccount(messagesOwnerId) && !isHiddenAccount(accountId) && !Settings.get()
                .main().isDont_write
        ) {
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

        val trimmedText = AppTextUtils.safeTrim(draftMessageText, null)
        val encryptionEnabled = securitySettings.isMessageEncryptionEnabled(messagesOwnerId, peerId)

        @KeyLocationPolicy
        var keyLocationPolicy = KeyLocationPolicy.PERSIST
        if (encryptionEnabled) {
            keyLocationPolicy =
                securitySettings.getEncryptionLocationPolicy(messagesOwnerId, peerId)
        }

        val builder = SaveMessageBuilder(messagesOwnerId, peer.id)
            .also {
                it.setText(trimmedText)
                it.setDraftMessageId(draftMessageId)
                it.setRequireEncryption(encryptionEnabled)
                it.setKeyLocationPolicy(keyLocationPolicy)
            }

        val fwds = ArrayList<Message>()

        for (model in outConfig.models) {
            if (model is FwdMessages) {
                fwds.addAll(model.fwds)
            } else {
                builder.attach(model)
            }
        }

        builder.setForwardMessages(fwds)

        outConfig.models.clear()
        outConfig.setInitialText(null)

        draftMessageId = null
        draftMessageText = null
        draftMessageDbAttachmentsCount = 0

        view?.resetInputAttachments()

        resolveAttachmentsCounter()
        resolveDraftMessageText()
        resolvePrimaryButton()

        sendMessage(builder)

        if (outConfig.closeOnSend) {
            view?.doCloseAfterSend()
        }
    }

    @SuppressLint("CheckResult")
    private fun sendMessage(builder: SaveMessageBuilder) {
        if (isHiddenAccount(builder.accountId)) {
            view?.showSnackbar(R.string.read_only_account, true)
            return
        }
        messagesRepository.put(builder)
            .fromIOToMain({
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
                onMessageSaveSuccess(it)
            }, {
                onMessageSaveError(it)
            })
    }

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
            view?.showEditAttachmentsDialog(attachments, isGroupChat)
            return
        }

        if (draftMessageId == null) {
            draftMessageId = Stores.instance
                .messages()
                .saveDraftMessageBody(messagesOwnerId, peerId, draftMessageText)
                .syncSingleSafe()
        }

        val destination = UploadDestination.forMessage(draftMessageId ?: return)
        view?.goToMessageAttachmentsEditor(
            accountId,
            messagesOwnerId,
            destination,
            draftMessageText,
            outConfig.models,
            isGroupChat
        )
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
            view?.displayDraftMessageText(text)
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
            for (model in outConfig.models) {
                if (model is FwdMessages) {
                    fwds.addAll(model.fwds)
                } else {
                    builder.attach(model)
                }
            }
            builder.setForwardMessages(fwds)
            outConfig.models.clear()
            resolveAttachmentsCounter()
        }
        sendMessage(builder)
    }

    fun sendRecordingCustomMessageImpl(context: Context, file: String) {
        val to = File(AudioRecordWrapper.getRecordingDirectory(context), "converted.mp3")
        to.delete()
        view?.customToast?.showToastInfo(R.string.do_convert)
        appendJob(
            flow {
                emit(ToMp4Audio.encodeToMp4Audio(file, to.absolutePath))
            }.fromIOToMain({ o ->
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
                    !isHiddenAccount(messagesOwnerId) && !isHiddenAccount(accountId)
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
        var needReload = false
        for (update in updates) {
            update.statusUpdate.requireNonNull {
                val targetIndex = indexOf(update.messageId)
                if (it.vkid != null) {
                    // message was sent
                    val alreadyExist = indexOf(it.vkid.orZero()) != -1

                    if (alreadyExist) {
                        if (targetIndex != -1) {
                            data.removeAt(targetIndex)
                            if (!needReload) {
                                needReload = true
                            }
                        }
                    } else {
                        if (targetIndex != -1) {
                            val message = data[targetIndex]
                            message.setStatus(it.status)
                            message.setId(it.vkid.orZero())

                            data.removeAt(targetIndex)
                            addMessageToList(message)
                            if (!needReload) {
                                needReload = true
                            }
                        }
                    }
                } else {
                    //message not sent
                    if (targetIndex != -1) {
                        val message = data[targetIndex]
                        message.setStatus(it.status)
                        if (!needReload) {
                            needReload = true
                        }
                    }
                }
            }

            update.deleteUpdate.requireNonNull {
                val targetIndex = indexOf(update.messageId)
                if (targetIndex != -1) {
                    data[targetIndex].setDeleted(it.deleted)
                    data[targetIndex].setDeletedForAll(it.deletedForAll)
                    if (!needReload) {
                        view?.notifyItemChanged(targetIndex)
                    }
                }
            }

            update.importantUpdate.requireNonNull {
                val targetIndex = indexOf(update.messageId)
                if (targetIndex != -1) {
                    data[targetIndex].setImportant(it.important)
                    if (!needReload) {
                        view?.notifyItemChanged(targetIndex)
                    }
                }
            }

            update.reactionUpdate.requireNonNull {
                waitReactionUpdateDisposable.cancel()
                val targetIndex = indexOf(update.messageId, it.peerId)
                if (targetIndex != -1) {
                    if (!it.keepMyReaction) {
                        data[targetIndex].setReactionId(it.reactionId)
                    }
                    data[targetIndex].reactions?.clear()
                    data[targetIndex].setReactions(null)
                    for (react in it.reactions) {
                        data[targetIndex].prepareReactions(it.reactions.size)
                            .add(Entity2Model.buildReactionFromDbo(react))
                    }
                    data[targetIndex].setReactionEditMode(false)
                    if (!needReload) {
                        view?.notifyItemChanged(targetIndex)
                    }
                }
            }
        }
        if (needReload) {
            view?.notifyDataChanged()
        }
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
            fetchConversationDisposable += Repository.messages
                .updateDialogKeyboard(accountId, peerId, message.keyboard)
                .hiddenIO()
            view?.convertToKeyboard(message.keyboard)
        }
        if (Settings.get().main().isAuto_read && !Processors.realtimeMessages
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

    fun resolveWritingInfo(context: Context, writeText: WriteText) {
        appendJob(
            OwnerInfo.getRx(context, accountId, writeText.getFrom_ids()[0])
                .fromIOToMain { t ->
                    view?.displayWriting(
                        t.owner,
                        writeText.getFrom_ids().size,
                        writeText.isText
                    )
                }
        )
    }

    private fun updateSubtitle() {
        subtitle = null

        when (Peer.getType(peerId)) {
            Peer.GROUP -> {
                subtitle = null
                resolveToolbarSubtitle()
            }

            Peer.CHAT -> appendJob(
                messagesRepository.getChatUsers(
                    accountId,
                    Peer.toChatId(peerId)
                )
                    .fromIOToMain({
                        chatAdminsIds = ArrayList()
                        for (i in it) {
                            if (i.isAdmin || i.isOwner) {
                                chatAdminsIds?.add(i.getOwnerObjectId())
                            }
                        }
                        subtitle = getString(R.string.chat_users_count, it.size)
                        resolveToolbarSubtitle()
                    }, { resolveToolbarSubtitle() })
            )


            Peer.USER -> appendJob(
                Stores.instance
                    .owners()
                    .getLocalizedUserActivity(messagesOwnerId, Peer.toUserId(peerId))
                    .fromIOToMain({ s ->
                        subtitle = s
                        resolveToolbarSubtitle()
                    }, { logThrowable("ChatPresenter", it) })
            )
        }
    }

    private fun canEdit(message: Message): Boolean {
        return message.isOut && Unixtime.now() - message.date < 24 * 60 * 60
                && !message.isSticker && !message.isVoiceMessage && !message.isGraffiti && !message.isCall
    }

    private fun canChangePin(): Boolean {
        return conversation?.run {
            hasFlag(acl, Conversation.AclFlags.CAN_CHANGE_PIN)
        } ?: run {
            false
        }
    }

    private fun doStar(): Boolean {
        val selectionCount = countOfSelection(data)
        if (selectionCount <= 0)
            return false
        data.find { it.isSelected }?.isImportant?.let {
            return !it
        } ?: return true
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

    private fun doStar(message: Message): Boolean {
        return !message.isImportant
    }

    private fun canStar(message: Message): Boolean {
        return message.status == MessageStatus.SENT
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

    override fun onGuiResumed() {
        super.onGuiResumed()
        checkLongpoll()
        Processors.realtimeMessages
            .registerNotificationsInterceptor(generatedId, Pair.create(messagesOwnerId, peerId))
    }

    override fun onGuiPaused() {
        super.onGuiPaused()
        checkLongpoll()
        Processors.realtimeMessages.unregisterNotificationsInterceptor(generatedId)
    }

    private fun tryToRestoreDraftMessage(ignoreBody: Boolean) {
        appendJob(
            Stores.instance
                .messages()
                .findDraftMessage(messagesOwnerId, peerId)
                .fromIOToMain(
                    { draft ->
                        if (draft != null) {
                            onDraftMessageRestored(draft, ignoreBody)
                        }
                    },
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
        for (model in outConfig.models) {
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
        for (model in outConfig.models) {
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
            draftMessageDbAttachmentsCount = message.attachmentsCount
            draftMessageId = message.id

            if (!ignoreBody) {
                draftMessageText = message.text
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
        stickersWordsDisplayDisposable.cancel()
        cacheLoadingDisposable.cancel()
        netLoadingDisposable.cancel()
        fetchConversationDisposable.cancel()
        waitReactionUpdateDisposable.cancel()

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
            .hiddenIO()
    }

    override fun onMessageClick(message: Message, position: Int, x: Int?, y: Int?) {
        if (message.status == MessageStatus.ERROR) {
            view?.showErrorSendDialog(message)
        } else {
            if (!readUnreadMessagesUpIfExists(message)) {
                if (x != null && y != null) {
                    resolvePopupMenu(message, position, x, y)
                }
            }
        }
    }

    private fun readUnreadMessagesUpIfExists(message: Message): Boolean {
        if (isHiddenAccount(messagesOwnerId) || isHiddenAccount(accountId)) return false

        if (!message.isOut && message.originalId > lastReadId.incoming) {
            lastReadId.incoming = message.originalId

            view?.notifyDataChanged()

            appendJob(
                messagesRepository.markAsRead(messagesOwnerId, peer.id, message.originalId)
                    .fromIOToMain(dummy()) { t -> showError(view, t) }
            )
            return true
        }
        return false
    }

    private fun readAllUnreadMessagesIfExists() {
        if (isHiddenAccount(messagesOwnerId) || isHiddenAccount(accountId)) return
        val last = if (data.nonNullNoEmpty()) data[0] else return

        if (!last.isOut && last.originalId > lastReadId.incoming) {
            lastReadId.incoming = last.originalId

            view?.notifyDataChanged()

            appendJob(
                messagesRepository.markAsRead(messagesOwnerId, peer.id, last.originalId)
                    .fromIOToMain(dummy()) { t -> showError(view, t) }
            )
        }
    }

    private fun resolvePopupMenu(message: Message, position: Int, x: Int, y: Int) {
        if (isHiddenAccount(messagesOwnerId) || isHiddenAccount(accountId) || !Settings.get()
                .main().isChat_popup_menu || countOfSelection(data) > 0
        ) {
            return
        }
        message.isSelected = true
        safeNotifyItemChanged(position)

        view?.showPopupOptions(
            position,
            x,
            y,
            canEdit(message),
            canChangePin(),
            canStar(message),
            doStar(message),
            !message.isOut
        )
    }

    fun fireMessageRestoreClick(message: Message) {
        restoreMessage(message.getObjectId())
    }

    private fun restoreMessage(messageId: Int) {
        appendJob(
            messagesRepository.restoreMessage(messagesOwnerId, peerId, messageId)
                .fromIOToMain(
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
            appendJob(
                messagesRepository.markAsImportant(
                    messagesOwnerId,
                    peer.id,
                    sent,
                    if (!isImportant) 1 else 0
                ).fromIOToMain(dummy()) { t -> showError(view, t) }
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
            appendJob(
                messagesRepository.deleteMessages(
                    messagesOwnerId, peerId, sent,
                    forAll = false,
                    spam = true
                )
                    .fromIOToMain(dummy()) { t -> showError(view, t) }
            )
        }

        if (hasChanged) {
            view?.notifyDataChanged()
        }
    }

    private fun normalDelete(ids: Collection<Int>, forAll: Boolean) {
        appendJob(
            messagesRepository.deleteMessages(messagesOwnerId, peerId, ids, forAll, false)
                .fromIOToMain(dummy()) { t -> showError(view, t) }
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
        appendJob(
            messagesRepository.edit(
                messagesOwnerId, message, "Ragnarök",
                emptyList(), false
            )
                .fromIOToMain({
                    result.add(it.getObjectId())
                    onMessageEdited(it)
                    superDeleteEditRecursive(messages, result)
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
        appendJob(
            messagesRepository.enqueueAgain(messagesOwnerId, message.getObjectId())
                .fromIOToMain({ startSendService() }, { logThrowable("ChatPresenter", it) })
        )
    }

    private fun deleteMessageFromDbAsync(message: Message) {
        Stores.instance
            .messages()
            .deleteMessage(messagesOwnerId, message.getObjectId())
            .hiddenIO()
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

    fun fireShowProfile() {
        view?.showUserWall(accountId, peerId)
    }

    fun fireLeaveChatClick() {
        val chatId = Peer.toChatId(peerId)
        val accountId = super.accountId

        appendJob(
            messagesRepository.removeChatMember(accountId, chatId, accountId)
                .fromIOToMain(dummy()) { t -> showError(view, t) }
        )
    }

    fun fireChatTitleClick() {
        view?.showChatTitleChangeDialog(peer.getTitle())
    }

    fun fireChatMembersClick() {
        view?.goToChatMembers(accountId, Peer.toChatId(peerId))
    }

    fun fireChatDownloadClick(context: Context, action: String) {
        val downloadWork = OneTimeWorkRequest.Builder(ChatDownloadWorker::class)
        val data = Data.Builder()
        data.putLong(Extra.OWNER_ID, peerId)
        data.putLong(Extra.ACCOUNT_ID, accountId)
        data.putString(Extra.TITLE, conversation?.getDisplayTitle(context))
        data.putString(Extra.ACTION, action)
        downloadWork.setInputData(data.build())
        WorkManager.getInstance(context).enqueue(downloadWork.build())
    }

    fun fireDialogAttachmentsClick() {
        view?.goToConversationAttachments(accountId, peerId)
    }

    fun fireSearchClick() {
        view?.goToSearchMessage(accountId, peer)
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
        view?.convertToKeyboard(conversation?.currentKeyboard)
    }

    private fun uploadStreamsImpl(streams: List<Uri>, size: Int?, is_video: Boolean) {
        outConfig.setUploadFiles(null)
        outConfig.setUploadFilesMimeType(null)

        view?.resetUploadImages()

        if (draftMessageId == null) {
            draftMessageId = Stores.instance
                .messages()
                .saveDraftMessageBody(messagesOwnerId, peerId, draftMessageText)
                .syncSingleSafe()
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
        outConfig.uploadFiles.nonNullNoEmpty {
            outConfig.uploadFilesMimeType.nonNullNoEmpty { s ->
                uploadStreams(it, s)
            }
        }
    }

    fun fireMessagesLookup(message: Message) {
        view?.goToMessagesLookup(accountId, message.peerId, message.getObjectId(), message)
    }

    fun fireChatTitleTyped(newValue: String) {
        val chatId = Peer.toChatId(peerId)

        appendJob(
            messagesRepository.editChat(messagesOwnerId, chatId, newValue)
                .fromIOToMain(dummy()) { t -> showError(view, t) }
        )
    }

    fun fireForwardToHereClick(messages: ArrayList<Message>) {
        for (i in outConfig.models) {
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
            outConfig.models.append(FwdMessages(messages))
        }

        resolveAttachmentsCounter()
        resolvePrimaryButton()
    }

    fun fireForwardToAnotherClick(messages: ArrayList<Message>) {
        view?.forwardMessagesToAnotherConversation(messages, messagesOwnerId)
    }

    override fun onActionModeForwardClick() {
        val selected = getSelected(data)
        if (selected.isNotEmpty()) {
            view?.displayForwardTypeSelectDialog(selected)
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

    fun fireEncryptionStatusClick() {
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
            REQUEST_CODE_KEY_EXCHANGE -> KeyExchangeService.initiateKeyExchangeSession(
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
        appendJob(
            Stores.instance
                .keys(policy)
                .getKeys(messagesOwnerId, peerId)
                .fromIOToMain(
                    { aesKeyPairs -> fireEncryptionEnableClick(policy, aesKeyPairs) },
                    { logThrowable("ChatPresenter", it) })
        )
    }

    private fun fireEncryptionEnableClick(@KeyLocationPolicy policy: Int, pairs: List<AesKeyPair>) {
        if (pairs.isEmpty()) {
            view?.displayInitiateKeyExchangeQuestion(policy)
        } else {
            Settings.get().security().enableMessageEncryption(messagesOwnerId, peerId, policy)
            resolveOptionMenu()
        }
    }

    fun fireInitiateKeyExchangeClick(@KeyLocationPolicy policy: Int) {
        KeyExchangeService.initiateKeyExchangeSession(App.instance, messagesOwnerId, peerId, policy)
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putLong(SAVE_ID, generatedId)
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

    private fun checkGraffitiMessage(filePath: Sticker.LocalSticker): Flow<Optional<IAttachmentToken>> {
        if (filePath.path.nonNullNoEmpty()) {
            val docsApi = Includes.networkInterfaces.vkDefault(accountId).docs()
            return docsApi.getMessagesUploadServer(
                peerId,
                if (filePath.isAnimated) "doc" else "graffiti"
            )
                .flatMapConcat { server ->
                    val file = File(filePath.path)
                    var inputStream: InputStream? = null
                    try {
                        inputStream = FileInputStream(file)
                        Includes.networkInterfaces.uploads()
                            .uploadDocumentRx(
                                server.url ?: throw NotFoundException("Upload url empty!"),
                                if (filePath.isAnimated) filePath.animationName else file.name,
                                inputStream,
                                null
                            )
                            .onCompletion { safelyClose(inputStream) }
                            .flatMapConcat { uploadDto ->
                                if (uploadDto.file.isNullOrEmpty()) {
                                    toFlowThrowable(NotFoundException("VK doesn't upload this file"))
                                } else {
                                    docsApi
                                        .save(uploadDto.file, null, null)
                                        .map { dtos ->
                                            if (dtos.type.isEmpty()) {
                                                throw NotFoundException("Unable to save graffiti message")
                                            } else {
                                                val dto = dtos.doc
                                                val token = AttachmentsTokenCreator.ofDocument(
                                                    dto.id,
                                                    dto.ownerId,
                                                    dto.accessKey
                                                )
                                                Optional.wrap(token)
                                            }
                                        }
                                }
                            }
                    } catch (e: FileNotFoundException) {
                        safelyClose(inputStream)
                        toFlowThrowable(e)
                    }
                }
        }
        return toFlow(Optional.empty())
    }

    fun fireSendMyStickerClick(file: Sticker.LocalSticker) {
        view?.scrollTo(0)
        netLoadingDisposable += checkGraffitiMessage(file)
            .fromIOToMain({
                if (it.nonEmpty()) {
                    val kk = it.get() as AttachmentTokens.AttachmentToken
                    val builder = SaveMessageBuilder(messagesOwnerId, peerId)

                    val fwds = ArrayList<Message>()
                    for (model in outConfig.models) {
                        if (model is FwdMessages) {
                            fwds.addAll(model.fwds)
                        }
                    }
                    if (fwds.size == 1) {
                        builder.setForwardMessages(fwds)
                        outConfig.models.clear()
                        view?.resetInputAttachments()
                        resolveAttachmentsCounter()
                    }

                    if (!file.isAnimated) {
                        val graffiti = Graffiti().setId(kk.id).setOwner_id(kk.ownerId)
                            .setAccess_key(kk.accessKey)
                        builder.attach(graffiti)
                        sendMessage(builder)
                    } else {
                        val doc = Document(kk.id, kk.ownerId).setAccessKey(kk.accessKey)
                        builder.attach(doc)
                        sendMessage(builder)
                    }
                }
            }, { onConversationFetchFail(it) })
    }

    fun fireStickerSendClick(sticker: Sticker) {
        view?.scrollTo(0)
        val builder = SaveMessageBuilder(messagesOwnerId, peerId).attach(sticker)

        val fwds = ArrayList<Message>()
        for (model in outConfig.models) {
            if (model is FwdMessages) {
                fwds.addAll(model.fwds)
            }
        }
        if (fwds.size == 1) {
            builder.setForwardMessages(fwds)
            outConfig.models.clear()
            view?.resetInputAttachments()
            resolveAttachmentsCounter()
        }
        sendMessage(builder)
    }

    fun fireReactionClicked(reaction_id: Int?, conversation_message_id: Int, peerId: Long) {
        if (isHiddenAccount(messagesOwnerId) || isHiddenAccount(accountId)) {
            return
        }
        netLoadingDisposable += messagesRepository.sendOrDeleteReaction(
            messagesOwnerId,
            peerId,
            conversation_message_id,
            reaction_id
        )
            .fromIOToMain({
                waitReactionUpdateDisposable.cancel()
                waitReactionUpdateDisposable += delayTaskFlow(1000)
                    .toMain {
                        val res = indexOf(conversation_message_id, peerId)
                        if (res != -1) {
                            data[res].setReactionEditMode(false)
                            if (data[res].reactions == null) {
                                data[res].setReactions(ArrayList())
                            }
                            if (reaction_id != null) {
                                val reaction = data[res].getReactionById(reaction_id)
                                if (reaction != null) {
                                    reaction.setCount(reaction.count + 1)
                                } else {
                                    data[res].reactions?.add(
                                        Reaction().setReactionId(reaction_id).setCount(1)
                                    )
                                }
                                if (data[res].reaction_id != 0) {
                                    data[res].getReactionById(data[res].reaction_id)?.let {
                                        it.setCount(it.count - 1)
                                        if (it.count <= 0) {
                                            data[res].getReactionIndexById(data[res].reaction_id)
                                                ?.let { it1 -> data[res].reactions?.removeAt(it1) }
                                        }
                                    }
                                }
                                data[res].setReactionId(reaction_id)
                            } else if (data[res].reaction_id != 0) {
                                val reaction = data[res].getReactionById(data[res].reaction_id)
                                reaction?.let {
                                    reaction.setCount(reaction.count - 1)
                                    if (reaction.count <= 0) {
                                        data[res].getReactionIndexById(data[res].reaction_id)
                                            ?.let { it1 -> data[res].reactions?.removeAt(it1) }
                                    }
                                }
                                data[res].setReactionId(0)
                            }
                            view?.notifyItemChanged(res)
                        }
                    }
            }, { onConversationFetchFail(it) })
    }

    fun fireBotSendClick(item: Keyboard.Button, context: Context) {
        if (item.type == "open_link") {
            LinkHelper.openLinkInBrowser(context, item.link)
            return
        }
        view?.scrollTo(0)
        val builder =
            SaveMessageBuilder(messagesOwnerId, peerId).setPayload(item.payload).setText(item.label)
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
        netLoadingDisposable += utilsInteractor.getInviteLink(accountId, peerId, 0)
            .fromIOToMain({
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

            appendJob(
                messagesRepository.edit(accountId, message, text, models, keepForward)
                    .fromIOToMain({ onMessageEdited(it) }, { t -> onMessageEditFail(t) })
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

    fun fireLongAvatarClick(uId: Long) {
        if (uId > 0) {
            netLoadingDisposable += Repository.owners.getFullUserInfo(accountId, uId, MODE_NET)
                .fromIOToMain({ info ->
                    run {
                        val Dmn: String = if (info.first?.domain == null)
                            "@id$uId,"
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

    fun fireCompressSettings(context: Context) {
        if (isGroupChat) {
            view?.openPollCreationWindow(accountId, messagesOwnerId)
            return
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.select_image_size_title))
            .setSingleChoiceItems(
                R.array.array_image_sizes_settings_names,
                Settings.get().main().uploadImageSizePref
            ) { dialogInterface, j ->
                Settings.get().main().uploadImageSize = j
                dialogInterface.dismiss()
            }
            .setCancelable(true)
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    fun fireEditLocalPhotosSelected(localPhotos: List<LocalPhoto>, imageSize: Int) {
        edited?.run {
            if (localPhotos.isNotEmpty()) {
                val destination = UploadDestination.forMessage(message.getObjectId())

                val intents = localPhotos.map {
                    UploadIntent(accountId, destination).apply {
                        pAutoCommit = false
                        fileId = it.imageId
                        pFileUri = it.fullImageUri
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
                pFileUri = Uri.parse(video.data.toString())
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
        appendJob(
            messagesRepository.pin(accountId, peerId, message)
                .fromIOToMain(dummy()) { onPinFail(it) }
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
        private val IDGEN = AtomicLong()
        private const val SAVE_ID = "save_presenter_chat_id"

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
