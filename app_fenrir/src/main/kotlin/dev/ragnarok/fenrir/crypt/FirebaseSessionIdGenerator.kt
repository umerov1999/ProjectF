package dev.ragnarok.fenrir.crypt

import com.google.firebase.database.*
import dev.ragnarok.fenrir.Constants
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

class FirebaseSessionIdGenerator : ISessionIdGenerator {
    override fun generateNextId(): Single<Long> {
        val database = FirebaseDatabase.getInstance(Constants.FCM_SESSION_ID_GEN_URL)
        val ref = database.reference
        val databaseCounter = ref.child("key_exchange_session_counter")

        //https://stackoverflow.com/questions/28915706/auto-increment-a-value-in-firebase
        return Single.create { emitter: SingleEmitter<Long> ->
            databaseCounter.runTransaction(object : Transaction.Handler {
                var nextValue: Long = 0
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    nextValue = if (currentData.value == null) {
                        1
                    } else {
                        ((currentData.value as Long?) ?: 0) + 1
                    }
                    currentData.value = nextValue
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    e: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (e != null) {
                        emitter.onError(SessionIdGenerationException(e.message))
                    } else {
                        emitter.onSuccess(nextValue)
                    }
                }
            })
        }
    }
}