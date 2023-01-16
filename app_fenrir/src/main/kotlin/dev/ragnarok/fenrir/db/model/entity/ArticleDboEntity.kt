package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("article")
class ArticleDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var ownerName: String? = null
        private set
    var uRL: String? = null
        private set
    var title: String? = null
        private set
    var subTitle: String? = null
        private set
    var photo: PhotoDboEntity? = null
        private set
    var accessKey: String? = null
        private set
    var isFavorite = false
        private set

    operator fun set(id: Int, owner_id: Long): ArticleDboEntity {
        this.id = id
        ownerId = owner_id
        return this
    }

    fun setOwnerName(owner_name: String?): ArticleDboEntity {
        ownerName = owner_name
        return this
    }

    fun setURL(url: String?): ArticleDboEntity {
        uRL = url
        return this
    }

    fun setTitle(title: String?): ArticleDboEntity {
        this.title = title
        return this
    }

    fun setSubTitle(subtitle: String?): ArticleDboEntity {
        subTitle = subtitle
        return this
    }

    fun setPhoto(photo: PhotoDboEntity?): ArticleDboEntity {
        this.photo = photo
        return this
    }

    fun setAccessKey(access_key: String?): ArticleDboEntity {
        accessKey = access_key
        return this
    }

    fun setIsFavorite(is_favorite: Boolean): ArticleDboEntity {
        isFavorite = is_favorite
        return this
    }
}