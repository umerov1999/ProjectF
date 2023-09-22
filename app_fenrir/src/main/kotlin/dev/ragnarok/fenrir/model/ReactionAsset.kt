package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class ReactionAsset : Parcelable {
    var big_animation: String? = null
        private set
    var small_animation: String? = null
        private set
    var static: String? = null
        private set
    var reaction_id = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        big_animation = parcel.readString()
        small_animation = parcel.readString()
        static = parcel.readString()
        reaction_id = parcel.readInt()
    }

    fun setBigAnimation(big_animation: String?): ReactionAsset {
        this.big_animation = big_animation
        return this
    }

    fun setSmallAnimation(small_animation: String?): ReactionAsset {
        this.small_animation = small_animation
        return this
    }

    fun setStatic(static: String?): ReactionAsset {
        this.static = static
        return this
    }

    fun setReactionId(reaction_id: Int): ReactionAsset {
        this.reaction_id = reaction_id
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(big_animation)
        parcel.writeString(small_animation)
        parcel.writeString(static)
        parcel.writeInt(reaction_id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ReactionAsset> {
        override fun createFromParcel(parcel: Parcel): ReactionAsset {
            return ReactionAsset(parcel)
        }

        override fun newArray(size: Int): Array<ReactionAsset?> {
            return arrayOfNulls(size)
        }
    }
}
