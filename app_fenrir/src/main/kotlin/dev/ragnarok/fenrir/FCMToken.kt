package dev.ragnarok.fenrir

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

object FCMToken {
    val fcmToken: Flow<String>
        get() = flow {
            val res = FirebaseMessaging.getInstance().token.await()
            if (res.nonNullNoEmpty()) {
                emit(res)
            } else {
                throw Throwable("fcmToken is empty!!!")
            }
        }
}