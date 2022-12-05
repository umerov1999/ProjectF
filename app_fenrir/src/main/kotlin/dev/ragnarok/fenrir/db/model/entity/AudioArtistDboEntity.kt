package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("audio_artist")
class AudioArtistDboEntity : DboEntity() {
    var id: String? = null
        private set
    var name: String? = null
        private set
    var photo: List<AudioArtistImageEntity>? = null
        private set

    fun setId(id: String?): AudioArtistDboEntity {
        this.id = id
        return this
    }

    fun setName(name: String?): AudioArtistDboEntity {
        this.name = name
        return this
    }

    fun setPhoto(photo: List<AudioArtistImageEntity>?): AudioArtistDboEntity {
        this.photo = photo
        return this
    }

    @Keep
    @Serializable
    class AudioArtistImageEntity {
        var url: String? = null
            private set
        var width = 0
            private set
        var height = 0
            private set

        operator fun set(url: String?, width: Int, height: Int): AudioArtistImageEntity {
            this.url = url
            this.width = width
            this.height = height
            return this
        }
    }
}