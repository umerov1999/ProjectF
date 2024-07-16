package dev.ragnarok.fenrir.push

import kotlinx.coroutines.flow.Flow

interface IPushRegistrationResolver {
    fun canReceivePushNotification(): Boolean
    fun resolvePushRegistration(): Flow<Boolean>
}