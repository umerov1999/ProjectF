package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("link")
class LinkDboEntity : DboEntity() {
    var url: String? = null
        private set
    var title: String? = null
        private set
    var caption: String? = null
        private set
    var description: String? = null
        private set
    var previewPhoto: String? = null
        private set
    var photo: PhotoDboEntity? = null
        private set

    fun setUrl(url: String?): LinkDboEntity {
        this.url = url
        return this
    }

    fun setTitle(title: String?): LinkDboEntity {
        this.title = title
        return this
    }

    fun setCaption(caption: String?): LinkDboEntity {
        this.caption = caption
        return this
    }

    fun setDescription(description: String?): LinkDboEntity {
        this.description = description
        return this
    }

    fun setPhoto(photo: PhotoDboEntity?): LinkDboEntity {
        this.photo = photo
        return this
    }

    fun setPreviewPhoto(photo: String?): LinkDboEntity {
        previewPhoto = photo
        return this
    }
}