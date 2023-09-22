package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class ReactionWithAsset : Parcelable {
    var count: Int = 0
        private set
    var reaction_id = 0
        private set
    var big_animation: String? = null
        private set
    var small_animation: String? = null
        private set
    var static: String? = null
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        count = parcel.readInt()
        reaction_id = parcel.readInt()
        big_animation = parcel.readString()
        small_animation = parcel.readString()
        static = parcel.readString()
    }

    fun setReaction(reaction: Reaction): ReactionWithAsset {
        count = reaction.count
        reaction_id = reaction.reaction_id
        return this
    }

    fun setReactionAsset(reaction: ReactionAsset): ReactionWithAsset {
        if (reaction_id == 0) {
            reaction_id = reaction.reaction_id
            count = 0
        }
        big_animation = reaction.big_animation
        small_animation = reaction.small_animation
        static = reaction.static
        return this
    }

    fun setCount(count: Int): ReactionWithAsset {
        this.count = count
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(count)
        parcel.writeInt(reaction_id)
        parcel.writeString(big_animation)
        parcel.writeString(small_animation)
        parcel.writeString(static)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ReactionWithAsset> {
        override fun createFromParcel(parcel: Parcel): ReactionWithAsset {
            return ReactionWithAsset(parcel)
        }

        override fun newArray(size: Int): Array<ReactionWithAsset?> {
            return arrayOfNulls(size)
        }
    }
}
