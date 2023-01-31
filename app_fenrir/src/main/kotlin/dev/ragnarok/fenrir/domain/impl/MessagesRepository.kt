package dev.ragnarok.fenrir.domain.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.local_json.ChatJsonResponse
import dev.ragnarok.fenrir.api.model.longpoll.*
import dev.ragnarok.fenrir.crypt.CryptHelper.encryptWithAes
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.crypt.KeyPairDoesNotExistException
import dev.ragnarok.fenrir.db.PeerStateEntity
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.MessageEditEntity
import dev.ragnarok.fenrir.db.model.MessagePatch
import dev.ragnarok.fenrir.db.model.MessagePatch.Important
import dev.ragnarok.fenrir.db.model.PeerPatch
import dev.ragnarok.fenrir.db.model.entity.DialogDboEntity
import dev.ragnarok.fenrir.db.model.entity.MessageDboEntity
import dev.ragnarok.fenrir.db.model.entity.SimpleDialogEntity
import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity
import dev.ragnarok.fenrir.domain.IMessagesDecryptor
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.InteractorFactory.createAccountInteractor
import dev.ragnarok.fenrir.domain.Mode
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapConversation
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapDialog
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapMessage
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformMessages
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Entity2Dto.createToken
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildDialogFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildKeyboardFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillOwnerIds
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.message
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.domain.mappers.Model2Dto.createTokens
import dev.ragnarok.fenrir.domain.mappers.Model2Entity.buildDboAttachments
import dev.ragnarok.fenrir.domain.mappers.Model2Entity.buildDialog
import dev.ragnarok.fenrir.domain.mappers.Model2Entity.buildKeyboardEntity
import dev.ragnarok.fenrir.domain.mappers.Model2Entity.buildMessageEntity
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.exception.UploadNotResolvedException
import dev.ragnarok.fenrir.longpoll.NotificationHelper.tryCancelNotificationForPeer
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.MessageUpdate.ImportantUpdate
import dev.ragnarok.fenrir.model.MessageUpdate.StatusUpdate
import dev.ragnarok.fenrir.model.criteria.DialogsCriteria
import dev.ragnarok.fenrir.model.criteria.MessagesCriteria
import dev.ragnarok.fenrir.push.OwnerInfo
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.settings.ISettings.IAccountsSettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.Method
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination.Companion.forMessage
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Unixtime.now
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.hasFlag
import dev.ragnarok.fenrir.util.Utils.isHiddenAccount
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNullMutable
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.VKOwnIds
import dev.ragnarok.fenrir.util.WeakMainLooperHandler
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import dev.ragnarok.fenrir.util.rxutils.RxUtils.safelyCloseAction
import dev.ragnarok.fenrir.util.serializeble.json.decodeFromStream
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleTransformer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.processors.PublishProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors

class MessagesRepository(
    private val accountsSettings: IAccountsSettings,
    private val networker: INetworker,
    private val ownersRepository: IOwnersRepository,
    private val storages: IStorages,
    uploadManager: IUploadManager
) : IMessagesRepository {
    private val decryptor: IMessagesDecryptor
    private val uploadManager: IUploadManager
    private val peerUpdatePublisher = PublishProcessor.create<List<PeerUpdate>>()
    private val peerDeletingPublisher = PublishProcessor.create<PeerDeleting>()
    private val messageUpdatesPublisher = PublishProcessor.create<List<MessageUpdate>>()
    private val writeTextPublisher = PublishProcessor.create<List<WriteText>>()
    private val sentMessagesPublisher = PublishProcessor.create<SentMsg>()
    private val sendErrorsPublisher = PublishProcessor.create<Throwable>()
    private val compositeDisposable = CompositeDisposable()
    private val senderScheduler = Schedulers.from(Executors.newFixedThreadPool(1))
    private val handler = InternalHandler(this)
    private var nowSending = false
    private var registeredAccounts: List<Long>? = null
    override fun observeMessagesSendErrors(): Flowable<Throwable> {
        return sendErrorsPublisher.onBackpressureBuffer()
    }

    override fun observeTextWrite(): Flowable<List<WriteText>> {
        return writeTextPublisher.onBackpressureBuffer()
    }

    private fun onAccountsChanged() {
        registeredAccounts = accountsSettings.registered
    }

    override fun runSendingQueue() {
        handler.runSend()
    }

    /**
     * Отправить первое неотправленное сообщение
     */
    @MainThread
    internal fun send() {
        if (nowSending) {
            return
        }
        nowSending = true
        sendMessage(registeredAccounts())
    }

    private fun registeredAccounts(): List<Long>? {
        if (registeredAccounts == null) {
            registeredAccounts = accountsSettings.registered
        }
        return registeredAccounts
    }

    private fun onMessageSent(msg: SentMsg) {
        nowSending = false
        sentMessagesPublisher.onNext(msg)
        send()
    }

    private fun onMessageSendError(t: Throwable) {
        val cause = getCauseIfRuntime(t)
        nowSending = false
        if (cause is NotFoundException) {
            val accountId = Settings.get().accounts().current
            if (!Settings.get().other().isBe_online || isHiddenAccount(accountId)) {
                compositeDisposable.add(
                    createAccountInteractor().setOffline(accountId)
                        .subscribeOn(senderScheduler)
                        .observeOn(provideMainThreadScheduler())
                        .subscribe(ignore(), ignore())
                )
            }
            // no unsent messages
            return
        }
        sendErrorsPublisher.onNext(t)
    }

    private fun sendMessage(accountIds: Collection<Long>?) {
        nowSending = true
        compositeDisposable.add(sendUnsentMessage(accountIds ?: return)
            .subscribeOn(senderScheduler)
            .observeOn(provideMainThreadScheduler())
            .subscribe({ msg -> onMessageSent(msg) }) { t ->
                onMessageSendError(
                    t
                )
            })
    }

    private fun onUpdloadSuccess(upload: Upload) {
        val accountId = upload.accountId
        val messagesId = upload.destination.id
        compositeDisposable.add(uploadManager[accountId, upload.destination]
            .flatMap { uploads ->
                if (uploads.isNotEmpty()) {
                    return@flatMap Single.just(false)
                }
                storages.messages().getMessageStatus(accountId, messagesId)
                    .flatMap { status ->
                        if (status != MessageStatus.WAITING_FOR_UPLOAD) {
                            Single.just(false)
                        } else {
                            changeMessageStatus(
                                accountId,
                                messagesId,
                                MessageStatus.QUEUE,
                                null
                            ).andThen(Single.just(true))
                        }
                    }
            }
            .subscribe({ needStartSendingQueue ->
                if (needStartSendingQueue) {
                    runSendingQueue()
                }
            }, ignore())
        )
    }

    override fun handleFlagsUpdates(
        accountId: Long,
        setUpdates: List<MessageFlagsSetUpdate>?,
        resetUpdates: List<MessageFlagsResetUpdate>?
    ): Completable {
        val patches: MutableList<MessagePatch> = ArrayList()
        if (setUpdates.nonNullNoEmpty()) {
            for (update in setUpdates) {
                if (!hasFlag(update.mask, VKApiMessage.FLAG_DELETED)
                    && !hasFlag(update.mask, VKApiMessage.FLAG_IMPORTANT)
                    && !hasFlag(update.mask, VKApiMessage.FLAG_DELETED_FOR_ALL)
                ) continue
                val patch = MessagePatch(update.messageId, update.peerId)
                if (hasFlag(update.mask, VKApiMessage.FLAG_DELETED)) {
                    val forAll = hasFlag(update.mask, VKApiMessage.FLAG_DELETED_FOR_ALL)
                    patch.deletion = MessagePatch.Deletion(true, forAll)
                }
                if (hasFlag(update.mask, VKApiMessage.FLAG_IMPORTANT)) {
                    patch.important = Important(true)
                }
                patches.add(patch)
            }
        }
        if (resetUpdates.nonNullNoEmpty()) {
            for (update in resetUpdates) {
                if (!hasFlag(update.mask, VKApiMessage.FLAG_DELETED) && !hasFlag(
                        update.mask,
                        VKApiMessage.FLAG_IMPORTANT
                    )
                ) continue
                val patch = MessagePatch(update.messageId, update.peerId)
                if (hasFlag(update.mask, VKApiMessage.FLAG_DELETED)) {
                    patch.deletion = MessagePatch.Deletion(deleted = false, deletedForAll = false)
                }
                if (hasFlag(update.mask, VKApiMessage.FLAG_IMPORTANT)) {
                    patch.important = Important(false)
                }
                patches.add(patch)
            }
        }
        return applyMessagesPatchesAndPublish(accountId, patches)
    }

    override fun handleWriteUpdates(
        accountId: Long,
        updates: List<WriteTextInDialogUpdate>?
    ): Completable {
        return Completable.fromAction {
            if (updates.nonNullNoEmpty()) {
                val list: MutableList<WriteText> = ArrayList()
                for (update in updates) {
                    list.add(WriteText(accountId, update.peer_id, update.from_ids, update.is_text))
                }
                writeTextPublisher.onNext(list)
            }
        }
    }

    override fun updateDialogKeyboard(
        accountId: Long,
        peerId: Long,
        keyboard: Keyboard?
    ): Completable {
        return storages.dialogs()
            .updateDialogKeyboard(accountId, peerId, buildKeyboardEntity(keyboard))
    }

    override fun handleUnreadBadgeUpdates(
        accountId: Long,
        updates: List<BadgeCountChangeUpdate>?
    ): Completable {
        return Completable.fromAction {
            if (updates.nonNullNoEmpty()) {
                for (update in updates) {
                    storages.dialogs().setUnreadDialogsCount(accountId, update.count)
                }
            }
        }
    }

    @StringRes
    private fun getTypeUser(ownr: OwnerInfo): Int {
        return if (ownr.owner.ownerType == OwnerType.USER) {
            when (ownr.user.sex) {
                Sex.MAN -> R.string.user_readed_yor_message_man
                Sex.WOMAN -> R.string.user_readed_yor_message_woman
                Sex.UNKNOWN -> R.string.user_readed_yor_message
                else -> R.string.user_readed_yor_message
            }
        } else R.string.user_readed_yor_message
    }

    override fun handleReadUpdates(
        accountId: Long,
        setUpdates: List<OutputMessagesSetReadUpdate>?,
        resetUpdates: List<InputMessagesSetReadUpdate>?
    ): Completable {
        val patches: MutableList<PeerPatch> = ArrayList()
        if (setUpdates.nonNullNoEmpty()) {
            for (update in setUpdates) {
                if (!Settings.get().other().isDisable_notifications && Settings.get()
                        .other().isInfo_reading && update.peerId < VKApiMessage.CHAT_PEER
                ) {
                    compositeDisposable.add(
                        getRx(
                            provideApplicationContext(),
                            Settings.get().accounts().current,
                            update.peerId
                        )
                            .fromIOToMain()
                            .subscribe({ userInfo ->
                                createCustomToast(
                                    provideApplicationContext()
                                ).setBitmap(userInfo.avatar).showToastInfo(
                                    userInfo.owner.fullName + " " + provideApplicationContext().getString(
                                        getTypeUser(userInfo)
                                    )
                                )
                            }) { })
                }
                patches.add(PeerPatch(update.peerId).withOutRead(update.localId))
            }
        }
        if (resetUpdates.nonNullNoEmpty()) {
            for (update in resetUpdates) {
                val patch = PeerPatch(update.peerId).withInRead(update.localId)
                    .withUnreadCount(update.unreadCount)
                if (update.peerId == accountId) {
                    patch.withOutRead(update.localId)
                }
                patches.add(
                    patch
                )
                tryCancelNotificationForPeer(provideApplicationContext(), accountId, update.peerId)
            }
        }
        return applyPeerUpdatesAndPublish(accountId, patches)
    }

    override fun observeSentMessages(): Flowable<SentMsg> {
        return sentMessagesPublisher.onBackpressureBuffer()
    }

    override fun observeMessageUpdates(): Flowable<List<MessageUpdate>> {
        return messageUpdatesPublisher.onBackpressureBuffer()
    }

    override fun observePeerUpdates(): Flowable<List<PeerUpdate>> {
        return peerUpdatePublisher.onBackpressureBuffer()
    }

    override fun observePeerDeleting(): Flowable<PeerDeleting> {
        return peerDeletingPublisher.onBackpressureBuffer()
    }

    override fun getConversationSingle(
        accountId: Long,
        peerId: Long,
        mode: Mode
    ): Single<Conversation> {
        val cached = getCachedConversation(accountId, peerId)
        val actual = getActualConversation(accountId, peerId)
        when (mode) {
            Mode.ANY -> return cached.flatMap { optional ->
                if (optional.isEmpty) actual else Single.just(
                    optional.requireNonEmpty()
                )
            }

            Mode.NET -> return actual
            Mode.CACHE -> return cached
                .flatMap { optional ->
                    if (optional.isEmpty) Single.error(
                        NotFoundException()
                    ) else Single.just(optional.requireNonEmpty())
                }

            else -> {}
        }
        throw IllegalArgumentException("Unsupported mode: $mode")
    }

    private fun getCachedConversation(
        accountId: Long,
        peerId: Long
    ): Single<Optional<Conversation>> {
        return storages.dialogs()
            .findSimple(accountId, peerId)
            .flatMap { optional ->
                if (optional.isEmpty) {
                    return@flatMap Single.just(empty<Conversation>())
                } else {
                    Single.just(optional.requireNonEmpty())
                        .compose(simpleEntity2Conversation(accountId, emptyList()))
                        .map { wrap(it) }
                }
            }
    }

    private fun getActualConversation(accountId: Long, peerId: Long): Single<Conversation> {
        return networker.vkDefault(accountId)
            .messages()
            .getConversations(listOf(peerId), true, Fields.FIELDS_BASE_OWNER)
            .flatMap { response ->
                if (response.items.isNullOrEmpty()) {
                    return@flatMap Single.error<Conversation>(NotFoundException())
                }
                val dto = response.items?.get(0) ?: return@flatMap Single.error<Conversation>(
                    NotFoundException()
                )
                val entity = mapConversation(dto, response.contacts)
                    ?: return@flatMap Single.error<Conversation>(
                        NotFoundException()
                    )
                val existsOwners = transformOwners(response.profiles, response.groups)
                val ownerEntities = mapOwners(response.profiles, response.groups)
                ownersRepository.insertOwners(accountId, ownerEntities)
                    .andThen(storages.dialogs().saveSimple(accountId, entity))
                    .andThen(Single.just(entity))
                    .compose(simpleEntity2Conversation(accountId, existsOwners))
            }
    }

    override fun getConversation(
        accountId: Long,
        peerId: Long,
        mode: Mode
    ): Flowable<Conversation> {
        val cached = getCachedConversation(accountId, peerId)
        val actual = getActualConversation(accountId, peerId)
        return when (mode) {
            Mode.ANY -> cached
                .flatMap { optional ->
                    if (optional.isEmpty) actual else Single.just(
                        optional.requireNonEmpty()
                    )
                }
                .toFlowable()

            Mode.NET -> actual.toFlowable()
            Mode.CACHE -> cached
                .flatMap { optional ->
                    if (optional.isEmpty) Single.error(
                        NotFoundException()
                    ) else Single.just(optional.requireNonEmpty())
                }
                .toFlowable()

            Mode.CACHE_THEN_ACTUAL -> {
                val cachedFlowable = cached.toFlowable()
                    .filter { it.nonEmpty() }
                    .map { it.requireNonEmpty() }
                Flowable.concat(cachedFlowable, actual.toFlowable())
            }
        }
    }

    private fun simpleEntity2Conversation(
        accountId: Long,
        existingOwners: Collection<Owner>
    ): SingleTransformer<SimpleDialogEntity, Conversation> {
        return SingleTransformer { single: Single<SimpleDialogEntity> ->
            single
                .flatMap { entity ->
                    val owners = VKOwnIds()
                    if (Peer.isGroup(entity.peerId) || Peer.isUser(
                            entity.peerId
                        )
                    ) {
                        owners.append(entity.peerId)
                    }
                    if (entity.pinned != null) {
                        fillOwnerIds(owners, listOf(entity.pinned))
                    }
                    ownersRepository.findBaseOwnersDataAsBundle(
                        accountId,
                        owners.all,
                        IOwnersRepository.MODE_ANY,
                        existingOwners
                    )
                        .map { bundle -> entity2Model(accountId, entity, bundle) }
                }
        }
    }

    override fun edit(
        accountId: Long,
        message: Message,
        body: String?,
        attachments: List<AbsModel>,
        keepForwardMessages: Boolean
    ): Single<Message> {
        val attachmentTokens = createTokens(attachments)
        return networker.vkDefault(accountId)
            .messages()
            .edit(
                message.peerId,
                message.getObjectId(),
                body,
                attachmentTokens,
                keepForwardMessages,
                null
            )
            .andThen(getById(accountId, message.getObjectId()))
    }

    override fun getCachedPeerMessages(
        accountId: Long,
        peerId: Long
    ): Single<List<Message>> {
        val criteria = MessagesCriteria(accountId, peerId)
        return storages.messages()
            .getByCriteria(criteria, withAtatchments = true, withForwardMessages = true)
            .compose(entities2Models(accountId))
            .compose(decryptor.withMessagesDecryption(accountId))
    }

    override fun getMessagesFromLocalJSon(
        accountId: Long,
        context: Context
    ): Single<Pair<Peer, List<Message>>> {
        return Single.create { its ->
            val b =
                (context as Activity).intent.data?.let {
                    context.contentResolver.openInputStream(
                        it
                    )
                }
            val resp = b?.let { kJson.decodeFromStream(ChatJsonResponse.serializer(), it) }
            b?.close()
            if (resp == null || resp.page_title.isNullOrEmpty()) {
                its.tryOnError(Throwable("parsing error"))
                return@create
            }
            val ids = VKOwnIds().append(resp.messages)
            its.onSuccess(
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ids.all,
                    IOwnersRepository.MODE_ANY,
                    emptyList()
                )
                    .map {
                        Pair(
                            Peer(resp.page_id).setAvaUrl(resp.page_avatar)
                                .setTitle(resp.page_title),
                            transformMessages(resp.page_id, resp.messages.orEmpty(), it)
                        )
                    }.blockingGet()
            )
        }
    }

    override fun getCachedDialogs(accountId: Long): Single<List<Dialog>> {
        val criteria = DialogsCriteria(accountId)
        return storages.dialogs()
            .getDialogs(criteria)
            .flatMap { dbos ->
                val ownIds = VKOwnIds()
                for (dbo in dbos) {
                    when (Peer.getType(dbo.peerId)) {
                        Peer.GROUP, Peer.USER -> ownIds.append(dbo.peerId)
                        Peer.CHAT, Peer.CONTACT -> ownIds.append(dbo.message?.fromId)
                    }
                }
                ownersRepository
                    .findBaseOwnersDataAsBundle(accountId, ownIds.all, IOwnersRepository.MODE_ANY)
                    .flatMap { owners ->
                        val messages: MutableList<Message> = ArrayList(0)
                        val dialogs: MutableList<Dialog> = ArrayList(dbos.size)
                        for (dbo in dbos) {
                            val dialog = buildDialogFromDbo(accountId, dbo, owners)
                            dialogs.add(dialog)
                            if (dbo.message?.isEncrypted == true) {
                                dialog.message?.let { messages.add(it) }
                            }
                        }
                        if (messages.nonNullNoEmpty()) {
                            Single.just<List<Message>>(
                                messages
                            )
                                .compose(decryptor.withMessagesDecryption(accountId))
                                .map<List<Dialog>> { dialogs }
                        } else {
                            Single.just<List<Dialog>>(dialogs)
                        }
                    }
            }
    }

    private fun getById(accountId: Long, messageId: Int): Single<Message> {
        return networker.vkDefault(accountId)
            .messages()
            .getById(listOf(messageId))
            .map { dtos ->
                mapAll(dtos) {
                    mapMessage(
                        it
                    )
                }
            }
            .compose(entities2Models(accountId))
            .flatMap { messages ->
                if (messages.isEmpty()) {
                    return@flatMap Single.error<Message>(NotFoundException())
                }
                Single.just(messages[0])
            }
    }

    private fun entities2Models(accountId: Long): SingleTransformer<List<MessageDboEntity>, List<Message>> {
        return SingleTransformer { single: Single<List<MessageDboEntity>> ->
            single
                .flatMap { dbos ->
                    val ownIds = VKOwnIds()
                    fillOwnerIds(ownIds, dbos)
                    ownersRepository
                        .findBaseOwnersDataAsBundle(
                            accountId,
                            ownIds.all,
                            IOwnersRepository.MODE_ANY
                        )
                        .map<List<Message>> { owners ->
                            val messages: MutableList<Message> =
                                ArrayList(dbos.size)
                            for (dbo in dbos) {
                                messages.add(message(accountId, dbo, owners))
                            }
                            messages
                        }
                }
        }
    }

    private fun insertPeerMessages(
        accountId: Long,
        peerId: Long,
        messages: List<VKApiMessage>,
        clearBefore: Boolean
    ): Completable {
        return Single.just(messages)
            .compose(DTO_TO_DBO)
            .flatMapCompletable { dbos: List<MessageDboEntity> ->
                storages.messages().insertPeerDbos(accountId, peerId, dbos, clearBefore)
            }
    }

    override fun insertMessages(accountId: Long, messages: List<VKApiMessage>): Completable {
        return Single.just(messages)
            .compose(DTO_TO_DBO)
            .flatMap { dbos -> storages.messages().insert(accountId, dbos) }
            .flatMapCompletable {
                val peers: MutableSet<Long> = HashSet()
                for (m in messages) {
                    peers.add(m.peer_id)
                }
                storages.dialogs()
                    .findPeerStates(accountId, peers)
                    .flatMapCompletable { peerStates: List<PeerStateEntity> ->
                        val patches: MutableList<PeerPatch> = ArrayList(peerStates.size)
                        for (state in peerStates) {
                            var unread = state.unreadCount
                            var messageId = state.lastMessageId
                            for (m in messages) {
                                if (m.peer_id != state.peerId) continue
                                if (m.out) {
                                    unread = 0
                                } else {
                                    unread++
                                }
                                if (m.id > messageId) {
                                    messageId = m.id
                                }
                            }
                            patches.add(
                                PeerPatch(state.peerId)
                                    .withUnreadCount(unread)
                                    .withLastMessage(messageId)
                            )
                        }
                        applyPeerUpdatesAndPublish(accountId, patches)
                    }
            }
    }

    private fun applyPeerUpdatesAndPublish(accountId: Long, patches: List<PeerPatch>): Completable {
        val updates: MutableList<PeerUpdate> = ArrayList()
        for (p in patches) {
            val update = PeerUpdate(accountId, p.id)
            p.inRead.requireNonNull {
                update.readIn = PeerUpdate.Read(it.id)
            }
            p.outRead.requireNonNull {
                update.readOut = PeerUpdate.Read(it.id)
            }
            p.lastMessage.requireNonNull {
                update.lastMessage = PeerUpdate.LastMessage(it.id)
            }
            p.unread.requireNonNull {
                update.unread = PeerUpdate.Unread(it.count)
            }
            p.title.requireNonNull {
                update.title = PeerUpdate.Title(it.title)
            }
            updates.add(update)
        }
        return storages.dialogs().applyPatches(accountId, patches)
            .doOnComplete { peerUpdatePublisher.onNext(updates) }
    }

    override fun getImportantMessages(
        accountId: Long, count: Int, offset: Int?,
        startMessageId: Int?
    ): Single<List<Message>> {
        return networker.vkDefault(accountId)
            .messages()
            .getImportantMessages(offset, count, startMessageId, true, Fields.FIELDS_BASE_OWNER)
            .flatMap { response ->
                val dtos: MutableList<VKApiMessage> =
                    if (response.messages == null) mutableListOf() else listEmptyIfNullMutable(
                        response.messages?.items
                    )
                if (startMessageId != null && dtos.nonNullNoEmpty() && startMessageId == dtos[0].id) {
                    dtos.removeAt(0)
                }
                val completable = Completable.complete()
                val ownerIds = VKOwnIds()
                ownerIds.append(dtos)
                val existsOwners = transformOwners(response.profiles, response.groups)
                val ownerEntities = mapOwners(response.profiles, response.groups)
                completable
                    .andThen(
                        ownersRepository
                            .findBaseOwnersDataAsBundle(
                                accountId,
                                ownerIds.all,
                                IOwnersRepository.MODE_ANY,
                                existsOwners
                            )
                            .flatMap {
                                val insertCompletable =
                                    ownersRepository.insertOwners(accountId, ownerEntities)
                                val messages: MutableList<Message> =
                                    ArrayList(dtos.size)
                                for (dto in dtos) {
                                    messages.add(transform(accountId, dto, it))
                                }
                                insertCompletable.andThen(
                                    Single.just<List<Message>>(messages)
                                        .compose(decryptor.withMessagesDecryption(accountId))
                                )
                            })
            }
    }

    override fun getJsonHistory(
        accountId: Long,
        offset: Int?,
        count: Int?,
        peerId: Long
    ): Single<List<String>> {
        return networker.vkDefault(accountId)
            .messages()
            .getJsonHistory(offset, count, peerId)
            .flatMap { response ->
                val dtos = listEmptyIfNull(
                    response.items
                )
                val messages: MutableList<String> = ArrayList(dtos.size)
                for (i in dtos) {
                    i.json_data.nonNullNoEmpty {
                        messages.add(it)
                    }
                }
                Single.just<List<String>>(messages)
            }
    }

    override fun getPeerMessages(
        accountId: Long, peerId: Long, count: Int, offset: Int?,
        startMessageId: Int?, cacheData: Boolean, rev: Boolean
    ): Single<List<Message>> {
        var pCount = count
        if (rev) pCount = 200
        return networker.vkDefault(accountId)
            .messages()
            .getHistory(
                offset,
                pCount,
                peerId,
                startMessageId,
                rev,
                true,
                Fields.FIELDS_BASE_OWNER
            )
            .flatMap { response ->
                val dtos: MutableList<VKApiMessage> = listEmptyIfNullMutable(response.messages)
                var patch: PeerPatch? = null
                if (startMessageId == null && cacheData && response.conversations.nonNullNoEmpty()) {
                    val conversation =
                        response.conversations?.get(0) ?: throw NullPointerException("WTF!")
                    patch = PeerPatch(peerId)
                        .withOutRead(conversation.outRead)
                        .withInRead(conversation.inRead)
                        .withLastMessage(conversation.lastMessageId)
                        .withUnreadCount(conversation.unreadCount)
                }
                if (startMessageId != null && dtos.nonNullNoEmpty() && startMessageId == dtos[0].id) {
                    dtos.removeAt(0)
                }
                var completable: Completable
                if (cacheData) {
                    completable =
                        insertPeerMessages(accountId, peerId, dtos, startMessageId == null)
                    if (patch != null) {
                        completable = completable.andThen(
                            applyPeerUpdatesAndPublish(
                                accountId,
                                listOf(patch)
                            )
                        )
                    }
                } else {
                    completable = Completable.complete()
                }
                val ownerIds = VKOwnIds()
                ownerIds.append(dtos)
                val existsOwners = transformOwners(response.profiles, response.groups)
                val ownerEntities = mapOwners(response.profiles, response.groups)
                completable
                    .andThen(
                        ownersRepository
                            .findBaseOwnersDataAsBundle(
                                accountId,
                                ownerIds.all,
                                IOwnersRepository.MODE_ANY,
                                existsOwners
                            )
                            .flatMap {
                                val insertCompletable =
                                    ownersRepository.insertOwners(accountId, ownerEntities)
                                if (startMessageId == null && cacheData) {
                                    // Это важно !!!
                                    // Если мы получаем сообщения сначала и кэшируем их в базу,
                                    // то нельзя отдать этот список в ответ (как сделано чуть ниже)
                                    // Так как мы теряем сообщения со статусами, отличными от SENT
                                    insertCompletable.andThen(
                                        getCachedPeerMessages(
                                            accountId,
                                            peerId
                                        )
                                    )
                                } else {
                                    val messages: MutableList<Message> =
                                        ArrayList(dtos.size)
                                    for (dto in dtos) {
                                        messages.add(transform(accountId, dto, it))
                                    }
                                    insertCompletable.andThen(
                                        Single.just<List<Message>>(messages)
                                            .compose(decryptor.withMessagesDecryption(accountId))
                                    )
                                }
                            })
            }
    }

    override fun insertDialog(accountId: Long, dialog: Dialog): Completable {
        val dialogsStore = storages.dialogs()
        return dialogsStore.insertDialogs(accountId, listOf(buildDialog(dialog)), false)
    }

    override fun getDialogs(
        accountId: Long,
        count: Int,
        startMessageId: Int?
    ): Single<List<Dialog>> {
        val clear = startMessageId == null
        val dialogsStore = storages.dialogs()
        return networker.vkDefault(accountId)
            .messages()
            .getDialogs(null, count, startMessageId, true, Fields.FIELDS_BASE_OWNER)
            .map { response ->
                if (startMessageId != null && safeCountOf(response.dialogs) > 0) {
                    // remove first item, because we will have duplicate with previous response
                    response.dialogs?.removeAt(0)
                }
                response
            }
            .flatMap { response ->
                val apiDialogs: List<VKApiDialog> = listEmptyIfNull(response.dialogs)
                val ownerIds: Collection<Long> = if (apiDialogs.nonNullNoEmpty()) {
                    val vkOwnIds = VKOwnIds()
                    vkOwnIds.append(accountId) // добавляем свой профайл на всякий случай
                    for (dialog in apiDialogs) {
                        vkOwnIds.append(dialog)
                    }
                    vkOwnIds.all
                } else {
                    emptyList()
                }
                val existsOwners = transformOwners(response.profiles, response.groups)
                val ownerEntities = mapOwners(response.profiles, response.groups)
                ownersRepository
                    .findBaseOwnersDataAsBundle(
                        accountId,
                        ownerIds,
                        IOwnersRepository.MODE_ANY,
                        existsOwners
                    )
                    .flatMap { owners ->
                        val entities: MutableList<DialogDboEntity> = ArrayList(apiDialogs.size)
                        val dialogs: MutableList<Dialog> = ArrayList(apiDialogs.size)
                        val encryptedMessages: MutableList<Message> =
                            ArrayList(0)
                        for (dto in apiDialogs) {
                            val entity = mapDialog(dto, response.contacts) ?: continue
                            entities.add(entity)
                            val dialog = transform(accountId, dto, owners, response.contacts)
                            if (dialog != null) {
                                dialogs.add(dialog)
                            }
                            if (entity.message?.isEncrypted == true) {
                                dialog?.message?.let { encryptedMessages.add(it) }
                            }
                        }
                        val insertCompletable = dialogsStore
                            .insertDialogs(accountId, entities, clear)
                            .andThen(ownersRepository.insertOwners(accountId, ownerEntities))
                            .doOnComplete {
                                dialogsStore.setUnreadDialogsCount(
                                    accountId,
                                    response.unreadCount
                                )
                            }
                        if (encryptedMessages.nonNullNoEmpty()) {
                            insertCompletable.andThen(
                                Single.just<List<Message>>(
                                    encryptedMessages
                                )
                                    .compose(decryptor.withMessagesDecryption(accountId))
                                    .map<List<Dialog>> { dialogs })
                        } else {
                            insertCompletable.andThen(Single.just<List<Dialog>>(dialogs))
                        }
                    }
            }
    }

    override fun findCachedMessages(
        accountId: Long,
        ids: List<Int>
    ): Single<List<Message>> {
        return storages.messages()
            .findMessagesByIds(accountId, ids, withAttachments = true, withForwardMessages = true)
            .compose(entities2Models(accountId))
            .compose(decryptor.withMessagesDecryption(accountId))
    }

    @SuppressLint("UseSparseArrays")
    override fun put(builder: SaveMessageBuilder): Single<Message> {
        val accountId = builder.getAccountId()
        val draftMessageId = builder.getDraftMessageId()
        val peerId = builder.getPeerId()
        return getTargetMessageStatus(builder)
            .flatMap { status ->
                val patch = MessageEditEntity(status, accountId)
                patch.setEncrypted(builder.isRequireEncryption())
                patch.setPayload(builder.getPayload())
                patch.setKeyboard(buildKeyboardEntity(builder.getKeyboard()))
                patch.setDate(now())
                patch.setRead(false)
                patch.setOut(true)
                patch.setDeleted(false)
                patch.setImportant(false)
                val voice = builder.getVoiceMessageFile()
                if (voice != null) {
                    val extras: MutableMap<Int, String> = HashMap(1)
                    extras[Message.Extra.VOICE_RECORD] =
                        voice.absolutePath
                    patch.setExtras(extras)
                }
                builder.getAttachments().nonNullNoEmpty {
                    patch.setAttachments(buildDboAttachments(it))
                }
                val fwds = builder.getForwardMessages()
                if (fwds.nonNullNoEmpty()) {
                    val fwddbos: MutableList<MessageDboEntity> = ArrayList(fwds.size)
                    for (message in fwds) {
                        val fwddbo = buildMessageEntity(message)
                        fwddbo.setOriginalId(message.getObjectId()) // сохранить original_id необходимо, так как при вставке в таблицу _ID потеряется

                        // fixes
                        if (fwddbo.isOut) {
                            fwddbo.setFromId(accountId)
                        }
                        fwddbos.add(fwddbo)
                    }
                    patch.setForward(fwddbos)
                } else {
                    patch.setForward(null)
                }
                getFinalMessagesBody(builder)
                    .flatMap { body ->
                        patch.setBody(body.get())
                        val storeSingle: Single<Int> = if (draftMessageId != null) {
                            storages.messages().applyPatch(accountId, draftMessageId, patch)
                        } else {
                            storages.messages().insert(accountId, peerId, patch)
                        }
                        storeSingle
                            .flatMap { resultMid ->
                                storages.messages()
                                    .findMessagesByIds(
                                        accountId, listOf(resultMid),
                                        withAttachments = true, withForwardMessages = true
                                    )
                                    .compose(entities2Models(accountId))
                                    .map { messages ->
                                        if (messages.isEmpty()) {
                                            throw NotFoundException()
                                        }
                                        val message = messages[0]
                                        if (builder.isRequireEncryption()) {
                                            message.decryptedBody = builder.getBody()
                                            message.cryptStatus = CryptStatus.DECRYPTED
                                        }
                                        message
                                    }
                            }
                    }
            }
    }

    private fun changeMessageStatus(
        accountId: Long,
        messageId: Int,
        @MessageStatus status: Int,
        vkid: Int?
    ): Completable {
        val update = MessageUpdate(accountId, messageId)
        update.setStatusUpdate(StatusUpdate(status, vkid))
        return storages.messages()
            .changeMessageStatus(accountId, messageId, status, vkid)
            .onErrorComplete()
            .doOnComplete { messageUpdatesPublisher.onNext(listOf(update)) }
    }

    override fun enqueueAgainList(accountId: Long, ids: Collection<Int>): Completable {
        val updates = ArrayList<MessageUpdate>(ids.size)
        for (i in ids) {
            val update = MessageUpdate(accountId, i)
            update.setStatusUpdate(StatusUpdate(MessageStatus.QUEUE, null))
            updates.add(update)
        }
        return storages.messages()
            .changeMessagesStatus(accountId, ids, MessageStatus.QUEUE)
            .onErrorComplete()
            .doOnComplete { messageUpdatesPublisher.onNext(updates) }
    }

    override fun enqueueAgain(accountId: Long, messageId: Int): Completable {
        return changeMessageStatus(accountId, messageId, MessageStatus.QUEUE, null)
    }

    override fun sendUnsentMessage(accountIds: Collection<Long>): Single<SentMsg> {
        val store = storages.messages()
        return store
            .findFirstUnsentMessage(accountIds, withAttachments = true, withForwardMessages = false)
            .flatMap { optional ->
                if (optional.isEmpty) {
                    return@flatMap Single.error<SentMsg>(NotFoundException())
                }
                val entity = optional.get()!!.second
                val accountId = optional.get()!!.first
                val dbid = entity.id
                val peerId = entity.peerId
                changeMessageStatus(accountId, dbid, MessageStatus.SENDING, null)
                    .andThen(internalSend(accountId, entity)
                        .flatMap { vkid ->
                            val patch = PeerPatch(entity.peerId)
                                .withLastMessage(vkid)
                                .withUnreadCount(0)
                            changeMessageStatus(accountId, dbid, MessageStatus.SENT, vkid)
                                .andThen(applyPeerUpdatesAndPublish(accountId, listOf(patch)))
                                .andThen(Single.just(SentMsg(dbid, vkid, peerId, accountId)))
                        }
                        .onErrorResumeNext {
                            changeMessageStatus(
                                accountId,
                                dbid,
                                MessageStatus.ERROR,
                                null
                            ).andThen(
                                Single.error(it)
                            )
                        })
            }
    }

    override fun searchConversations(
        accountId: Long,
        count: Int,
        q: String?
    ): Single<List<Conversation>> {
        return networker.vkDefault(accountId)
            .messages()
            .searchConversations(q, count, 1, Fields.FIELDS_BASE_OWNER)
            .flatMap { chattables ->
                val conversations: List<VKApiConversation> =
                    listEmptyIfNull(chattables.conversations)
                val ownerIds: Collection<Long> = if (conversations.nonNullNoEmpty()) {
                    val vkOwnIds = VKOwnIds()
                    vkOwnIds.append(accountId)
                    for (dialog in conversations) {
                        vkOwnIds.append(dialog)
                    }
                    vkOwnIds.all
                } else {
                    emptyList()
                }
                val existsOwners = transformOwners(chattables.profiles, chattables.groups)
                ownersRepository
                    .findBaseOwnersDataAsBundle(
                        accountId,
                        ownerIds,
                        IOwnersRepository.MODE_ANY,
                        existsOwners
                    )
                    .flatMap { bundle ->
                        val models: MutableList<Conversation> = ArrayList(conversations.size)
                        for (dialog in conversations) {
                            transform(
                                accountId,
                                dialog,
                                bundle,
                                chattables.contacts
                            )?.let { models.add(it) }
                        }
                        Single.just<List<Conversation>>(models)
                    }
            }
    }

    override fun searchMessages(
        accountId: Long,
        peerId: Long?,
        count: Int,
        offset: Int,
        q: String?
    ): Single<List<Message>> {
        return networker.vkDefault(accountId)
            .messages()
            .search(q, peerId, null, null, offset, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .flatMap { dtos ->
                val ids = VKOwnIds().append(dtos)
                ownersRepository
                    .findBaseOwnersDataAsBundle(accountId, ids.all, IOwnersRepository.MODE_ANY)
                    .map<List<Message>> { bundle: IOwnersBundle? ->
                        val data: MutableList<Message> =
                            ArrayList(dtos.size)
                        for (dto in dtos) {
                            val message = transform(accountId, dto, bundle!!)
                            data.add(message)
                        }
                        data
                    }
                    .compose(decryptor.withMessagesDecryption(accountId))
            }
    }

    override fun getChatUsers(accountId: Long, chatId: Long): Single<List<AppChatUser>> {
        return networker.vkDefault(accountId)
            .messages()
            .getConversationMembers(Peer.fromChatId(chatId), Fields.FIELDS_BASE_OWNER)
            .flatMap { chatDto ->
                val dtos: List<VKApiConversationMembers> =
                    listEmptyIfNull(chatDto.conversationMembers)
                val ownerIds: Collection<Long> = if (dtos.nonNullNoEmpty()) {
                    val vkOwnIds = VKOwnIds()
                    vkOwnIds.append(accountId)
                    for (dto in dtos) {
                        vkOwnIds.append(dto.member_id)
                        vkOwnIds.append(dto.invited_by)
                    }
                    vkOwnIds.all
                } else {
                    emptyList()
                }
                val existsOwners = transformOwners(chatDto.profiles, chatDto.groups)
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ownerIds,
                    IOwnersRepository.MODE_ANY,
                    existsOwners
                )
                    .map<List<AppChatUser>> { ownersBundle: IOwnersBundle ->
                        val models: MutableList<AppChatUser> = ArrayList(dtos.size)
                        for (dto in dtos) {
                            val user =
                                AppChatUser(ownersBundle.getById(dto.member_id), dto.invited_by)
                            user.setCanRemove(dto.can_kick)
                            user.setJoin_date(dto.join_date)
                            user.setAdmin(dto.is_admin)
                            user.setOwner(dto.is_owner)
                            if (user.getInvitedBy() != 0L) {
                                user.setInviter(ownersBundle.getById(user.getInvitedBy()))
                            }
                            models.add(user)
                        }
                        models
                    }
            }
    }

    override fun removeChatMember(accountId: Long, chatId: Long, userId: Long): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .removeChatMember(chatId, userId)
            .ignoreElement()
    }

    override fun deleteChatPhoto(accountId: Long, chatId: Long): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .deleteChatPhoto(chatId)
            .ignoreElement()
    }

    override fun addChatUsers(
        accountId: Long,
        chatId: Long,
        users: List<User>
    ): Single<List<AppChatUser>> {
        val api = networker.vkDefault(accountId).messages()
        return ownersRepository.getBaseOwnerInfo(accountId, accountId, IOwnersRepository.MODE_ANY)
            .flatMap { iam ->
                var completable = Completable.complete()
                val data: MutableList<AppChatUser> = ArrayList()
                for (user in users) {
                    completable =
                        completable.andThen(
                            api.addChatUser(chatId, user.getOwnerObjectId()).ignoreElement()
                        )
                    val chatUser = AppChatUser(user, accountId)
                        .setCanRemove(true)
                        .setInviter(iam)
                    data.add(chatUser)
                }
                completable.andThen(Single.just<List<AppChatUser>>(data))
            }
    }

    override fun deleteDialog(accountId: Long, peedId: Long): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .deleteDialog(peedId)
            .flatMapCompletable {
                storages.dialogs()
                    .removePeerWithId(accountId, peedId)
                    .andThen(
                        storages.messages().insertPeerDbos(accountId, peedId, emptyList(), true)
                    )
            }
            .doOnComplete { peerDeletingPublisher.onNext(PeerDeleting(accountId, peedId)) }
    }

    override fun deleteMessages(
        accountId: Long,
        peerId: Long,
        ids: Collection<Int>,
        forAll: Boolean,
        spam: Boolean
    ): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .delete(ids, forAll, spam)
            .flatMapCompletable { result: Map<String, Int> ->
                val patches: MutableList<MessagePatch> = ArrayList(result.size)
                for ((key, value) in result) {
                    val removed = value == 1
                    val removedId = key.toInt()
                    if (removed) {
                        val patch = MessagePatch(removedId, peerId)
                        patch.deletion = MessagePatch.Deletion(true, forAll)
                        patches.add(patch)
                    }
                }
                applyMessagesPatchesAndPublish(accountId, patches)
            }
    }

    override fun pinUnPinConversation(accountId: Long, peerId: Long, peen: Boolean): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .pinUnPinConversation(peerId, peen)
    }

    override fun markAsListened(
        accountId: Long,
        message_id: Int
    ): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .markAsListened(message_id)
    }

    override fun markAsImportant(
        accountId: Long,
        peerId: Long,
        ids: Collection<Int>,
        important: Int?
    ): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .markAsImportant(ids, important)
            .flatMapCompletable { result: List<Int> ->
                val patches: MutableList<MessagePatch> = ArrayList(result.size)
                for (entry in result) {
                    val marked = important == 1
                    val patch = MessagePatch(entry, peerId)
                    patch.important = Important(marked)
                    patches.add(patch)
                }
                applyMessagesPatchesAndPublish(accountId, patches)
            }
    }

    private fun applyMessagesPatchesAndPublish(
        accountId: Long,
        patches: List<MessagePatch>
    ): Completable {
        val updates: MutableList<MessageUpdate> = ArrayList(patches.size)
        val requireInvalidate: MutableSet<PeerId> = HashSet(0)
        for (patch in patches) {
            updates.add(patch2Update(accountId, patch))
            if (patch.deletion != null) {
                requireInvalidate.add(PeerId(accountId, patch.peerId))
            }
        }
        var afterApply = Completable.complete()
        val invalidatePeers: MutableList<Completable> = LinkedList()
        for (pair in requireInvalidate) {
            invalidatePeers.add(invalidatePeerLastMessage(pair.accountId, pair.peerId))
        }
        if (invalidatePeers.isNotEmpty()) {
            afterApply = Completable.merge(invalidatePeers)
        }
        return storages.messages()
            .applyPatches(accountId, patches)
            .andThen(afterApply)
            .doOnComplete { messageUpdatesPublisher.onNext(updates) }
    }

    private fun invalidatePeerLastMessage(accountId: Long, peerId: Long): Completable {
        return storages.messages()
            .findLastSentMessageIdForPeer(accountId, peerId)
            .flatMapCompletable {
                if (it.isEmpty) {
                    val deleting = PeerDeleting(accountId, peerId)
                    return@flatMapCompletable storages.dialogs().removePeerWithId(accountId, peerId)
                        .doOnComplete { peerDeletingPublisher.onNext(deleting) }
                } else {
                    val patch = PeerPatch(peerId).withLastMessage(it.requireNonEmpty())
                    return@flatMapCompletable applyPeerUpdatesAndPublish(accountId, listOf(patch))
                }
            }
    }

    override fun restoreMessage(accountId: Long, peerId: Long, messageId: Int): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .restore(messageId)
            .flatMapCompletable {
                val patch = MessagePatch(messageId, peerId)
                patch.deletion = MessagePatch.Deletion(deleted = false, deletedForAll = false)
                applyMessagesPatchesAndPublish(accountId, listOf(patch))
            }
    }

    override fun editChat(accountId: Long, chatId: Long, title: String?): Completable {
        val patch = PeerPatch(Peer.fromChatId(chatId)).withTitle(title)
        return networker.vkDefault(accountId)
            .messages()
            .editChat(chatId, title)
            .flatMapCompletable {
                applyPeerUpdatesAndPublish(
                    accountId,
                    listOf(patch)
                )
            }
    }

    override fun createGroupChat(
        accountId: Long,
        users: Collection<Long>,
        title: String?
    ): Single<Long> {
        return networker.vkDefault(accountId)
            .messages()
            .createChat(users, title)
    }

    override fun recogniseAudioMessage(
        accountId: Long,
        message_id: Int?,
        audio_message_id: String?
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .messages()
            .recogniseAudioMessage(message_id, audio_message_id)
    }

    override fun setMemberRole(
        accountId: Long,
        chat_id: Long,
        member_id: Long,
        isAdmin: Boolean
    ): Completable {
        return networker.vkDefault(accountId)
            .messages()
            .setMemberRole(Peer.fromChatId(chat_id), member_id, if (isAdmin) "admin" else "member")
            .ignoreElement()
    }

    override fun markAsRead(accountId: Long, peerId: Long, toId: Int): Completable {
        val patch = PeerPatch(peerId).withInRead(toId).withUnreadCount(0)
        return networker.vkDefault(accountId)
            .messages()
            .markAsRead(peerId, toId)
            .flatMapCompletable {
                applyPeerUpdatesAndPublish(
                    accountId,
                    listOf(patch)
                )
            }
    }

    override fun pin(
        accountId: Long,
        peerId: Long,
        message: Message?
    ): Completable {
        val update = PeerUpdate(accountId, peerId)
        update.pin = PeerUpdate.Pin(message)
        val apiCompletable: Completable = if (message == null) {
            networker.vkDefault(accountId)
                .messages()
                .unpin(peerId)
        } else {
            networker.vkDefault(accountId)
                .messages()
                .pin(peerId, message.getObjectId())
        }
        val patch = PeerPatch(peerId)
            .withPin(if (message == null) null else buildMessageEntity(message))
        return apiCompletable
            .andThen(storages.dialogs().applyPatches(accountId, listOf(patch)))
            .doOnComplete { peerUpdatePublisher.onNext(listOf(update)) }
    }

    private fun internalSend(accountId: Long, dbo: MessageDboEntity): Single<Int> {
        if (dbo.extras.isNullOrEmpty() && dbo.getAttachments()
                .isNullOrEmpty() && dbo.forwardCount == 0
        ) {
            return networker.vkDefault(accountId)
                .messages()
                .send(
                    dbo.id.toLong(),
                    dbo.peerId,
                    null,
                    dbo.body,
                    null,
                    null,
                    null,
                    null,
                    null,
                    dbo.payload,
                    null
                )
        }
        val attachments: MutableCollection<IAttachmentToken> = LinkedList()
        try {
            dbo.getAttachments().nonNullNoEmpty { att ->
                for (a in att) {
                    if (a is StickerDboEntity) {
                        val stickerId = a.id
                        return checkForwardMessages(accountId, dbo)
                            .flatMap {
                                return@flatMap networker.vkDefault(accountId)
                                    .messages()
                                    .send(
                                        dbo.id.toLong(),
                                        dbo.peerId,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        stickerId,
                                        dbo.payload,
                                        if (it.first) it.second.requireNonEmpty()[0] else null
                                    )
                            }
                    }
                    attachments.add(createToken(a))
                }
            }
        } catch (e: Exception) {
            return Single.error(e)
        }
        return checkVoiceMessage(accountId, dbo)
            .flatMap { optionalToken ->
                if (optionalToken.nonEmpty()) {
                    attachments.add(optionalToken.requireNonEmpty())
                }
                checkForwardMessages(accountId, dbo)
                    .flatMap {
                        networker.vkDefault(accountId)
                            .messages()
                            .send(
                                dbo.id.toLong(),
                                dbo.peerId,
                                null,
                                dbo.body,
                                null,
                                null,
                                attachments,
                                if (!it.first) it.second.get() else null,
                                null,
                                dbo.payload,
                                if (it.first) it.second.requireNonEmpty()[0] else null
                            )
                    }
            }
    }

    private fun checkForwardMessages(
        accountId: Long,
        dbo: MessageDboEntity
    ): Single<Pair<Boolean, Optional<List<Int>>>> {
        return if (dbo.forwardCount == 0) {
            Single.just(Pair(false, empty()))
        } else storages.messages()
            .getForwardMessageIds(accountId, dbo.id, dbo.peerId)
            .map {
                Pair(
                    it.first,
                    wrap(it.second)
                )
            }
    }

    private fun checkVoiceMessage(
        accountId: Long,
        dbo: MessageDboEntity
    ): Single<Optional<IAttachmentToken>> {
        val extras = dbo.extras
        if (extras != null && extras.containsKey(Message.Extra.VOICE_RECORD)) {
            val filePath = extras[Message.Extra.VOICE_RECORD]
            val docsApi = networker.vkDefault(accountId).docs()
            return docsApi.getMessagesUploadServer(dbo.peerId, "audio_message")
                .flatMap { server ->
                    val file = File(filePath!!)
                    val inputStream = arrayOfNulls<InputStream>(1)
                    try {
                        inputStream[0] = FileInputStream(file)
                        return@flatMap networker.uploads()
                            .uploadDocumentRx(
                                server.url ?: throw NotFoundException("upload url empty"),
                                file.name,
                                inputStream[0]!!,
                                null
                            )
                            .doFinally(safelyCloseAction(inputStream[0]))
                            .flatMap { uploadDto ->
                                docsApi
                                    .save(uploadDto.file, null, null)
                                    .map { dtos ->
                                        if (dtos.type.isEmpty()) {
                                            throw NotFoundException("Unable to save voice message")
                                        }
                                        val dto = dtos.doc
                                        val token = AttachmentsTokenCreator.ofDocument(
                                            dto.id,
                                            dto.ownerId,
                                            dto.accessKey
                                        )
                                        wrap(token)
                                    }
                            }
                    } catch (e: FileNotFoundException) {
                        safelyClose(inputStream[0])
                        return@flatMap Single.error<Optional<IAttachmentToken>>(e)
                    }
                }
        }
        return Single.just(empty())
    }

    private fun getFinalMessagesBody(builder: SaveMessageBuilder): Single<Optional<String>> {
        if (builder.getBody().isNullOrEmpty() || !builder.isRequireEncryption()) {
            return Single.just(
                wrap(
                    builder.getBody()
                )
            )
        }
        @KeyLocationPolicy val policy = builder.getKeyLocationPolicy()
        return storages.keys(policy)
            .findLastKeyPair(builder.getAccountId(), builder.getPeerId())
            .map {
                if (it.isEmpty) {
                    throw KeyPairDoesNotExistException()
                }
                val pair = it.requireNonEmpty()
                val encrypted = encryptWithAes(
                    builder.getBody().orEmpty(),
                    pair.myAesKey,
                    builder.getBody().orEmpty(),
                    pair.sessionId,
                    builder.getKeyLocationPolicy()
                )
                wrap(encrypted)
            }
    }

    private fun getTargetMessageStatus(builder: SaveMessageBuilder): Single<Int> {
        val accountId = builder.getAccountId()
        val destination =
            forMessage(builder.getDraftMessageId() ?: return Single.just(MessageStatus.QUEUE))
        return uploadManager[accountId, destination]
            .map { uploads ->
                if (uploads.isEmpty()) {
                    return@map MessageStatus.QUEUE
                }
                var uploadingNow = false
                for (o in uploads) {
                    if (o.status == Upload.STATUS_CANCELLING) {
                        continue
                    }
                    if (o.status == Upload.STATUS_ERROR) {
                        throw UploadNotResolvedException()
                    }
                    uploadingNow = true
                }
                if (uploadingNow) MessageStatus.WAITING_FOR_UPLOAD else MessageStatus.QUEUE
            }
    }

    private class InternalHandler(repository: MessagesRepository) :
        WeakMainLooperHandler<MessagesRepository>(repository) {
        fun runSend() {
            sendEmptyMessage(SEND)
        }

        override fun handleMessage(t: MessagesRepository, msg: android.os.Message) {
            if (msg.what == SEND) {
                t.send()
            }
        }

        companion object {
            const val SEND = 1
        }
    }

    private class PeerId(val accountId: Long, val peerId: Long) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val peerId1 = other as PeerId
            return if (accountId != peerId1.accountId) false else peerId == peerId1.peerId
        }

        override fun hashCode(): Int {
            var result = accountId.hashCode()
            result = 31 * result + peerId.hashCode()
            return result
        }
    }

    companion object {
        private val DTO_TO_DBO = SingleTransformer { single: Single<List<VKApiMessage>> ->
            single
                .map<List<MessageDboEntity>> { dtos: List<VKApiMessage> ->
                    val dbos: MutableList<MessageDboEntity> = ArrayList(dtos.size)
                    for (dto in dtos) {
                        dbos.add(mapMessage(dto))
                    }
                    dbos
                }
        }

        internal fun entity2Model(
            accountId: Long,
            entity: SimpleDialogEntity,
            owners: IOwnersBundle
        ): Conversation {
            return Conversation(entity.peerId)
                .setInRead(entity.inRead)
                .setOutRead(entity.outRead)
                .setPhoto50(entity.photo50)
                .setPhoto100(entity.photo100)
                .setPhoto200(entity.photo200)
                .setUnreadCount(entity.unreadCount)
                .setTitle(entity.title)
                .setInterlocutor(
                    if (Peer.isGroup(entity.peerId) || Peer.isUser(
                            entity.peerId
                        )
                    ) owners.getById(entity.peerId) else null
                )
                .setPinned(
                    entity.pinned?.let {
                        message(
                            accountId,
                            it,
                            owners
                        )
                    }
                )
                .setAcl(entity.acl)
                .setGroupChannel(entity.isGroupChannel)
                .setCurrentKeyboard(buildKeyboardFromDbo(entity.currentKeyboard))
                .setMajor_id(entity.major_id)
                .setMinor_id(entity.minor_id)
        }

        internal fun patch2Update(accountId: Long, patch: MessagePatch): MessageUpdate {
            val update = MessageUpdate(accountId, patch.messageId)
            patch.deletion.requireNonNull {
                update.setDeleteUpdate(MessageUpdate.DeleteUpdate(it.deleted, it.deletedForAll))
            }
            patch.important.requireNonNull {
                update.setImportantUpdate(ImportantUpdate(it.important))
            }
            return update
        }
    }

    init {
        decryptor = MessagesDecryptor(storages)
        this.uploadManager = uploadManager
        compositeDisposable.add(
            uploadManager.observeResults()
                .filter { it.first.destination.method == Method.TO_MESSAGE }
                .subscribe({ result ->
                    onUpdloadSuccess(
                        result.first
                    )
                }, ignore())
        )
        compositeDisposable.add(
            accountsSettings.observeRegistered()
                .observeOn(provideMainThreadScheduler())
                .subscribe({ onAccountsChanged() }, ignore())
        )
    }
}
