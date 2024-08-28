package dev.ragnarok.fenrir.push

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface IPushRegistrationResolver {
    fun canReceivePushNotification(accountId: Long): Boolean
    fun resolvePushRegistration(accountId: Long, context: Context): Flow<Boolean>
}