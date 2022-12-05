package dev.ragnarok.fenrir.model

class PhotoWithOwner(private val photo: Photo, private val owner: Owner) {
    fun getOwner(): Owner {
        return owner
    }

    fun getPhoto(): Photo {
        return photo
    }
}