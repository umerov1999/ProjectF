package dev.ragnarok.fenrir.api

import kotlinx.coroutines.flow.SharedFlow

interface IValidateProvider {
    fun requestValidate(url: String?, accountId: Long)
    fun cancel(url: String)
    fun observeCanceling(): SharedFlow<String>

    @Throws(OutOfDateException::class)
    fun lookupState(url: String): Boolean

    fun observeWaiting(): SharedFlow<String>

    fun notifyThatValidateEntryActive(url: String)

    fun enterState(url: String, state: Boolean)
}
