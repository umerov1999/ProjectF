package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("graffiti")
class GraffitiDboEntity : DboEntity() {
    var id = 0
        private set
    var owner_id = 0
        private set
    var url: String? = null
        private set
    var width = 0
        private set
    var height = 0
        private set
    var access_key: String? = null
        private set

    fun setId(id: Int): GraffitiDboEntity {
        this.id = id
        return this
    }

    fun setOwner_id(owner_id: Int): GraffitiDboEntity {
        this.owner_id = owner_id
        return this
    }

    fun setUrl(url: String?): GraffitiDboEntity {
        this.url = url
        return this
    }

    fun setWidth(width: Int): GraffitiDboEntity {
        this.width = width
        return this
    }

    fun setHeight(height: Int): GraffitiDboEntity {
        this.height = height
        return this
    }

    fun setAccess_key(access_key: String?): GraffitiDboEntity {
        this.access_key = access_key
        return this
    }
}