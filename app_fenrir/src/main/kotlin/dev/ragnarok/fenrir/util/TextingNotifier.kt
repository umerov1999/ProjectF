package dev.ragnarok.fenrir.util

import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.fromIOToMain
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class TextingNotifier(private val accountId: Int) {
    private var lastNotifyTime: Long = 0
    private var isRequestNow = false
    private var disposable = Disposable.disposed()
    fun notifyAboutTyping(peerId: Int) {
        if (!canNotifyNow()) {
            return
        }
        lastNotifyTime = System.currentTimeMillis()
        isRequestNow = true
        disposable = createNotifier(accountId, peerId)
            .fromIOToMain()
            .subscribe({ isRequestNow = false }) { isRequestNow = false }
    }

    fun shutdown() {
        disposable.dispose()
    }

    private fun canNotifyNow(): Boolean {
        return !isRequestNow && abs(System.currentTimeMillis() - lastNotifyTime) > 5000
    }

    companion object {
        internal fun createNotifier(accountId: Int, peerId: Int): Completable {
            return get()
                .vkDefault(accountId)
                .messages()
                .setActivity(peerId, true)
                .delay(5, TimeUnit.SECONDS)
                .ignoreElement()
        }
    }
}