package dev.ragnarok.filegallery.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.filegallery.api.adapters.AudioDtoAdapter
import dev.ragnarok.filegallery.getBoolean
import dev.ragnarok.filegallery.putBoolean
import dev.ragnarok.filegallery.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.filegallery.util.Utils.stringEmptyIfNull
import kotlinx.serialization.Serializable

@Keep
@Serializable(with = AudioDtoAdapter::class)
class Audio : Parcelable {
    var id = 0
        private set
    var ownerId = 0
        private set
    var thumb_image: String? = null
        private set
    var artist: String? = null
        private set
    var title: String? = null
        private set
    var duration = 0
        private set
    var url: String? = null
        private set
    var isAnimationNow = false
    var isSelected = false
    var isLocal = false
        private set
    var isLocalServer = false
        private set
    var downloadIndicator = false

    constructor()
    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        artist = `in`.readString()
        title = `in`.readString()
        duration = `in`.readInt()
        url = `in`.readString()
        isAnimationNow = `in`.getBoolean()
        isSelected = `in`.getBoolean()
        isLocal = `in`.getBoolean()
        isLocalServer = `in`.getBoolean()
        downloadIndicator = `in`.getBoolean()
        thumb_image = `in`.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeString(artist)
        dest.writeString(title)
        dest.writeInt(duration)
        dest.writeString(url)
        dest.putBoolean(isAnimationNow)
        dest.putBoolean(isSelected)
        dest.putBoolean(isLocal)
        dest.putBoolean(isLocalServer)
        dest.putBoolean(downloadIndicator)
        dest.writeString(thumb_image)
    }

    fun updateDownloadIndicator(): Audio {
        downloadIndicator = TrackIsDownloaded(this)
        return this
    }

    fun setThumb_image(thumb_image: String?): Audio {
        this.thumb_image = thumb_image
        return this
    }

    fun setId(id: Int): Audio {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Int): Audio {
        this.ownerId = ownerId
        return this
    }

    fun setArtist(artist: String?): Audio {
        this.artist = artist
        return this
    }

    fun setTitle(title: String?): Audio {
        this.title = title
        return this
    }

    fun setDuration(duration: Int): Audio {
        this.duration = duration
        return this
    }

    fun setUrl(url: String?): Audio {
        this.url = url
        return this
    }

    fun setIsLocal(): Audio {
        isLocal = true
        return this
    }

    fun setIsLocalServer(): Audio {
        isLocalServer = true
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    val artistAndTitle: String
        get() = stringEmptyIfNull(artist) + " - " + stringEmptyIfNull(title)

    override fun equals(other: Any?): Boolean {
        if (other !is Audio) return false
        return id == other.id && ownerId == other.ownerId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId
        return result
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Audio> = object : Parcelable.Creator<Audio> {
            override fun createFromParcel(`in`: Parcel): Audio {
                return Audio(`in`)
            }

            override fun newArray(size: Int): Array<Audio?> {
                return arrayOfNulls(size)
            }
        }
    }
}