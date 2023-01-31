package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.activity.ValidateActivity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*

class ValidateProvider(private val app: Context, private val uiScheduler: Scheduler) :
    IValidateProvider {
    private val entryMap: MutableMap<String, Entry> = Collections.synchronizedMap(HashMap())
    private val cancelingNotifier: PublishSubject<String> = PublishSubject.create()
    private val waitingNotifier: PublishSubject<String> = PublishSubject.create()
    override fun requestValidate(url: String?, accountId: Long) {
        url ?: return
        entryMap[url] = Entry()
        startValidateActivity(app, url, accountId)
    }

    @SuppressLint("CheckResult")
    private fun startValidateActivity(context: Context, url: String, accountId: Long) {
        Completable.complete()
            .observeOn(Includes.provideMainThreadScheduler())
            .subscribe {
                val intent = ValidateActivity.createIntent(context, url, accountId)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
    }

    override fun cancel(url: String) {
        entryMap.remove(url)
        cancelingNotifier.onNext(url)
    }

    override fun observeCanceling(): Observable<String> {
        return cancelingNotifier
    }

    @Throws(OutOfDateException::class)
    override fun lookupState(url: String): Boolean {
        val iterator: MutableIterator<Map.Entry<String, Entry>> = entryMap.entries.iterator()
        while (iterator.hasNext()) {
            val (lookupsid, lookupEntry) = iterator.next()
            if (System.currentTimeMillis() - lookupEntry.lastActivityTime > MAX_WAIT_DELAY) {
                iterator.remove()
            } else {
                waitingNotifier.onNext(lookupsid)
            }
        }
        val entry = entryMap[url] ?: throw OutOfDateException()
        return entry.state
    }

    override fun observeWaiting(): Observable<String> {
        return waitingNotifier
    }

    override fun notifyThatValidateEntryActive(url: String) {
        val entry = entryMap[url]
        if (entry != null) {
            entry.lastActivityTime = System.currentTimeMillis()
        }
    }

    override fun enterState(url: String, state: Boolean) {
        val entry = entryMap[url]
        if (entry != null) {
            entry.state = state
        }
    }

    private class Entry {
        var state: Boolean = false
        var lastActivityTime: Long = System.currentTimeMillis()

    }

    companion object {
        private const val MAX_WAIT_DELAY = 15 * 60 * 1000
    }

}