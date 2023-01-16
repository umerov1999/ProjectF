package dev.ragnarok.fenrir.db.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class IdPairEntity {
    var id = 0
        private set

    var ownerId = 0L
        private set

    operator fun set(id: Int, ownerId: Long): IdPairEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }
}