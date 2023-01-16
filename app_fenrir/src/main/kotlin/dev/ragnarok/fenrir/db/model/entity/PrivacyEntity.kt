package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class PrivacyEntity {
    var type: String? = null
        private set

    var entries: List<Entry>? = null
        private set

    operator fun set(type: String?, entries: List<Entry>?): PrivacyEntity {
        this.type = type
        this.entries = entries
        return this
    }

    @Keep
    @Serializable
    class Entry {
        var type = 0
            private set

        var id = 0L
            private set

        var isAllowed = false
            private set

        operator fun set(type: Int, id: Long, allowed: Boolean): Entry {
            this.type = type
            this.id = id
            isAllowed = allowed
            return this
        }
    }
}