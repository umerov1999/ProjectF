package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("audio_playlist")
class AudioPlaylistDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0
        private set
    var count = 0
        private set
    var update_time: Long = 0
        private set
    var year = 0
        private set
    var artist_name: String? = null
        private set
    var genre: String? = null
        private set
    var title: String? = null
        private set
    var description: String? = null
        private set
    var thumb_image: String? = null
        private set
    var access_key: String? = null
        private set
    var original_access_key: String? = null
        private set
    var original_id = 0
        private set
    var original_owner_id = 0
        private set

    fun setId(id: Int): AudioPlaylistDboEntity {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Int): AudioPlaylistDboEntity {
        this.ownerId = ownerId
        return this
    }

    fun setCount(count: Int): AudioPlaylistDboEntity {
        this.count = count
        return this
    }

    fun setUpdate_time(update_time: Long): AudioPlaylistDboEntity {
        this.update_time = update_time
        return this
    }

    fun setYear(Year: Int): AudioPlaylistDboEntity {
        year = Year
        return this
    }

    fun setArtist_name(artist_name: String?): AudioPlaylistDboEntity {
        this.artist_name = artist_name
        return this
    }

    fun setGenre(genre: String?): AudioPlaylistDboEntity {
        this.genre = genre
        return this
    }

    fun setTitle(title: String?): AudioPlaylistDboEntity {
        this.title = title
        return this
    }

    fun setDescription(description: String?): AudioPlaylistDboEntity {
        this.description = description
        return this
    }

    fun setThumb_image(thumb_image: String?): AudioPlaylistDboEntity {
        this.thumb_image = thumb_image
        return this
    }

    fun setAccess_key(access_key: String?): AudioPlaylistDboEntity {
        this.access_key = access_key
        return this
    }

    fun setOriginal_access_key(original_access_key: String?): AudioPlaylistDboEntity {
        this.original_access_key = original_access_key
        return this
    }

    fun setOriginal_id(original_id: Int): AudioPlaylistDboEntity {
        this.original_id = original_id
        return this
    }

    fun setOriginal_owner_id(original_owner_id: Int): AudioPlaylistDboEntity {
        this.original_owner_id = original_owner_id
        return this
    }
}