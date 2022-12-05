package dev.ragnarok.fenrir.db.model.entity

class FaveLinkEntity(val id: String?, val url: String?) {
    var title: String? = null
        private set
    var description: String? = null
        private set
    var photo: PhotoDboEntity? = null
        private set

    fun setTitle(title: String?): FaveLinkEntity {
        this.title = title
        return this
    }

    fun setDescription(description: String?): FaveLinkEntity {
        this.description = description
        return this
    }

    fun setPhoto(photo: PhotoDboEntity?): FaveLinkEntity {
        this.photo = photo
        return this
    }
}