package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("not_supported")
class NotSupportedDboEntity : DboEntity() {
    var type: String? = null
        private set
    var body: String? = null
        private set

    fun setType(type: String?): NotSupportedDboEntity {
        this.type = type
        return this
    }

    fun setBody(body: String?): NotSupportedDboEntity {
        this.body = body
        return this
    }
}