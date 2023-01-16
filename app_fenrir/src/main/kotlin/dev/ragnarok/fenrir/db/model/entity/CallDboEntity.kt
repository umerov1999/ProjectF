package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("call")
class CallDboEntity : DboEntity() {
    var initiator_id = 0L
        private set
    var receiver_id = 0L
        private set
    var state: String? = null
        private set
    var time: Long = 0
        private set

    fun setInitiator_id(initiator_id: Long): CallDboEntity {
        this.initiator_id = initiator_id
        return this
    }

    fun setReceiver_id(receiver_id: Long): CallDboEntity {
        this.receiver_id = receiver_id
        return this
    }

    fun setTime(time: Long): CallDboEntity {
        this.time = time
        return this
    }

    fun setState(state: String?): CallDboEntity {
        this.state = state
        return this
    }
}