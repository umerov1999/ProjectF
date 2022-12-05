package dev.ragnarok.fenrir.db.model.entity

class VideoAlbumDboEntity(val id: Int, val ownerId: Int) : DboEntity() {
    var title: String? = null
        private set
    var image: String? = null
        private set
    var count = 0
        private set
    var updateTime: Long = 0
        private set
    var privacy: PrivacyEntity? = null
        private set

    fun setTitle(title: String?): VideoAlbumDboEntity {
        this.title = title
        return this
    }

    fun setImage(image: String?): VideoAlbumDboEntity {
        this.image = image
        return this
    }

    fun setCount(count: Int): VideoAlbumDboEntity {
        this.count = count
        return this
    }

    fun setUpdateTime(updateTime: Long): VideoAlbumDboEntity {
        this.updateTime = updateTime
        return this
    }

    fun setPrivacy(privacy: PrivacyEntity?): VideoAlbumDboEntity {
        this.privacy = privacy
        return this
    }
}