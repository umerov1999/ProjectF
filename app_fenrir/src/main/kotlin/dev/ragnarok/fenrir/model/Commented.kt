package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Commented : Parcelable {
    val sourceId: Int
    val sourceOwnerId: Long

    @CommentedType
    val sourceType: Int
    var accessKey: String?
        private set

    constructor(
        sourceId: Int,
        sourceOwnerId: Long,
        @CommentedType sourceType: Int,
        accessKey: String?
    ) {
        this.sourceId = sourceId
        this.sourceOwnerId = sourceOwnerId
        this.sourceType = sourceType
        this.accessKey = accessKey
    }

    internal constructor(parcel: Parcel) {
        sourceId = parcel.readInt()
        sourceOwnerId = parcel.readLong()
        sourceType = parcel.readInt()
        accessKey = parcel.readString()
    }

    fun setAccessKey(accessKey: String?): Commented {
        this.accessKey = accessKey
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val commented = other as Commented
        return sourceId == commented.sourceId && sourceOwnerId == commented.sourceOwnerId && sourceType == commented.sourceType
    }

    /**
     * Тип комментируемого обьекта
     * Используется в процедуре http://vk.com/dev/execute.getComments
     */
    val typeForStoredProcedure: String
        get() = when (sourceType) {
            CommentedType.POST -> "post"
            CommentedType.PHOTO -> "photo"
            CommentedType.VIDEO -> "video"
            CommentedType.TOPIC -> "topic"
            else -> throw IllegalArgumentException("Unknown source type: $sourceType")
        }

    override fun hashCode(): Int {
        var result = sourceId
        result = 31 * result + sourceOwnerId.hashCode()
        result = 31 * result + sourceType
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "Commented{" +
                "sourceId=" + sourceId +
                ", sourceOwnerId=" + sourceOwnerId +
                ", sourceType=" + sourceType +
                ", accessKey='" + accessKey + '\'' +
                '}'
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(sourceId)
        dest.writeLong(sourceOwnerId)
        dest.writeInt(sourceType)
        dest.writeString(accessKey)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Commented> = object : Parcelable.Creator<Commented> {
            override fun createFromParcel(source: Parcel): Commented {
                return Commented(source)
            }

            override fun newArray(size: Int): Array<Commented?> {
                return arrayOfNulls(size)
            }
        }

        fun from(model: AbsModel): Commented {
            return when (model) {
                is Post -> {
                    Commented(model.vkid, model.ownerId, CommentedType.POST, null)
                }
                is Photo -> {
                    Commented(
                        model.getObjectId(),
                        model.ownerId,
                        CommentedType.PHOTO,
                        model.accessKey
                    )
                }
                is Video -> {
                    Commented(
                        model.id,
                        model.ownerId,
                        CommentedType.VIDEO,
                        model.accessKey
                    )
                }
                is Topic -> {
                    Commented(
                        model.id,
                        model.ownerId,
                        CommentedType.TOPIC,
                        null
                    )
                }
                else -> {
                    throw IllegalArgumentException("Invalid model, class: " + model.javaClass)
                }
            }
        }
    }
}