package dev.ragnarok.fenrir.push

import io.reactivex.rxjava3.core.Completable

interface IPushRegistrationResolver {
    fun canReceivePushNotification(): Boolean
    fun resolvePushRegistration(): Completable
}