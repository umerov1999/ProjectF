package dev.ragnarok.fenrir.api

import io.reactivex.rxjava3.core.Observable

interface IValidateProvider {
    fun requestValidate(url: String?)
    fun cancel(url: String)
    fun observeCanceling(): Observable<String>

    @Throws(OutOfDateException::class)
    fun lookupState(url: String): Boolean

    fun observeWaiting(): Observable<String>

    fun notifyThatValidateEntryActive(url: String)

    fun enterState(url: String, state: Boolean)
}
