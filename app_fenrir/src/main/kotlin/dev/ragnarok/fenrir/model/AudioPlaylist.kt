package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class AudioPlaylist : AbsModel {
    private var id = 0
    private var owner_id = 0
    private var count = 0
    private var update_time: Long = 0
    private var Year = 0
    private var artist_name: String? = null
    private var genre: String? = null
    private var title: String? = null
    private var description: String? = null
    private var thumb_image: String? = null
    private var access_key: String? = null
    private var original_access_key: String? = null
    private var original_id = 0
    private var original_owner_id = 0

    constructor()
    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        owner_id = parcel.readInt()
        count = parcel.readInt()
        update_time = parcel.readLong()
        Year = parcel.readInt()
        artist_name = parcel.readString()
        genre = parcel.readString()
        title = parcel.readString()
        description = parcel.readString()
        thumb_image = parcel.readString()
        access_key = parcel.readString()
        original_access_key = parcel.readString()
        original_id = parcel.readInt()
        original_owner_id = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(owner_id)
        parcel.writeInt(count)
        parcel.writeLong(update_time)
        parcel.writeInt(Year)
        parcel.writeString(artist_name)
        parcel.writeString(genre)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(thumb_image)
        parcel.writeString(access_key)
        parcel.writeString(original_access_key)
        parcel.writeInt(original_id)
        parcel.writeInt(original_owner_id)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_AUDIO_PLAYLIST
    }

    fun getId(): Int {
        return id
    }

    fun setId(id: Int): AudioPlaylist {
        this.id = id
        return this
    }

    fun getOwnerId(): Int {
        return owner_id
    }

    fun setOwnerId(ownerId: Int): AudioPlaylist {
        owner_id = ownerId
        return this
    }

    fun getCount(): Int {
        return count
    }

    fun setCount(count: Int): AudioPlaylist {
        this.count = count
        return this
    }

    fun getUpdate_time(): Long {
        return update_time
    }

    fun setUpdate_time(update_time: Long): AudioPlaylist {
        this.update_time = update_time
        return this
    }

    fun getYear(): Int {
        return Year
    }

    fun setYear(Year: Int): AudioPlaylist {
        this.Year = Year
        return this
    }

    fun getArtist_name(): String? {
        return artist_name
    }

    fun setArtist_name(artist_name: String?): AudioPlaylist {
        this.artist_name = artist_name
        return this
    }

    fun getGenre(): String? {
        return genre
    }

    fun setGenre(genre: String?): AudioPlaylist {
        this.genre = genre
        return this
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): AudioPlaylist {
        this.title = title
        return this
    }

    fun getDescription(): String? {
        return description
    }

    fun setDescription(description: String?): AudioPlaylist {
        this.description = description
        return this
    }

    fun getThumb_image(): String? {
        return thumb_image
    }

    fun setThumb_image(thumb_image: String?): AudioPlaylist {
        this.thumb_image = thumb_image
        return this
    }

    fun getAccess_key(): String? {
        return access_key
    }

    fun setAccess_key(access_key: String?): AudioPlaylist {
        this.access_key = access_key
        return this
    }

    fun getOriginal_access_key(): String? {
        return original_access_key
    }

    fun setOriginal_access_key(original_access_key: String?): AudioPlaylist {
        this.original_access_key = original_access_key
        return this
    }

    fun getOriginal_id(): Int {
        return original_id
    }

    fun setOriginal_id(original_id: Int): AudioPlaylist {
        this.original_id = original_id
        return this
    }

    fun getOriginal_owner_id(): Int {
        return original_owner_id
    }

    fun setOriginal_owner_id(original_owner_id: Int): AudioPlaylist {
        this.original_owner_id = original_owner_id
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AudioPlaylist) return false
        return id == other.id && owner_id == other.owner_id
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + owner_id
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