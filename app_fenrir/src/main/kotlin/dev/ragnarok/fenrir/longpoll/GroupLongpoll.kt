package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.longpoll.VKApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class GroupLongpoll(
    private val networker: INetworker,
    private val groupId: Long,
    private val callback: Callback
) : ILongpoll {
    private val compositeJob = CompositeJob()
    private var key: String? = null
    private var server: String? = null
    private var ts: String? = null
    override val accountId: Long
        get() = -groupId

    private val delayedObservable: Flow<Boolean> = flow {
        while (isActive()) {
            delay(DELAY_ON_ERROR.toLong())
            emit(true)
        }
    }

    private fun resetServerAttrs() {
        server = null
        key = null
        ts = null
    }

    override fun shutdown() {
        compositeJob.cancel()
    }

    override fun connect() {
        if (!isListeningNow) {
            get()
        }
    }

    private val isListeningNow: Boolean
        get() = compositeJob.size() > 0

    private fun onServerInfoReceived(info: GroupLongpollServer) {
        ts = info.ts
        key = info.key
        server = info.server
        get()
    }

    private fun onServerGetError(throwable: Throwable) {
        logThrowable("Longpoll, ServerGet", throwable)
        withDelay
    }

    private fun get() {
        compositeJob.clear()
        val validServer = server.nonNullNoEmpty() && key.nonNullNoEmpty() && ts.nonNullNoEmpty()
        if (validServer) {
            compositeJob.add(
                networker.longpoll()
                    .getGroupUpdates(server ?: return, key, ts, Constants.LONGPOLL_WAIT)
                    .fromIOToMain({ updates -> onUpdates(updates) }) { throwable ->
                        onUpdatesGetError(
                            throwable
                        )
                    })
        } else {
            compositeJob.add(
                networker.vkDefault(accountId)
                    .groups()
                    .getLongPollServer(groupId)
                    .fromIOToMain({ info -> onServerInfoReceived(info) }) { throwable ->
                        onServerGetError(
                            throwable
                        )
                    })
        }
    }

    private fun onUpdates(updates: VKApiGroupLongpollUpdates) {
        if (updates.failed > 0) {
            resetServerAttrs()
            withDelay
        } else {
            ts = updates.ts
            if (updates.count > 0) {
                callback.onUpdates(groupId, updates)
            }
            get()
        }
    }

    private fun onUpdatesGetError(throwable: Throwable) {
        logThrowable("Longpoll, UpdatesGet", throwable)
        withDelay
    }

    private val withDelay: Unit
        get() {
            compositeJob.add(delayedObservable.toMain { get() })
        }

    interface Callback {
        fun onUpdates(groupId: Long, updates: VKApiGroupLongpollUpdates)
    }

    companion object {
        private const val DELAY_ON_ERROR = 10 * 1000
    }
}