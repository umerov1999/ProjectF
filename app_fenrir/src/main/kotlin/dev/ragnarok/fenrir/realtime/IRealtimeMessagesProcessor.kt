package dev.ragnarok.fenrir.realtime

import dev.ragnarok.fenrir.api.model.longpoll.AddMessageUpdate
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Observable

interface IRealtimeMessagesProcessor {
    fun observeResults(): Observable<TmpResult>
    fun process(accountId: Long, updates: List<AddMessageUpdate>): Int

    @Throws(QueueContainsException::class)
    fun process(
        accountId: Long,
        messageId: Int,
        peerId: Long,
        conversationMessageId: Int,
        ignoreIfExists: Boolean
    ): Int

    fun registerNotificationsInterceptor(interceptorId: Long, aidPeerPair: Pair<Long, Long>)
    fun unregisterNotificationsInterceptor(interceptorId: Long)
    fun isNotificationIntercepted(accountId: Long, peerId: Long): Boolean
}