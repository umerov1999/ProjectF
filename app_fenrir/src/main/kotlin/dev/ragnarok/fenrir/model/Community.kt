package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.fenrir.CheckDonate
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.Utils.isValueAssigned
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Keep
@Serializable
@SerialName("community")
class Community : Owner {
    val id: Int
    override var fullName: String? = null
        private set
    override val domain: String?
        get() = screenName
    private var screenName: String? = null
    var closed = 0
        private set
    var isAdmin = false
        private set
    var adminLevel = 0
        private set
    var isMember = false
        private set
    var membersCount = 0
        private set
    var memberStatus = 0
        private set
    var communityType = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    var verified = false
        private set
    var isBlacklisted = false
        private set

    constructor(id: Int) : super(OwnerType.COMMUNITY) {
        this.id = id
    }

    internal constructor(`in`: Parcel) : super(`in`) {
        id = `in`.readInt()
        fullName = `in`.readString()
        screenName = `in`.readString()
        closed = `in`.readInt()
        isAdmin = `in`.getBoolean()
        adminLevel = `in`.readInt()
        isMember = `in`.getBoolean()
        membersCount = `in`.readInt()
        memberStatus = `in`.readInt()
        communityType = `in`.readInt()
        photo50 = `in`.readString()
        photo100 = `in`.readString()
        photo200 = `in`.readString()
        verified = `in`.getBoolean()
        isBlacklisted = `in`.getBoolean()
    }

    internal constructor(`in`: ParcelNative) : super(`in`) {
        id = `in`.readInt()
        fullName = `in`.readString()
        screenName = `in`.readString()
        closed = `in`.readInt()
        isAdmin = `in`.readBoolean()
        adminLevel = `in`.readInt()
        isMember = `in`.readBoolean()
        membersCount = `in`.readInt()
        memberStatus = `in`.readInt()
        communityType = `in`.readInt()
        photo50 = `in`.readString()
        photo100 = `in`.readString()
        photo200 = `in`.readString()
        verified = `in`.readBoolean()
        isBlacklisted = `in`.readBoolean()
    }

    override val ownerId: Int
        get() = -abs(id)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(id)
        parcel.writeString(fullName)
        parcel.writeString(screenName)
        parcel.writeInt(closed)
        parcel.putBoolean(isAdmin)
        parcel.writeInt(adminLevel)
        parcel.putBoolean(isMember)
        parcel.writeInt(membersCount)
        parcel.writeInt(memberStatus)
        parcel.writeInt(communityType)
        parcel.writeString(photo50)
        parcel.writeString(photo100)
        parcel.writeString(photo200)
        parcel.putBoolean(verified)
        parcel.putBoolean(isBlacklisted)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        super.writeToParcelNative(dest)
        dest.writeInt(id)
        dest.writeString(fullName)
        dest.writeString(screenName)
        dest.writeInt(closed)
        dest.writeBoolean(isAdmin)
        dest.writeInt(adminLevel)
        dest.writeBoolean(isMember)
        dest.writeInt(membersCount)
        dest.writeInt(memberStatus)
        dest.writeInt(communityType)
        dest.writeString(photo50)
        dest.writeString(photo100)
        dest.writeString(photo200)
        dest.writeBoolean(verified)
        dest.writeBoolean(isBlacklisted)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_COMMUNITY
    }

    fun setName(name: String?): Community {
        fullName = name
        return this
    }

    fun setScreenName(screenName: String?): Community {
        this.screenName = screenName
        return this
    }

    fun setBlacklisted(isBlacklisted: Boolean): Community {
        this.isBlacklisted = isBlacklisted
        return this
    }

    fun setClosed(closed: Int): Community {
        this.closed = closed
        return this
    }

    fun setAdmin(admin: Boolean): Community {
        isAdmin = admin
        return this
    }

    fun setAdminLevel(adminLevel: Int): Community {
        this.adminLevel = adminLevel
        return this
    }

    fun setMember(member: Boolean): Community {
        isMember = member
        return this
    }

    fun setMembersCount(membersCount: Int): Community {
        this.membersCount = membersCount
        return this
    }

    fun setMemberStatus(memberStatus: Int): Community {
        this.memberStatus = memberStatus
        return this
    }

    fun setCommunityType(type: Int): Community {
        this.communityType = type
        return this
    }

    fun setPhoto50(photo50: String?): Community {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): Community {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): Community {
        this.photo200 = photo200
        return this
    }

    override fun get100photoOrSmaller(): String? {
        return firstNonEmptyString(photo100, photo50)
    }

    override val maxSquareAvatar: String?
        get() = firstNonEmptyString(photo200, photo100, photo50)
    override val originalAvatar: String?
        get() = maxSquareAvatar

    override fun describeContents(): Int {
        return 0
    }

    override val isDonated: Boolean
        get() = isValueAssigned(ownerId, CheckDonate.donatedOwnersLocal)

    override val isVerified: Boolean
        get() {
            return verified || isDonated
        }

    fun setVerified(verified: Boolean): Community {
        this.verified = verified
        return this
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Community> = object : Parcelable.Creator<Community> {
            override fun createFromParcel(`in`: Parcel): Community {
                return Community(`in`)
            }

            override fun newArray(size: Int): Array<Community?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<Community> =
            object : ParcelNative.Creator<Community> {
                override fun readFromParcelNative(dest: ParcelNative): Community {
                    return Community(dest)
                }

            }
    }
}