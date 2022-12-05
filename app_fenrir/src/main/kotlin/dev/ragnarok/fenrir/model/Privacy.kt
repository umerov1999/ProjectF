package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Utils.join
import java.util.*

class Privacy : Parcelable, Cloneable {
    var type: String
        private set
    private var allowedUsers: ArrayList<User>
    private var disallowedUsers: ArrayList<User>
    private var allowedLists: ArrayList<FriendList>
    private var disallowedLists: ArrayList<FriendList>

    @JvmOverloads
    constructor(type: String = Type.ALL) {
        this.type = type
        allowedUsers = ArrayList()
        disallowedUsers = ArrayList()
        allowedLists = ArrayList()
        disallowedLists = ArrayList()
    }

    internal constructor(`in`: Parcel) {
        type = `in`.readString()!!
        allowedUsers = `in`.createTypedArrayList(User.CREATOR)!!
        disallowedUsers = `in`.createTypedArrayList(User.CREATOR)!!
        allowedLists = `in`.createTypedArrayList(FriendList.CREATOR)!!
        disallowedLists = `in`.createTypedArrayList(FriendList.CREATOR)!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(type)
        dest.writeTypedList(allowedUsers)
        dest.writeTypedList(disallowedUsers)
        dest.writeTypedList(allowedLists)
        dest.writeTypedList(disallowedLists)
    }

    fun setType(type: String): Privacy {
        this.type = type
        return this
    }

    fun getAllowedUsers(): List<User> {
        return Collections.unmodifiableList(allowedUsers)
    }

    fun getDisallowedUsers(): List<User> {
        return Collections.unmodifiableList(disallowedUsers)
    }

    fun getAllowedLists(): List<FriendList> {
        return Collections.unmodifiableList(allowedLists)
    }

    fun getDisallowedLists(): List<FriendList> {
        return Collections.unmodifiableList(disallowedLists)
    }

    fun allowFor(user: User): Privacy {
        if (!allowedUsers.contains(user)) {
            allowedUsers.add(user)
        }
        return this
    }

    fun disallowFor(user: User): Privacy {
        if (!disallowedUsers.contains(user)) {
            disallowedUsers.add(user)
        }
        return this
    }

    fun allowFor(friendList: FriendList): Privacy {
        if (!allowedLists.contains(friendList)) {
            allowedLists.add(friendList)
        }
        return this
    }

    fun disallowFor(friendList: FriendList): Privacy {
        if (!disallowedLists.contains(friendList)) {
            disallowedLists.add(friendList)
        }
        return this
    }

    fun removeFromAllowed(user: User) {
        allowedUsers.remove(user)
    }

    fun removeFromAllowed(friendList: FriendList) {
        allowedLists.remove(friendList)
    }

    fun removeFromDisallowed(user: User) {
        disallowedUsers.remove(user)
    }

    fun removeFromDisallowed(friendList: FriendList) {
        disallowedLists.remove(friendList)
    }

    fun createAllowedString(context: Context): String {
        val sufix: String = when (type) {
            Type.FRIENDS -> context.getString(R.string.privacy_to_friends_only)
            Type.FRIENDS_OF_FRIENDS, Type.FRIENDS_OF_FRIENDS_ONLY -> context.getString(
                R.string.privacy_to_friends_and_friends_of_friends
            )
            Type.ONLY_ME, Type.NOBODY -> context.getString(
                R.string.privacy_to_only_me
            )
            else -> context.getString(R.string.privacy_to_all_users)
        }
        val users = join(", ", allowedUsers)
        val friendsLists = join(", ", allowedLists)
        val additional =
            if (users.isEmpty()) friendsLists else if (friendsLists.isEmpty()) users else "$users, $friendsLists"
        val and = context.getString(R.string.and)
        return if (additional.isEmpty()) sufix else "$sufix $and $additional"
    }

    fun createDisallowedString(): String {
        val users = join(", ", disallowedUsers)
        val friendsLists = join(", ", disallowedLists)
        val additional =
            if (users.isEmpty()) friendsLists else if (friendsLists.isEmpty()) users else "$users, $friendsLists"
        return additional.ifEmpty { "-" }
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Privacy {
        val clone = super.clone() as Privacy
        clone.allowedUsers = ArrayList(allowedUsers)
        clone.allowedLists = ArrayList(allowedLists)
        clone.disallowedUsers = ArrayList(disallowedUsers)
        clone.disallowedLists = ArrayList(disallowedLists)
        return clone
    }

    object Type {
        const val ALL = "all"
        const val FRIENDS = "friends"
        const val FRIENDS_OF_FRIENDS = "friends_of_friends"
        const val FRIENDS_OF_FRIENDS_ONLY = "friends_of_friends_only"
        const val NOBODY = "nobody"
        const val ONLY_ME = "only_me"
    }

    companion object CREATOR : Parcelable.Creator<Privacy> {
        override fun createFromParcel(parcel: Parcel): Privacy {
            return Privacy(parcel)
        }

        override fun newArray(size: Int): Array<Privacy?> {
            return arrayOfNulls(size)
        }
    }
}