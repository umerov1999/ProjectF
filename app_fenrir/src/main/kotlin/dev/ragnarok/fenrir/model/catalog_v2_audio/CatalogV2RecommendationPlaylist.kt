package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import dev.ragnarok.fenrir.api.model.VKApiAudioPlaylist
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2RecommendedPlaylist
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.writeTypedObjectCompat
import kotlin.math.abs

class CatalogV2RecommendationPlaylist : AbsModel {
    var id = 0
        private set
    var owner_id = 0L
        private set

    @ColorInt
    var color: Int = 0
        private set
    var percentage: Double = 0.0
        private set
    var percentage_title: String? = null
        private set
    private var playlist: AudioPlaylist? = null
    var audios: ArrayList<Audio>? = null
        private set
    var owner: Owner? = null
        private set
    private var scrollToIt = false

    constructor(
        res: VKApiCatalogV2RecommendedPlaylist,
        profiles: List<VKApiUser>?,
        groups: List<VKApiCommunity>?,
        playlists: List<VKApiAudioPlaylist>?,
        transform: (List<String>) -> ArrayList<Audio>?
    ) {
        id = res.id
        owner_id = res.owner_id
        color = res.color?.toColor() ?: Color.BLACK
        percentage = res.percentage ?: 0.0
        percentage_title = res.percentage_title

        if (owner_id < 0) {
            for (d in groups.orEmpty()) {
                if (d.id == abs(owner_id)) {
                    owner = Dto2Model.transformOwner(d)
                    break
                }
            }
        } else {
            for (d in profiles.orEmpty()) {
                if (d.id == owner_id) {
                    owner = Dto2Model.transformOwner(d)
                    break
                }
            }
        }
        res.audios?.let {
            audios = transform(it)
        }

        for (s in playlists.orEmpty()) {
            if (s.id == id && s.owner_id == owner_id) {
                playlist = Dto2Model.transform(s)
                break
            }
        }
    }

    constructor()

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        owner_id = parcel.readLong()
        color = parcel.readInt()
        percentage = parcel.readDouble()
        percentage_title = parcel.readString()
        scrollToIt = parcel.getBoolean()
        audios = parcel.createTypedArrayList(Audio.CREATOR)
        playlist = parcel.readTypedObjectCompat(AudioPlaylist.CREATOR)
        val wrapper = parcel.readTypedObjectCompat(
            ParcelableOwnerWrapper.CREATOR
        )
        owner = wrapper?.owner
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(owner_id)
        parcel.writeInt(color)
        parcel.writeDouble(percentage)
        parcel.writeString(percentage_title)
        parcel.putBoolean(scrollToIt)
        parcel.writeTypedList(audios)
        parcel.writeTypedObjectCompat(playlist, flags)
        ParcelableOwnerWrapper.writeOwner(parcel, flags, owner)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_CATALOG_V2_RECOMMENDATION_PLAYLIST
    }

    fun setScroll() {
        scrollToIt = true
    }

    fun isScroll(): Boolean {
        val ret = scrollToIt
        scrollToIt = false
        return ret
    }

    fun getPlaylist(): AudioPlaylist {
        return playlist ?: AudioPlaylist().setId(id).setOwnerId(owner_id).setTitle("null")
    }

    fun getAudiosModels(): MutableList<AbsModel>? {
        if (audios.isNullOrEmpty()) {
            return null
        }
        val ret = ArrayList<AbsModel>()
        for (i in audios.orEmpty()) {
            ret.add(i)
        }
        return ret
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return other is CatalogV2RecommendationPlaylist && id == other.id && owner_id == other.owner_id
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + owner_id.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2RecommendationPlaylist> {
        override fun createFromParcel(parcel: Parcel): CatalogV2RecommendationPlaylist {
            return CatalogV2RecommendationPlaylist(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2RecommendationPlaylist?> {
            return arrayOfNulls(size)
        }
    }
}
