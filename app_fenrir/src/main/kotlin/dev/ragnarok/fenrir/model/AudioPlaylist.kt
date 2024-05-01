package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

class AudioPlaylist : AbsModel {
    var id = 0
        private set
    var owner_id = 0L
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
    var subtitle: String? = null
        private set
    var subtitle_badge: Boolean = false
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
    var original_owner_id = 0L
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        owner_id = parcel.readLong()
        count = parcel.readInt()
        update_time = parcel.readLong()
        year = parcel.readInt()
        artist_name = parcel.readString()
        genre = parcel.readString()
        title = parcel.readString()
        subtitle = parcel.readString()
        subtitle_badge = parcel.getBoolean()
        description = parcel.readString()
        thumb_image = parcel.readString()
        access_key = parcel.readString()
        original_access_key = parcel.readString()
        original_id = parcel.readInt()
        original_owner_id = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(owner_id)
        parcel.writeInt(count)
        parcel.writeLong(update_time)
        parcel.writeInt(year)
        parcel.writeString(artist_name)
        parcel.writeString(genre)
        parcel.writeString(title)
        parcel.writeString(subtitle)
        parcel.putBoolean(subtitle_badge)
        parcel.writeString(description)
        parcel.writeString(thumb_image)
        parcel.writeString(access_key)
        parcel.writeString(original_access_key)
        parcel.writeInt(original_id)
        parcel.writeLong(original_owner_id)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_AUDIO_PLAYLIST
    }

    fun setId(id: Int): AudioPlaylist {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Long): AudioPlaylist {
        owner_id = ownerId
        return this
    }

    fun setCount(count: Int): AudioPlaylist {
        this.count = count
        return this
    }

    fun setUpdate_time(update_time: Long): AudioPlaylist {
        this.update_time = update_time
        return this
    }

    fun setYear(year: Int): AudioPlaylist {
        this.year = year
        return this
    }

    fun setArtist_name(artist_name: String?): AudioPlaylist {
        this.artist_name = artist_name
        return this
    }

    fun setGenre(genre: String?): AudioPlaylist {
        this.genre = genre
        return this
    }

    fun setTitle(title: String?): AudioPlaylist {
        this.title = title
        return this
    }

    fun setSubtitle(subtitle: String?): AudioPlaylist {
        this.subtitle = subtitle
        return this
    }

    fun setSubtitleBadge(subtitle_badge: Boolean): AudioPlaylist {
        this.subtitle_badge = subtitle_badge
        return this
    }

    fun setDescription(description: String?): AudioPlaylist {
        this.description = description
        return this
    }

    fun getDescriptionOrSubtitle(): String? {
        return if (description.isNullOrEmpty()) {
            subtitle
        } else {
            description
        }
    }

    fun setThumb_image(thumb_image: String?): AudioPlaylist {
        this.thumb_image = thumb_image
        return this
    }

    fun setAccess_key(access_key: String?): AudioPlaylist {
        this.access_key = access_key
        return this
    }

    fun setOriginal_access_key(original_access_key: String?): AudioPlaylist {
        this.original_access_key = original_access_key
        return this
    }

    fun setOriginal_id(original_id: Int): AudioPlaylist {
        this.original_id = original_id
        return this
    }

    fun setOriginal_owner_id(original_owner_id: Long): AudioPlaylist {
        this.original_owner_id = original_owner_id
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return other is AudioPlaylist && id == other.id && owner_id == other.owner_id
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + owner_id.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<AudioPlaylist> {
        override fun createFromParcel(parcel: Parcel): AudioPlaylist {
            return AudioPlaylist(parcel)
        }

        override fun newArray(size: Int): Array<AudioPlaylist?> {
            return arrayOfNulls(size)
        }
    }
}