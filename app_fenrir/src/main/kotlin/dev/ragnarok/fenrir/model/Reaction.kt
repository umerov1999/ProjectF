package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Reaction : Parcelable {
    var count: Int = 0
        private set
    var reaction_id = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        count = parcel.readInt()
        reaction_id = parcel.readInt()
    }

    fun setCount(count: Int): Reaction {
        this.count = count
        return this
    }

    fun setReactionId(reaction_id: Int): Reaction {
        this.reaction_id = reaction_id
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(count)
        parcel.writeInt(reaction_id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Reaction> {
        override fun createFromParcel(parcel: Parcel): Reaction {
            return Reaction(parcel)
        }

        override fun newArray(size: Int): Array<Reaction?> {
            return arrayOfNulls(size)
        }
    }
}
