package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("story")
class StoryDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var date: Long = 0
        private set
    var expires: Long = 0
        private set
    var isIs_expired = false
        private set
    var accessKey: String? = null
        private set
    var target_url: String? = null
        private set
    var photo: PhotoDboEntity? = null
        private set
    var video: VideoDboEntity? = null
        private set

    fun setPhoto(photo: PhotoDboEntity?): StoryDboEntity {
        this.photo = photo
        return this
    }

    fun setId(id: Int): StoryDboEntity {
        this.id = id
        return this
    }

    fun setVideo(video: VideoDboEntity?): StoryDboEntity {
        this.video = video
        return this
    }

    fun setOwnerId(ownerId: Long): StoryDboEntity {
        this.ownerId = ownerId
        return this
    }

    fun setDate(date: Long): StoryDboEntity {
        this.date = date
        return this
    }

    fun setExpires(expires_at: Long): StoryDboEntity {
        expires = expires_at
        return this
    }

    fun setIs_expired(is_expired: Boolean): StoryDboEntity {
        isIs_expired = is_expired
        return this
    }

    fun setAccessKey(access_key: String?): StoryDboEntity {
        accessKey = access_key
        return this
    }

    fun setTarget_url(target_url: String?): StoryDboEntity {
        this.target_url = target_url
        return this
    }
}