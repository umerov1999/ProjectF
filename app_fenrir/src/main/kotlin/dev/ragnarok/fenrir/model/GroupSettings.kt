package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class GroupSettings : Parcelable {
    var title: String? = null
        private set
    var description: String? = null
        private set
    var address: String? = null
        private set
    var access: Int = 0
        private set
    var age: Int = 1
        private set
    var category: IdOption? = null
        private set
    var subcategory: IdOption? = null
        private set
    var availableCategories: List<IdOption>?
        private set
    var website: String? = null
        private set
    var dateCreated: Day? = null
        private set
    var feedbackCommentsEnabled = false
        private set
    var obsceneFilterEnabled = false
        private set
    var obsceneStopwordsEnabled = false
        private set
    var obsceneWords: String? = null
        private set

    constructor() {
        availableCategories = emptyList()
    }

    internal constructor(parcel: Parcel) {
        title = parcel.readString()
        description = parcel.readString()
        address = parcel.readString()
        category = parcel.readTypedObjectCompat(IdOption.CREATOR)
        subcategory = parcel.readTypedObjectCompat(IdOption.CREATOR)
        availableCategories = parcel.createTypedArrayList(IdOption.CREATOR)
        website = parcel.readString()
        feedbackCommentsEnabled = parcel.getBoolean()
        obsceneFilterEnabled = parcel.getBoolean()
        obsceneStopwordsEnabled = parcel.getBoolean()
        obsceneWords = parcel.readString()
        access = parcel.readInt()
        age = parcel.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(description)
        dest.writeString(address)
        dest.writeTypedObjectCompat(category, flags)
        dest.writeTypedObjectCompat(subcategory, flags)
        dest.writeTypedList(availableCategories)
        dest.writeString(website)
        dest.putBoolean(feedbackCommentsEnabled)
        dest.putBoolean(obsceneFilterEnabled)
        dest.putBoolean(obsceneStopwordsEnabled)
        dest.writeString(obsceneWords)
        dest.writeInt(access)
        dest.writeInt(age)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setTitle(title: String?): GroupSettings {
        this.title = title
        return this
    }

    fun setAge(age: Int): GroupSettings {
        this.age = age
        return this
    }

    fun setAccess(access: Int): GroupSettings {
        this.access = access
        return this
    }

    fun incAccess(): GroupSettings {
        access++
        if (access > 2) {
            access = 0
        }
        return this
    }

    fun setDescription(description: String?): GroupSettings {
        this.description = description
        return this
    }

    fun setAddress(address: String?): GroupSettings {
        this.address = address
        return this
    }

    fun setCategory(category: IdOption?): GroupSettings {
        this.category = category
        return this
    }

    fun setAvailableCategories(availableCategories: List<IdOption>?): GroupSettings {
        this.availableCategories = availableCategories
        return this
    }

    fun setSubcategory(subcategory: IdOption?): GroupSettings {
        this.subcategory = subcategory
        return this
    }

    fun setWebsite(website: String?): GroupSettings {
        this.website = website
        return this
    }

    fun setFeedbackCommentsEnabled(feedbackCommentsEnabled: Boolean): GroupSettings {
        this.feedbackCommentsEnabled = feedbackCommentsEnabled
        return this
    }

    fun setDateCreated(dateCreated: Day?): GroupSettings {
        this.dateCreated = dateCreated
        return this
    }

    fun setObsceneFilterEnabled(obsceneFilterEnabled: Boolean): GroupSettings {
        this.obsceneFilterEnabled = obsceneFilterEnabled
        return this
    }

    fun setObsceneStopwordsEnabled(obsceneStopwordsEnabled: Boolean): GroupSettings {
        this.obsceneStopwordsEnabled = obsceneStopwordsEnabled
        return this
    }

    fun setObsceneWords(obsceneWords: String?): GroupSettings {
        this.obsceneWords = obsceneWords
        return this
    }

    companion object CREATOR : Parcelable.Creator<GroupSettings> {
        override fun createFromParcel(parcel: Parcel): GroupSettings {
            return GroupSettings(parcel)
        }

        override fun newArray(size: Int): Array<GroupSettings?> {
            return arrayOfNulls(size)
        }
    }
}