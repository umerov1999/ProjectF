package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import dev.ragnarok.fenrir.activity.CaptchaActivity.Companion.createIntent
import dev.ragnarok.fenrir.api.model.Captcha
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.Collections

class CaptchaProvider(private val app: Context, private val uiScheduler: Scheduler) :
    ICaptchaProvider {
    private val entryMap: MutableMap<String, Entry> = Collections.synchronizedMap(HashMap())
    private val cancelingNotifier: PublishSubject<String> = PublishSubject.create()
    private val waitingNotifier: PublishSubject<String> = PublishSubject.create()
    override fun requestCaptha(sid: String?, captcha: Captcha) {
        sid ?: return
        entryMap[sid] = Entry()
        startCapthaActivity(app, sid, captcha)
    }

    @SuppressLint("CheckResult")
    private fun startCapthaActivity(context: Context, sid: String, captcha: Captcha) {
        Completable.complete()
            .observeOn(uiScheduler)
            .subscribe {
                val intent = createIntent(context, sid, captcha.img)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
    }

    override fun cancel(sid: String) {
        entryMap.remove(sid)
        cancelingNotifier.onNext(sid)
    }

    override fun observeCanceling(): Observable<String> {
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
                waitingNotifier.onNext(lookupsid)
            }
        }
        val entry = entryMap[sid] ?: throw OutOfDateException()
        return entry.code
    }

    override fun observeWaiting(): Observable<String> {
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