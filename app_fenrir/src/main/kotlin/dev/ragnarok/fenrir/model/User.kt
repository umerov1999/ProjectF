package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.fenrir.CheckDonate
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.settings.Settings.get
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.isValueAssigned
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Keep
@Serializable
@SerialName("user")
class User : Owner {
    private val id: Long
    var firstName: String? = null
        private set
    var lastName: String? = null
        private set
    var isOnline = false
        private set
    var isOnlineMobile = false
        private set
    var onlineApp = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    var photoMax: String? = null
        private set
    var lastSeen: Long = 0
        private set
    var bdate: String? = null
        private set
    var hasUnseenStories = false
        private set

    @UserPlatform
    var platform = 0
        private set
    var status: String? = null
        private set

    @Sex
    var sex = 0
        private set
    override var domain: String? = null
        private set
    var maiden_name: String? = null
        private set
    var isFriend = false
        private set
    var friendStatus = 0
        private set
    var canWritePrivateMessage = false
        private set
    var blacklisted_by_me = false
        private set
    var blacklisted = false
        private set
    private var verified = false
    var isCan_access_closed = false
        private set

    constructor(id: Long) : super(OwnerType.USER) {
        this.id = id
    }

    internal constructor(parcel: Parcel) : super(parcel) {
        id = parcel.readLong()
        firstName = parcel.readString()
        lastName = parcel.readString()
        isOnline = parcel.getBoolean()
        isOnlineMobile = parcel.getBoolean()
        onlineApp = parcel.readInt()
        photo50 = parcel.readString()
        photo100 = parcel.readString()
        photo200 = parcel.readString()
        photoMax = parcel.readString()
        lastSeen = parcel.readLong()
        platform = parcel.readInt()
        status = parcel.readString()
        sex = parcel.readInt()
        domain = parcel.readString()
        maiden_name = parcel.readString()
        isFriend = parcel.getBoolean()
        friendStatus = parcel.readInt()
        canWritePrivateMessage = parcel.getBoolean()
        blacklisted_by_me = parcel.getBoolean()
        blacklisted = parcel.getBoolean()
        verified = parcel.getBoolean()
        isCan_access_closed = parcel.getBoolean()
        bdate = parcel.readString()
        hasUnseenStories = parcel.getBoolean()
    }

    internal constructor(parcel: ParcelNative) : super(parcel) {
        id = parcel.readLong()
        firstName = parcel.readString()
        lastName = parcel.readString()
        isOnline = parcel.readBoolean()
        isOnlineMobile = parcel.readBoolean()
        onlineApp = parcel.readInt()
        photo50 = parcel.readString()
        photo100 = parcel.readString()
        photo200 = parcel.readString()
        photoMax = parcel.readString()
        lastSeen = parcel.readLong()
        platform = parcel.readInt()
        status = parcel.readString()
        sex = parcel.readInt()
        domain = parcel.readString()
        maiden_name = parcel.readString()
        isFriend = parcel.readBoolean()
        friendStatus = parcel.readInt()
        canWritePrivateMessage = parcel.readBoolean()
        blacklisted_by_me = parcel.readBoolean()
        blacklisted = parcel.readBoolean()
        verified = parcel.readBoolean()
        isCan_access_closed = parcel.readBoolean()
        bdate = parcel.readString()
        hasUnseenStories = parcel.readBoolean()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_USER
    }

    override val fullName: String
        get() {
            val custom = get().main().getUserNameChanges(id)
            return if (custom.nonNullNoEmpty()) {
                custom
            } else "$firstName $lastName"
        }

    fun setFirstName(firstName: String?): User {
        this.firstName = firstName
        return this
    }

    fun setLastName(lastName: String?): User {
        this.lastName = lastName
        return this
    }

    fun setOnline(online: Boolean): User {
        isOnline = online
        return this
    }

    fun setOnlineMobile(onlineMobile: Boolean): User {
        isOnlineMobile = onlineMobile
        return this
    }

    fun setOnlineApp(onlineApp: Int): User {
        this.onlineApp = onlineApp
        return this
    }

    fun setHasUnseenStories(hasUnseenStories: Boolean): User {
        this.hasUnseenStories = hasUnseenStories
        return this
    }

    fun setPhoto50(photo50: String?): User {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): User {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): User {
        this.photo200 = photo200
        return this
    }

    fun setPhotoMax(photoMax: String?): User {
        this.photoMax = photoMax
        return this
    }

    fun setLastSeen(lastSeen: Long): User {
        this.lastSeen = lastSeen
        return this
    }

    fun setPlatform(@UserPlatform platform: Int): User {
        this.platform = platform
        return this
    }

    fun setStatus(status: String?): User {
        this.status = status
        return this
    }

    fun setSex(@Sex sex: Int): User {
        this.sex = sex
        return this
    }

    fun setDomain(domain: String?): User {
        this.domain = domain
        return this
    }

    fun setFriend(friend: Boolean): User {
        isFriend = friend
        return this
    }

    fun setBdate(bdate: String?): User {
        this.bdate = bdate
        return this
    }

    fun setFriendStatus(friendStatus: Int): User {
        this.friendStatus = friendStatus
        return this
    }

    fun setCanWritePrivateMessage(can_write_private_message: Boolean): User {
        canWritePrivateMessage = can_write_private_message
        return this
    }

    fun setBlacklisted_by_me(blacklisted_by_me: Boolean): User {
        this.blacklisted_by_me = blacklisted_by_me
        return this
    }

    fun setBlacklisted(blacklisted: Boolean): User {
        this.blacklisted = blacklisted
        return this
    }

    override val isDonated: Boolean
        get() = isValueAssigned(ownerId, CheckDonate.donatedOwnersLocal)

    override val isVerified: Boolean
        get() = verified || isDonated

    override val isHasUnseenStories: Boolean
        get() {
            return hasUnseenStories
        }

    fun setVerified(verified: Boolean): User {
        this.verified = verified
        return this
    }

    fun setCan_access_closed(can_access_closed: Boolean): User {
        isCan_access_closed = can_access_closed
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeLong(id)
        parcel.writeString(firstName)
        parcel.writeString(lastName)
        parcel.putBoolean(isOnline)
        parcel.putBoolean(isOnlineMobile)
        parcel.writeInt(onlineApp)
        parcel.writeString(photo50)
        parcel.writeString(photo100)
        parcel.writeString(photo200)
        parcel.writeString(photoMax)
        parcel.writeLong(lastSeen)
        parcel.writeInt(platform)
        parcel.writeString(status)
        parcel.writeInt(sex)
        parcel.writeString(domain)
        parcel.writeString(maiden_name)
        parcel.putBoolean(isFriend)
        parcel.writeInt(friendStatus)
        parcel.putBoolean(canWritePrivateMessage)
        parcel.putBoolean(blacklisted_by_me)
        parcel.putBoolean(blacklisted)
        parcel.putBoolean(verified)
        parcel.putBoolean(isCan_access_closed)
        parcel.writeString(bdate)
        parcel.putBoolean(hasUnseenStories)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        super.writeToParcelNative(dest)
        dest.writeLong(id)
        dest.writeString(firstName)
        dest.writeString(lastName)
        dest.writeBoolean(isOnline)
        dest.writeBoolean(isOnlineMobile)
        dest.writeInt(onlineApp)
        dest.writeString(photo50)
        dest.writeString(photo100)
        dest.writeString(photo200)
        dest.writeString(photoMax)
        dest.writeLong(lastSeen)
        dest.writeInt(platform)
        dest.writeString(status)
        dest.writeInt(sex)
        dest.writeString(domain)
        dest.writeString(maiden_name)
        dest.writeBoolean(isFriend)
        dest.writeInt(friendStatus)
        dest.writeBoolean(canWritePrivateMessage)
        dest.writeBoolean(blacklisted_by_me)
        dest.writeBoolean(blacklisted)
        dest.writeBoolean(verified)
        dest.writeBoolean(isCan_access_closed)
        dest.writeString(bdate)
        dest.writeBoolean(hasUnseenStories)
    }

    override val ownerId: Long
        get() = abs(id)

    override fun describeContents(): Int {
        return 0
    }

    override fun getOwnerObjectId(): Long {
        return abs(id)
    }

    override fun get100photoOrSmaller(): String? {
        return firstNonEmptyString(photo100, photo50)
    }

    override val maxSquareAvatar: String?
        get() = firstNonEmptyString(photo200, photo100, photo50)
    override val originalAvatar: String?
        get() = firstNonEmptyString(photoMax, photo200, photo100, photo50)

    fun setMaiden_name(maiden_name: String?): User {
        this.maiden_name = maiden_name
        return this
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<User> = object : Parcelable.Creator<User> {
            override fun createFromParcel(parcel: Parcel): User {
                return User(parcel)
            }

            override fun newArray(size: Int): Array<User?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<User> =
            object : ParcelNative.Creator<User> {
                override fun readFromParcelNative(dest: ParcelNative): User {
                    return User(dest)
                }

            }
    }
}
