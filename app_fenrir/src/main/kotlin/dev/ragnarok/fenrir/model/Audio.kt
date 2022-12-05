package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.readStringMap
import dev.ragnarok.fenrir.util.Utils.stringEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.writeStringMap
import kotlinx.serialization.Serializable

@Keep
@Serializable
class Audio : AbsModel {
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
    var isDeleted = false
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
    var isAnimationNow = false
    var isSelected = false
    var isHq = false
        private set
    var isLocal = false
        private set
    var isLocalServer = false
        private set
    var downloadIndicator = 0

    constructor()
    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        artist = `in`.readString()
        title = `in`.readString()
        duration = `in`.readInt()
        url = `in`.readString()
        lyricsId = `in`.readInt()
        date = `in`.readLong()
        albumId = `in`.readInt()
        album_owner_id = `in`.readInt()
        album_access_key = `in`.readString()
        genre = `in`.readInt()
        accessKey = `in`.readString()
        isDeleted = `in`.getBoolean()
        thumb_image_big = `in`.readString()
        thumb_image_very_big = `in`.readString()
        thumb_image_little = `in`.readString()
        album_title = `in`.readString()
        isAnimationNow = `in`.getBoolean()
        isSelected = `in`.getBoolean()
        isHq = `in`.getBoolean()
        main_artists = readStringMap(`in`)
        isLocal = `in`.getBoolean()
        isLocalServer = `in`.getBoolean()
        downloadIndicator = `in`.readInt()
    }

    /*
    public static String getMp3FromM3u8(String url) {
        if (Utils.isEmpty(url) || !url.contains("index.m3u8"))
            return url;
        if (url.contains("/audios/")) {
            final String regex = "^(.+?)/[^/]+?/audios/([^/]+)/.+$";
            final String subst = "$1/audios/$2.mp3";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);

            return matcher.replaceFirst(subst);
        } else {
            final String regex = "^(.+?)/(p[0-9]+)/[^/]+?/([^/]+)/.+$";
            final String subst = "$1/$2/$3.mp3";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);
            return matcher.replaceFirst(subst);
        }
    }
     */
    fun needRefresh(): Pair<Boolean, Boolean> {
        val empty_url = url.isNullOrEmpty()
        val refresh_old = get().other().isUse_api_5_90_for_audio
        return Pair(empty_url || refresh_old && isHLS, refresh_old)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_AUDIO
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeString(artist)
        parcel.writeString(title)
        parcel.writeInt(duration)
        parcel.writeString(url)
        parcel.writeInt(lyricsId)
        parcel.writeLong(date)
        parcel.writeInt(albumId)
        parcel.writeInt(album_owner_id)
        parcel.writeString(album_access_key)
        parcel.writeInt(genre)
        parcel.writeString(accessKey)
        parcel.putBoolean(isDeleted)
        parcel.writeString(thumb_image_big)
        parcel.writeString(thumb_image_very_big)
        parcel.writeString(thumb_image_little)
        parcel.writeString(album_title)
        parcel.putBoolean(isAnimationNow)
        parcel.putBoolean(isSelected)
        parcel.putBoolean(isHq)
        writeStringMap(parcel, main_artists)
        parcel.putBoolean(isLocal)
        parcel.putBoolean(isLocalServer)
        parcel.writeInt(downloadIndicator)
    }

    fun updateDownloadIndicator(): Audio {
        downloadIndicator = TrackIsDownloaded(this)
        return this
    }

    fun setId(id: Int): Audio {
        this.id = id
        return this
    }

    fun setIsHq(isHq: Boolean): Audio {
        this.isHq = isHq
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

    @get:DrawableRes
    val songIcon: Int
        get() {
            if (url.isNullOrEmpty()) {
                return R.drawable.audio_died
            } else if ("https://vk.com/mp3/audio_api_unavailable.mp3" == url) {
                return R.drawable.report
            }
            return R.drawable.song
        }

    fun setUrl(url: String?): Audio {
        this.url = url
        return this
    }

    val isHLS: Boolean
        get() = url?.contains("index.m3u8") == true

    fun setAlbum_title(album_title: String?): Audio {
        this.album_title = album_title
        return this
    }

    fun setThumb_image_little(thumb_image_little: String?): Audio {
        this.thumb_image_little = thumb_image_little
        return this
    }

    fun setThumb_image_big(thumb_image_big: String?): Audio {
        this.thumb_image_big = thumb_image_big
        return this
    }

    fun setThumb_image_very_big(thumb_image_very_big: String?): Audio {
        this.thumb_image_very_big = thumb_image_very_big
        return this
    }

    fun setAlbum_owner_id(album_owner_id: Int): Audio {
        this.album_owner_id = album_owner_id
        return this
    }

    fun setAlbum_access_key(album_access_key: String?): Audio {
        this.album_access_key = album_access_key
        return this
    }

    fun setLyricsId(lyricsId: Int): Audio {
        this.lyricsId = lyricsId
        return this
    }

    fun setDate(date: Long): Audio {
        this.date = date
        return this
    }

    fun setAlbumId(albumId: Int): Audio {
        this.albumId = albumId
        return this
    }

    fun setGenre(genre: Int): Audio {
        this.genre = genre
        return this
    }

    val genreByID3: Int
        get() {
            when (genre) {
                VKApiAudio.Genre.ROCK -> return 17
                VKApiAudio.Genre.POP, VKApiAudio.Genre.INDIE_POP -> return 13
                VKApiAudio.Genre.EASY_LISTENING -> return 98
                VKApiAudio.Genre.DANCE_AND_HOUSE -> return 125
                VKApiAudio.Genre.INSTRUMENTAL -> return 33
                VKApiAudio.Genre.METAL -> return 9
                VKApiAudio.Genre.DRUM_AND_BASS -> return 127
                VKApiAudio.Genre.TRANCE -> return 31
                VKApiAudio.Genre.CHANSON -> return 102
                VKApiAudio.Genre.ETHNIC -> return 48
                VKApiAudio.Genre.ACOUSTIC_AND_VOCAL -> return 99
                VKApiAudio.Genre.REGGAE -> return 16
                VKApiAudio.Genre.CLASSICAL -> return 32
                VKApiAudio.Genre.OTHER -> return 12
                VKApiAudio.Genre.SPEECH -> return 101
                VKApiAudio.Genre.ALTERNATIVE -> return 20
                VKApiAudio.Genre.ELECTROPOP_AND_DISCO -> return 52
                VKApiAudio.Genre.JAZZ_AND_BLUES -> return 8
            }
            return 0
        }

    fun setAccessKey(accessKey: String?): Audio {
        this.accessKey = accessKey
        return this
    }

    fun setMain_artists(main_artists: Map<String, String>?): Audio {
        this.main_artists = main_artists
        return this
    }

    fun setDeleted(deleted: Boolean): Audio {
        isDeleted = deleted
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

    @Keep
    @Serializable
    class AudioCommentTag {
        constructor(owner_id: Int, id: Int, lyricText: String?) {
            this.owner_id = owner_id
            this.id = id
            this.lyricText = lyricText
            this.lyricText?.let {
                this.lyricText = it.replace('\"', '_')
                this.lyricText = it.replace('\'', '_')
            }
        }

        constructor(owner_id: Int, id: Int) {
            this.owner_id = owner_id
            this.id = id
        }

        var owner_id: Int = 0
        var id: Int = 0
        var lyricText: String? = null
        fun toText(): String {
            return kJson.encodeToString(serializer(), this)
        }
    }

    companion object CREATOR : Parcelable.Creator<Audio> {
        override fun createFromParcel(parcel: Parcel): Audio {
            return Audio(parcel)
        }

        override fun newArray(size: Int): Array<Audio?> {
            return arrayOfNulls(size)
        }
    }
}