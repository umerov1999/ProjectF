package dev.ragnarok.fenrir.realtime

import android.content.Context
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.longpoll.AddMessageUpdate
import dev.ragnarok.fenrir.crypt.KeyExchangeService
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IMessagesRepository
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.longpoll.LongPollNotificationHelper
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.push.NotificationScheduler
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import dev.ragnarok.fenrir.util.Utils.collectIds
import dev.ragnarok.fenrir.util.Utils.removeIf
import dev.ragnarok.fenrir.util.VKOwnIds
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromScopeToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicInteger

internal class RealtimeMessagesProcessor : IRealtimeMessagesProcessor {
    private val publishSubject = createPublishSubject<TmpResult>()
    private val repositories: IStorages = stores
    private val networker: INetworker = networkInterfaces
    private val stateLock = Any()
    private val queue: MutableList<Entry> = LinkedList()
    private val app: Context = provideApplicationContext()
    private val notificationsInterceptors: HashMap<Long, Pair<Long, Long>> = HashMap(3)
    private val ownersRepository: IOwnersRepository = owners
    private val messagesInteractor: IMessagesRepository = messages

    @Volatile
    private var current: Entry? = null
    override fun observeResults(): SharedFlow<TmpResult> {
        return publishSubject
    }

    override fun process(accountId: Long, updates: List<AddMessageUpdate>): Int {
        val id = ID_GENERATOR.incrementAndGet()
        val entry = Entry(accountId, id, false)
        for (update in updates) {
            entry.append(update)
        }
        addToQueue(entry)
        startIfNotStarted()
        return id
    }

    private fun hasInQueueOrCurrent(id: Int): Boolean {
        synchronized(stateLock) {
            val c = current
            if (c != null && c.has(id)) {
                return true
            }
            for (q in queue) {
                if (q.has(id)) {
                    return true
                }
            }
        }
        return false
    }

    @Throws(QueueContainsException::class)
    override fun process(
        accountId: Long,
        messageId: Int,
        peerId: Long,
        conversationMessageId: Int,
        ignoreIfExists: Boolean
    ): Int {
        if (hasInQueueOrCurrent(messageId)) {
            throw QueueContainsException()
        }
        d(TAG, "Register entry, aid: $accountId, mid: $messageId, ignoreIfExists: $ignoreIfExists")
        val id = ID_GENERATOR.incrementAndGet()
        val entry = Entry(accountId, id, ignoreIfExists)
        entry.append(messageId)
        addToQueue(entry)
        startIfNotStarted()
        return id
    }

    override fun registerNotificationsInterceptor(
        interceptorId: Long,
        aidPeerPair: Pair<Long, Long>
    ) {
        notificationsInterceptors[interceptorId] = aidPeerPair
    }

    override fun unregisterNotificationsInterceptor(interceptorId: Long) {
        notificationsInterceptors.remove(interceptorId)
    }

    override fun isNotificationIntercepted(accountId: Long, peerId: Long): Boolean {
        for (i in notificationsInterceptors) {
            if (i.value.first == accountId && i.value.second == peerId) {
                return false
            }
        }
        return true
    }

    private fun addToQueue(entry: Entry) {
        synchronized(stateLock) { queue.add(entry) }
    }

    private fun prepareForStartFirst(): Boolean {
        synchronized(stateLock) {
            if (current != null || queue.isEmpty()) {
                return false
            }
            current = queue.removeAt(0)
            return true
        }
    }

    /*private Completable refreshChangedDialogs(TmpResult result) {
        Set<Integer> peers = new HashSet<>();

        for (TmpResult.Msg msg : result.getData()) {
            VKApiMessage dto = msg.getDto();
            if (dto != null) {
                peers.add(dto.peer_id);
            }
        }

        Completable completable = Completable.complete();
        for (int peerId : peers) {
            completable = completable.andThen(messagesInteractor.fixDialogs(result.getAccountId(), peerId));
        }

        return completable;
    }*/
    private fun resetCurrent() {
        synchronized(stateLock) { current = null }
    }

    private fun startIfNotStarted() {
        if (!prepareForStartFirst()) {
            return
        }
        var entry: Entry?
        synchronized(stateLock) { entry = current }
        val start = System.currentTimeMillis()
        val ignoreIfExists = entry?.isIgnoreIfExists
        entry?.let { toFlow(it) }?.let { its ->
            its.map {
                val result = TmpResult(
                    it.id, it.accountId, it.count()
                )
                val updates = it.updates
                if (updates.hasFullMessages()) {
                    for (update in updates.fullMessages.orEmpty()) {
                        result.add(update.messageId)
                            .setDto(Dto2Model.transform(it.accountId, update))
                    }
                }
                if (updates.hasNonFullMessages()) {
                    for (update in updates.nonFull.orEmpty()) {
                        result.add(update.messageId)
                            .setBackup(Dto2Model.transform(it.accountId, update))
                    }
                }
                result
            } // ищем недостающие сообщения в локальной базе
                .flatMapConcat { result ->
                    repositories
                        .messages()
                        .getMissingMessages(result.accountId, result.allIds)
                        .map {
                            result.setMissingIds(
                                it
                            )
                        }
                }
                .flatMapConcat { result ->
                    // отсеиваем сообщения, которые уже есть в локальной базе (если требуется)
                    if (ignoreIfExists == true) {
                        removeIf(result.data) { it.isAlreadyExists }
                    }
                    if (result.data.isEmpty()) {
                        toFlow(result)
                    } else {
                        toFlow(result)
                            .flatMapConcat(andStore())
                    }
                }
                .fromScopeToMain(NotificationScheduler.INSTANCE, { result ->
                    onResultReceived(
                        start,
                        result
                    )
                }, { throwable -> onProcessError(throwable) })
        }
    }// сохраняем сообщения в локальную базу и получаем оттуда "тяжелые" обьекты сообщений// отсеиваем сообщения, которые имеют отношение к обмену ключами

    private fun andStore(): suspend (TmpResult) -> Flow<TmpResult> = { result ->
        // если в исходных данных недостаточно инфы - получаем нужные данные с api
        val needGetFromNet =
            collectIds(result.data) {
                it.dto == null
            }
        if (needGetFromNet.isEmpty()) {
            toFlow(result)
        } else {
            networker.vkDefault(result.accountId)
                .messages()
                .getById(needGetFromNet)
                .map { result.appendDtos(it) }
        }
            .map { s ->
                // отсеиваем сообщения, которые имеют отношение к обмену ключами
                removeIf(
                    s.data
                ) {
                    KeyExchangeService.intercept(
                        app,
                        s.accountId,
                        it.dto
                    )
                }
                s
            }
            .flatMapConcat { s ->
                if (s.data.isEmpty()) {
                    toFlow(s)
                } else {
                    identifyMissingObjectsGetAndStore(s)
                        .andThen(toFlow(s)) // сохраняем сообщения в локальную базу и получаем оттуда "тяжелые" обьекты сообщений
                        .flatMapConcat(storeToCacheAndReturn())
                }
            }
    }

    private fun storeToCacheAndReturn(): suspend (TmpResult) -> Flow<TmpResult> = { result ->
        messagesInteractor.insertMessages(result.accountId, result.collectDtos())
            .flatMapConcat {
                // собственно, получение из локальной базы
                val ids = collectIds(result.data) {
                    true
                }
                messagesInteractor
                    .findCachedMessages(result.accountId, ids)
                    .map { result.appendModel(it) }
            }
    }

    private fun onResultReceived(startTime: Long, result: TmpResult) {
        val lastEnryProcessTime = System.currentTimeMillis() - startTime
        d(TAG, "SUCCESS, data: $result, time: $lastEnryProcessTime")
        sendNotifications(result)
        publishSubject.myEmit(result)
        resetCurrent()
        startIfNotStarted()
    }

    private fun sendNotifications(result: TmpResult) {
        for (msg in result.data) {
            if (msg.isAlreadyExists) {
                continue
            }
            val message = msg.message
            if (message == null || !isNotificationIntercepted(result.accountId, message.peerId)) {
                continue
            }
            LongPollNotificationHelper.notifyAbountNewMessage(app, message)
        }
    }

    private fun onProcessError(throwable: Throwable) {
        throwable.printStackTrace()
        logThrowable(RealtimeMessagesProcessor::class.simpleName.orEmpty(), throwable)
        resetCurrent()
        startIfNotStarted()
    }

    private fun identifyMissingObjectsGetAndStore(result: TmpResult): Flow<Boolean> {
        val ownIds = getOwnIds(result)
        val chatIds: Collection<Long>? = getChatIds(result)
        val accountId = result.accountId
        var completable = emptyTaskFlow()
        if (ownIds.nonEmpty()) {
            // проверяем на недостающих пользователей и групп
            completable = completable.andThen(findMissingOwnersGetAndStore(accountId, ownIds))
        }
        if (chatIds.nonNullNoEmpty()) {
            // проверяем на отсутствие чатов
            completable = completable.andThen(findMissingChatsGetAndStore(accountId, chatIds))
        }
        return completable
    }

    private fun findMissingChatsGetAndStore(accountId: Long, ids: Collection<Long>): Flow<Boolean> {
        return repositories.dialogs()
            .getMissingGroupChats(accountId, ids)
            .flatMapConcat { integers ->
                if (integers.isEmpty()) {
                    emptyTaskFlow()
                } else {
                    networker.vkDefault(accountId)
                        .messages()
                        .getChat(null, integers, null, null)
                        .flatMapConcat {
                            repositories.dialogs()
                                .insertChats(accountId, it)
                        }
                }
            }
    }

    private fun findMissingOwnersGetAndStore(accountId: Long, ids: VKOwnIds): Flow<Boolean> {
        return findMissingOwnerIds(accountId, ids)
            .flatMapConcat { integers ->
                if (integers.isEmpty()) {
                    emptyTaskFlow()
                } else {
                    ownersRepository.cacheActualOwnersData(accountId, integers)
                }
            }
    }

    private fun findMissingOwnerIds(accountId: Long, ids: VKOwnIds): Flow<List<Long>> {
        return repositories.owners()
            .getMissingUserIds(accountId, ids.getUids())
            .zip(
                repositories.owners().getMissingCommunityIds(accountId, ids.getGids())
            ) { integers, integers2 ->
                if (integers.isEmpty() && integers2.isEmpty()) {
                    emptyList()
                } else {
                    val result: MutableList<Long> = ArrayList(integers.size + integers2.size)
                    result.addAll(integers)
                    result.addAll(integers2)
                    result
                }
            }
    }

    companion object {
        private const val TAG = "RealtimeMessagesProcessor"
        private val ID_GENERATOR = AtomicInteger()
        internal fun getChatIds(result: TmpResult): Set<Long>? {
            var peersIds: MutableSet<Long>? = null
            for (msg in result.data) {
                val dto = msg.dto ?: continue
                if (Peer.isGroupChat(dto.peer_id)) {
                    if (peersIds == null) {
                        peersIds = HashSet(1)
                    }
                    peersIds.add(dto.peer_id)
                }
            }
            return peersIds
        }

        internal fun getOwnIds(result: TmpResult): VKOwnIds {
            val vkOwnIds = VKOwnIds()
            for (msg in result.data) {
                msg.dto.requireNonNull {
                    vkOwnIds.append(it)
                }
            }
            return vkOwnIds
        }
    }
}
