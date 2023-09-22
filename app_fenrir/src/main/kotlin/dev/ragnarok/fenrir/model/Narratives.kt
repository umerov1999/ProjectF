package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.nonNullNoEmpty

class Narratives : AbsModel {
    val id: Int
    val owner_id: Long
    var accessKey: String? = null
        private set
    var title: String? = null
        private set
    var cover: String? = null
        private set
    var stories: IntArray? = null
        private set

    constructor(id: Int, owner_id: Long) {
        this.id = id
        this.owner_id = owner_id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        owner_id = parcel.readLong()
        title = parcel.readString()
        cover = parcel.readString()
        accessKey = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeLong(owner_id)
        parcel.writeString(title)
        parcel.writeString(cover)
        parcel.writeString(accessKey)
    }

    fun getStoriesIds(): List<AccessIdPair> {
        val ret: ArrayList<AccessIdPair> = ArrayList()
        stories.nonNullNoEmpty {
            for (i in it) {
                ret.add(AccessIdPair(i, owner_id, null))
            }
        }
        return ret
    }

    fun setTitle(title: String?): Narratives {
        this.title = title
        return this
    }

    fun setCover(cover: String?): Narratives {
        this.cover = cover
        return this
    }

    fun setStory_ids(stories: IntArray?): Narratives {
        this.stories = stories
        return this
    }

    fun setAccessKey(access_key: String?): Narratives {
        accessKey = access_key
        return this
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_NARRATIVE
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Narratives) return false
        return id == other.id && owner_id == other.owner_id
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + owner_id.hashCode()
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Narratives> {
        override fun createFromParcel(parcel: Parcel): Narratives {
            return Narratives(parcel)
        }

        override fun newArray(size: Int): Array<Narratives?> {
            return arrayOfNulls(size)
        }
    }
}
