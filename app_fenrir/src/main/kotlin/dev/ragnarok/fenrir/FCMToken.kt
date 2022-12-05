package dev.ragnarok.fenrir

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

object FCMToken {
    val fcmToken: Single<String>
        get() = Single.create { emitter: SingleEmitter<String> ->
            val listener = OnCompleteListener { task: Task<String> ->
                if (task.isSuccessful) {
                    emitter.onSuccess(task.result)
                } else {
                    emitter.tryOnError(task.exception ?: Throwable("fcmToken!!!"))
                }
            }
            FirebaseMessaging.getInstance().token.addOnCompleteListener(listener)
        }
}