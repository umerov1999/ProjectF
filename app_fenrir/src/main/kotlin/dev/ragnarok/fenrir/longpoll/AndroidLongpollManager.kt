package dev.ragnarok.fenrir.longpoll

import android.os.Handler
import android.os.Looper
import android.os.Message
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.longpoll.VKApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.realtime.IRealtimeMessagesProcessor
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromScopeToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.SharedFlow
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class AndroidLongpollManager internal constructor(
    private val networker: INetworker,
    private val messagesProcessor: IRealtimeMessagesProcessor
) : ILongpollManager, UserLongpoll.Callback, GroupLongpoll.Callback {
    private val map: HashMap<Long, LongpollEntry> = HashMap(1)
    private val keepAlivePublisher = createPublishSubject<Long>()
    private val actionsPublisher = createPublishSubject<VKApiLongpollUpdates>()
    private val lock = Any()
    private val compositeJob = CompositeJob()
    override fun observe(): SharedFlow<VKApiLongpollUpdates> {
        return actionsPublisher
    }

    override fun observeKeepAlive(): SharedFlow<Long> {
        return keepAlivePublisher
    }

    private fun createLongpoll(accountId: Long): ILongpoll {
        //return accountId > 0 ? new UserLongpoll(networker, accountId, this) : new GroupLongpoll(networker, Math.abs(accountId), this);
        return UserLongpoll(networker, accountId, this)
    }

    override fun forceDestroy(accountId: Long) {
        d(TAG, "forceDestroy, accountId: $accountId")
        synchronized(lock) {
            val entry = map[accountId]
            entry?.destroy()
        }
    }

    override fun keepAlive(accountId: Long) {
        d(TAG, "keepAlive, accountId: $accountId")
        synchronized(lock) {
            var entry = map[accountId]
            if (entry != null) {
                entry.deferDestroy()
            } else {
                entry = LongpollEntry(createLongpoll(accountId), this)
                map[accountId] = entry
                entry.connect()
            }
        }
    }

    internal fun notifyDestroy(entry: LongpollEntry) {
        d(TAG, "destroyed, accountId: " + entry.accountId)
        synchronized(lock) { map.remove(entry.accountId) }
    }

    internal fun notifyPreDestroy(entry: LongpollEntry) {
        d(TAG, "pre-destroy, accountId: " + entry.accountId)
        keepAlivePublisher.myEmit(entry.accountId)
    }

    override fun onUpdates(aid: Long, updates: VKApiLongpollUpdates) {
        d(TAG, "updates, accountId: $aid")
        updates.add_message_updates.nonNullNoEmpty {
            val deletes = ArrayList<Int>()
            updates.message_reaction_changed_updates.nonNullNoEmpty { st ->
                for (i in st.indices) {
                    for (s in it) {
                        if (s.conversationMessageId == st[i].conversation_message_id && s.peerId == st[i].peer_id) {
                            deletes.add(i)
                        }
                    }
                }
            }
            for (i in deletes.reversed()) {
                it.removeAt(i)
            }

            messagesProcessor.process(aid, it)
        }
        if (!updates.isOnlyAddMessages()) {
            compositeJob.add(
                LongPollEventSaver()
                    .save(aid, updates)
                    .fromScopeToMain(MONO_SCHEDULER) {
                        onUpdatesSaved(updates)
                    }
            )
        }
    }

    private fun onUpdatesSaved(updates: VKApiLongpollUpdates) {
        actionsPublisher.myEmit(updates)
    }

    override fun onUpdates(groupId: Long, updates: VKApiGroupLongpollUpdates) {}
    class LongpollEntry(
        val longpoll: ILongpoll,
        manager: AndroidLongpollManager
    ) {
        val handler: SocketHandler = SocketHandler(this)
        val managerReference: WeakReference<AndroidLongpollManager> = WeakReference(manager)
        val accountId: Long = longpoll.accountId
        var released = false
        fun connect() {
            longpoll.connect()
            handler.restartPreDestroy()
        }

        fun destroy() {
            handler.release()
            longpoll.shutdown()
            released = true
            val manager = managerReference.get()
            manager?.notifyDestroy(this)
        }

        fun deferDestroy() {
            handler.restartPreDestroy()
        }

        fun firePreDestroy() {
            val manager = managerReference.get()
            manager?.notifyPreDestroy(this)
        }

    }

    class SocketHandler(holder: LongpollEntry) :
        Handler(Looper.getMainLooper()) {
        val reference: WeakReference<LongpollEntry> = WeakReference(holder)
        fun restartPreDestroy() {
            removeMessages(PRE_DESTROY)
            removeMessages(DESTROY)
            sendEmptyMessageDelayed(PRE_DESTROY, 30000L)
        }

        fun postDestroy() {
            sendEmptyMessageDelayed(DESTROY, 30000L)
        }

        fun release() {
            removeMessages(PRE_DESTROY)
            removeMessages(DESTROY)
        }

        override fun handleMessage(msg: Message) {
            val holder = reference.get()
            if (holder != null && !holder.released) {
                when (msg.what) {
                    PRE_DESTROY -> {
                        postDestroy()
                        holder.firePreDestroy()
                    }

                    DESTROY -> holder.destroy()
                }
            }
        }

        companion object {
            const val PRE_DESTROY = 2
            const val DESTROY = 3
        }

    }

    companion object {
        private val TAG = AndroidLongpollManager::class.simpleName.orEmpty()
        private val MONO_SCHEDULER =
            CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    }

}
