package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.longpoll.VkApiGroupLongpollUpdates
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

internal class GroupLongpoll(
    private val networker: INetworker,
    private val groupId: Int,
    private val callback: Callback
) : ILongpoll {
    private val compositeDisposable = CompositeDisposable()
    private val delayedObservable = Observable.interval(
        DELAY_ON_ERROR.toLong(), DELAY_ON_ERROR.toLong(),
        TimeUnit.MILLISECONDS, provideMainThreadScheduler()
    )
    private var key: String? = null
    private var server: String? = null
    private var ts: String? = null
    override val accountId: Int
        get() = -groupId

    private fun resetServerAttrs() {
        server = null
        key = null
        ts = null
    }

    override fun shutdown() {
        compositeDisposable.dispose()
    }

    override fun connect() {
        if (!isListeningNow) {
            get()
        }
    }

    private val isListeningNow: Boolean
        get() = compositeDisposable.size() > 0

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
        compositeDisposable.clear()
        val validServer = server.nonNullNoEmpty() && key.nonNullNoEmpty() && ts.nonNullNoEmpty()
        if (validServer) {
            compositeDisposable.add(
                networker.longpoll()
                    .getGroupUpdates(server, key, ts, 25)
                    .fromIOToMain()
                    .subscribe({ updates -> onUpdates(updates) }) { throwable ->
                        onUpdatesGetError(
                            throwable
                        )
                    })
        } else {
            compositeDisposable.add(
                networker.vkDefault(accountId)
                    .groups()
                    .getLongPollServer(groupId)
                    .fromIOToMain()
                    .subscribe({ info -> onServerInfoReceived(info) }) { throwable ->
                        onServerGetError(
                            throwable
                        )
                    })
        }
    }

    private fun onUpdates(updates: VkApiGroupLongpollUpdates) {
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
            compositeDisposable.add(delayedObservable.subscribe { get() })
        }

    interface Callback {
        fun onUpdates(groupId: Int, updates: VkApiGroupLongpollUpdates)
    }

    companion object {
        private const val DELAY_ON_ERROR = 10 * 1000
    }
}