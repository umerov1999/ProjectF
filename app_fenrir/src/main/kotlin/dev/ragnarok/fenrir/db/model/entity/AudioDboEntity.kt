package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("audio")
class AudioDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0
        private set
    var artist: String? = null
        private set
    var title: String? = null
        private set
    var duration = 0
        private set
    var url: String? = null
        private set
    var lyricsId = 0
        private set
    var date: Long = 0
        private set
    var albumId = 0
        private set
    var album_owner_id = 0
        private set
    var album_access_key: String? = null
        private set
    var genre = 0
        private set
    var accessKey: String? = null
        private set
    var thumb_image_little: String? = null
        private set
    var thumb_image_big: String? = null
        private set
    var thumb_image_very_big: String? = null
        private set
    var album_title: String? = null
        private set
    var main_artists: Map<String, String>? = null
        private set
    var isHq = false
        private set

    operator fun set(id: Int, ownerId: Int): AudioDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setAlbum_title(album_title: String?): AudioDboEntity {
        this.album_title = album_title
        return this
    }

    fun setThumb_image_little(thumb_image_little: String?): AudioDboEntity {
        this.thumb_image_little = thumb_image_little
        return this
    }

    fun setThumb_image_big(thumb_image_big: String?): AudioDboEntity {
        this.thumb_image_big = thumb_image_big
        return this
    }

    fun setThumb_image_very_big(thumb_image_very_big: String?): AudioDboEntity {
        this.thumb_image_very_big = thumb_image_very_big
        return this
    }

    fun setAlbum_owner_id(album_owner_id: Int): AudioDboEntity {
        this.album_owner_id = album_owner_id
        return this
    }

    fun setAlbum_access_key(album_access_key: String?): AudioDboEntity {
        this.album_access_key = album_access_key
        return this
    }

    fun setIsHq(isHq: Boolean): AudioDboEntity {
        this.isHq = isHq
        return this
    }

    fun setArtist(artist: String?): AudioDboEntity {
        this.artist = artist
        return this
    }

    fun setTitle(title: String?): AudioDboEntity {
        this.title = title
        return this
    }

    fun setDuration(duration: Int): AudioDboEntity {
        this.duration = duration
        return this
    }

    fun setUrl(url: String?): AudioDboEntity {
        this.url = url
        return this
    }

    fun setLyricsId(lyricsId: Int): AudioDboEntity {
        this.lyricsId = lyricsId
        return this
    }

    fun setDate(date: Long): AudioDboEntity {
        this.date = date
        return this
    }

    fun setAlbumId(albumId: Int): AudioDboEntity {
        this.albumId = albumId
        return this
    }

    fun setGenre(genre: Int): AudioDboEntity {
        this.genre = genre
        return this
    }

    fun setAccessKey(accessKey: String?): AudioDboEntity {
        this.accessKey = accessKey
        return this
    }

    fun setMain_artists(main_artists: Map<String, String>?): AudioDboEntity {
        this.main_artists = main_artists
        return this
    }
}