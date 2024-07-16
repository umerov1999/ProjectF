package dev.ragnarok.fenrir.util

import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayedFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

class TextingNotifier(private val accountId: Long) {
    private var lastNotifyTime: Long = 0
    private var isRequestNow = false
    private var disposable = CancelableJob()
    fun notifyAboutTyping(peerId: Long) {
        if (!canNotifyNow()) {
            return
        }
        lastNotifyTime = System.currentTimeMillis()
        isRequestNow = true
        disposable += createNotifier(accountId, peerId)
            .fromIOToMain({ isRequestNow = false }) { isRequestNow = false }
    }

    fun shutdown() {
        disposable.cancel()
    }

    private fun canNotifyNow(): Boolean {
        return !isRequestNow && abs(System.currentTimeMillis() - lastNotifyTime) > 5000
    }

    companion object {
        internal fun createNotifier(accountId: Long, peerId: Long): Flow<Boolean> {
            return get()
                .vkDefault(accountId)
                .messages()
                .setActivity(peerId, true)
                .delayedFlow(5000)
                .ignoreElement()
        }
    }
}