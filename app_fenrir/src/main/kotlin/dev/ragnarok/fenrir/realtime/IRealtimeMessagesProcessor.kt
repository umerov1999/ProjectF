package dev.ragnarok.fenrir.realtime

import dev.ragnarok.fenrir.api.model.longpoll.AddMessageUpdate
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Observable

interface IRealtimeMessagesProcessor {
    fun observeResults(): Observable<TmpResult>
    fun process(accountId: Int, updates: List<AddMessageUpdate>): Int

    @Throws(QueueContainsException::class)
    fun process(accountId: Int, messageId: Int, ignoreIfExists: Boolean): Int
    fun registerNotificationsInterceptor(interceptorId: Int, aidPeerPair: Pair<Int, Int>)
    fun unregisterNotificationsInterceptor(interceptorId: Int)
    fun isNotificationIntercepted(accountId: Int, peerId: Int): Boolean
}