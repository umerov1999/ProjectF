package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("photo_album")
class PhotoAlbumDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var size = 0
        private set
    var title: String? = null
        private set
    var description: String? = null
        private set
    var isCanUpload = false
        private set
    var updatedTime: Long = 0
        private set
    var createdTime: Long = 0
        private set
    var sizes: PhotoSizeEntity? = null
        private set
    var isUploadByAdminsOnly = false
        private set
    var isCommentsDisabled = false
        private set
    var privacyView: PrivacyEntity? = null
        private set
    var privacyComment: PrivacyEntity? = null
        private set

    operator fun set(id: Int, ownerId: Long): PhotoAlbumDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setSize(size: Int): PhotoAlbumDboEntity {
        this.size = size
        return this
    }

    fun setTitle(title: String?): PhotoAlbumDboEntity {
        this.title = title
        return this
    }

    fun setDescription(description: String?): PhotoAlbumDboEntity {
        this.description = description
        return this
    }

    fun setCanUpload(canUpload: Boolean): PhotoAlbumDboEntity {
        isCanUpload = canUpload
        return this
    }

    fun setUpdatedTime(updatedTime: Long): PhotoAlbumDboEntity {
        this.updatedTime = updatedTime
        return this
    }

    fun setCreatedTime(createdTime: Long): PhotoAlbumDboEntity {
        this.createdTime = createdTime
        return this
    }

    fun setSizes(sizes: PhotoSizeEntity?): PhotoAlbumDboEntity {
        this.sizes = sizes
        return this
    }

    fun setUploadByAdminsOnly(uploadByAdminsOnly: Boolean): PhotoAlbumDboEntity {
        isUploadByAdminsOnly = uploadByAdminsOnly
        return this
    }

    fun setCommentsDisabled(commentsDisabled: Boolean): PhotoAlbumDboEntity {
        isCommentsDisabled = commentsDisabled
        return this
    }

    fun setPrivacyView(privacyView: PrivacyEntity?): PhotoAlbumDboEntity {
        this.privacyView = privacyView
        return this
    }

    fun setPrivacyComment(privacyComment: PrivacyEntity?): PhotoAlbumDboEntity {
        this.privacyComment = privacyComment
        return this
    }
}