package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("market_album")
class MarketAlbumDboEntity : DboEntity() {
    var id = 0
        private set
    var owner_id = 0L
        private set
    var access_key: String? = null
        private set
    var title: String? = null
        private set
    var photo: PhotoDboEntity? = null
        private set
    var count = 0
        private set
    var updated_time: Long = 0
        private set

    operator fun set(id: Int, owner_id: Long): MarketAlbumDboEntity {
        this.id = id
        this.owner_id = owner_id
        return this
    }

    fun setAccess_key(access_key: String?): MarketAlbumDboEntity {
        this.access_key = access_key
        return this
    }

    fun setTitle(title: String?): MarketAlbumDboEntity {
        this.title = title
        return this
    }

    fun setPhoto(photo: PhotoDboEntity?): MarketAlbumDboEntity {
        this.photo = photo
        return this
    }

    fun setCount(count: Int): MarketAlbumDboEntity {
        this.count = count
        return this
    }

    fun setUpdated_time(updated_time: Long): MarketAlbumDboEntity {
        this.updated_time = updated_time
        return this
    }
}