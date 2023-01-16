package dev.ragnarok.fenrir.longpoll

import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiLongpollServer
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.notDisposed
import dev.ragnarok.fenrir.util.Logger.d
import dev.ragnarok.fenrir.util.PersistentLogger.logThrowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

internal class UserLongpoll(
    private val networker: INetworker,
    override val accountId: Long,
    private val callback: Callback
) : ILongpoll {
    private val mDelayedObservable = Observable.interval(
        DELAY_ON_ERROR.toLong(), DELAY_ON_ERROR.toLong(),
        TimeUnit.MILLISECONDS, provideMainThreadScheduler()
    )
    private var key: String? = null
    private var server: String? = null
    private var ts: Long? = null
    private var mCurrentUpdatesDisposable: Disposable? = null

    private fun resetServerAttrs() {
        server = null
        key = null
        ts = null
    }

    override fun shutdown() {
        d(TAG, "shutdown, aid: $accountId")
        resetUpdatesDisposable()
    }

    override fun connect() {
        d(TAG, "connect, aid: $accountId")
        if (!isListeningNow) {
            get()
        }
    }

    private val isListeningNow: Boolean
        get() = mCurrentUpdatesDisposable?.notDisposed() == true

    private fun resetUpdatesDisposable() {
        if (mCurrentUpdatesDisposable != null) {
            if (!(mCurrentUpdatesDisposable ?: return).isDisposed) {
                (mCurrentUpdatesDisposable ?: return).dispose()
            }
            mCurrentUpdatesDisposable = null
        }
    }

    private fun onServerInfoReceived(info: VKApiLongpollServer) {
        d(TAG, "onResponse, info: $info")
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
        resetUpdatesDisposable()
        val serverIsValid = server.nonNullNoEmpty() && key.nonNullNoEmpty() && ts != null
        if (!serverIsValid) {
            setDisposable(
                networker.vkDefault(accountId)
                    .messages()
                    .getLongpollServer(true, V)
                    .fromIOToMain()
                    .subscribe({ info -> onServerInfoReceived(info) }) { throwable ->
                        onServerGetError(
                            throwable
                        )
                    })
            return
        }
        setDisposable(
            networker.longpoll()
                .getUpdates("https://$server", key, ts ?: return, 25, MODE, V)
                .fromIOToMain()
                .subscribe({ updates -> onUpdates(updates) }) { throwable ->
                    onUpdatesGetError(
                        throwable
                    )
                })
    }

    private fun setDisposable(disposable: Disposable) {
        mCurrentUpdatesDisposable = disposable
    }

    private fun onUpdates(updates: VkApiLongpollUpdates) {
        d(TAG, "onUpdates, updates: $updates")
        if (updates.failed > 0) {
            resetServerAttrs()
            withDelay
        } else {
            ts = updates.ts
            if (updates.updatesCount > 0) {
                fixUpdates(updates)
                callback.onUpdates(accountId, updates)
            }
            get()
        }
    }

    private fun fixUpdates(updates: VkApiLongpollUpdates) {
        updates.add_message_updates.nonNullNoEmpty {
            for (update in it) {
                if (update.peerId == accountId) {
                    update.isOut = true
                }
                if (update.isOut) {
                    update.from = accountId
                }
            }
        }
    }

    private fun onUpdatesGetError(throwable: Throwable) {
        logThrowable("Longpoll, UpdatesGet", throwable)
        withDelay
    }

    private val withDelay: Unit
        get() {
            setDisposable(mDelayedObservable.subscribe { get() })
        }

    interface Callback {
        fun onUpdates(aid: Long, updates: VkApiLongpollUpdates)
    }

    companion object {
        private const val TAG = "Longpoll_TAG"
        private const val DELAY_ON_ERROR = 10 * 1000
        private const val V = 10
        private const val MODE = 2 +  //получать вложения;
                8 +  // возвращать расширенный набор событий;
                //32 + //возвращать pts (это требуется для работы метода messages.getLongPollHistory без ограничения в 256 последних событий);
                64 +  //в событии с кодом 8 (друг стал онлайн) возвращать дополнительные данные в поле $extra (подробнее в разделе Структура событий);
                128 //возвращать с сообщением параметр random_id (random_id может быть передан при отправке сообщения методом messages.send).
    }
}
