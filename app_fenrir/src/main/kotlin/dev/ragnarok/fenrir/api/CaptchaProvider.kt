package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import dev.ragnarok.fenrir.activity.CaptchaActivity.Companion.createIntent
import dev.ragnarok.fenrir.api.model.Captcha
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.inMainThread
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.myEmit
import kotlinx.coroutines.flow.SharedFlow
import java.util.Collections

class CaptchaProvider(private val app: Context) :
    ICaptchaProvider {
    private val entryMap: MutableMap<String, Entry> = Collections.synchronizedMap(HashMap())
    private val cancelingNotifier = createPublishSubject<String>()
    private val waitingNotifier = createPublishSubject<String>()
    override fun requestCaptcha(sid: String?, captcha: Captcha) {
        sid ?: return
        entryMap[sid] = Entry()
        startCaptchaActivity(app, sid, captcha)
    }

    @SuppressLint("CheckResult")
    private fun startCaptchaActivity(context: Context, sid: String, captcha: Captcha) {
        inMainThread {
            val intent = createIntent(context, sid, captcha.img)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun cancel(sid: String) {
        entryMap.remove(sid)
        cancelingNotifier.myEmit(sid)
    }

    override fun observeCanceling(): SharedFlow<String> {
        return cancelingNotifier
    }

    @Throws(OutOfDateException::class)
    override fun lookupCode(sid: String): String? {
        val iterator: MutableIterator<Map.Entry<String, Entry>> = entryMap.entries.iterator()
        while (iterator.hasNext()) {
            val (lookupsid, lookupEntry) = iterator.next()
            if (System.currentTimeMillis() - lookupEntry.lastActivityTime > MAX_WAIT_DELAY) {
                iterator.remove()
            } else {
                waitingNotifier.myEmit(lookupsid)
            }
        }
        val entry = entryMap[sid] ?: throw OutOfDateException()
        return entry.code
    }

    override fun observeWaiting(): SharedFlow<String> {
        return waitingNotifier
    }

    override fun notifyThatCaptchaEntryActive(sid: String) {
        val entry = entryMap[sid]
        if (entry != null) {
            entry.lastActivityTime = System.currentTimeMillis()
        }
    }

    override fun enterCode(sid: String, code: String?) {
        val entry = entryMap[sid]
        if (entry != null) {
            entry.code = code
        }
    }

    private class Entry {
        var code: String? = null
        var lastActivityTime: Long = System.currentTimeMillis()

    }

    companion object {
        private const val MAX_WAIT_DELAY = 15 * 60 * 1000
    }

}