package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.nonNullNoEmpty

class Narratives : Parcelable {
    val id: Int
    val owner_id: Int
    var title: String? = null
        private set
    var cover: String? = null
        private set
    var stories: IntArray? = null
        private set

    constructor(id: Int, owner_id: Int) {
        this.id = id
        this.owner_id = owner_id
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        owner_id = `in`.readInt()
        title = `in`.readString()
        cover = `in`.readString()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeInt(owner_id)
        parcel.writeString(title)
        parcel.writeString(cover)
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
