package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class FriendsCounters : Parcelable {
    private val all: Int
    private val online: Int
    private val followers: Int
    private val mutual: Int

    constructor(all: Int, online: Int, followers: Int, mutual: Int) {
        this.all = all
        this.online = online
        this.followers = followers
        this.mutual = mutual
    }

    internal constructor(parcel: Parcel) {
        all = parcel.readInt()
        online = parcel.readInt()
        followers = parcel.readInt()
        mutual = parcel.readInt()
    }

    fun getAll(): Int {
        return all
    }

    fun getFollowers(): Int {
        return followers
    }

    fun getMutual(): Int {
        return mutual
    }

    fun getOnline(): Int {
        return online
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(all)
        parcel.writeInt(online)
        parcel.writeInt(followers)
        parcel.writeInt(mutual)
    }

    companion object CREATOR : Parcelable.Creator<FriendsCounters> {
        override fun createFromParcel(parcel: Parcel): FriendsCounters {
            return FriendsCounters(parcel)
        }

        override fun newArray(size: Int): Array<FriendsCounters?> {
            return arrayOfNulls(size)
        }
    }
}