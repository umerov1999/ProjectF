package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class GroupSettings : Parcelable {
    private var title: String? = null
    private var description: String? = null
    private var address: String? = null
    private var access: Int = 0
    private var age: Int = 1
    private var category: IdOption? = null
    private var subcategory: IdOption? = null
    private var availableCategories: List<IdOption>?
    private var website: String? = null
    private var dateCreated: Day? = null
    private var feedbackCommentsEnabled = false
    private var obsceneFilterEnabled = false
    private var obsceneStopwordsEnabled = false
    private var obsceneWords: String? = null

    constructor() {
        availableCategories = emptyList()
    }

    internal constructor(`in`: Parcel) {
        title = `in`.readString()
        description = `in`.readString()
        address = `in`.readString()
        category = `in`.readTypedObjectCompat(IdOption.CREATOR)
        subcategory = `in`.readTypedObjectCompat(IdOption.CREATOR)
        availableCategories = `in`.createTypedArrayList(IdOption.CREATOR)
        website = `in`.readString()
        feedbackCommentsEnabled = `in`.getBoolean()
        obsceneFilterEnabled = `in`.getBoolean()
        obsceneStopwordsEnabled = `in`.getBoolean()
        obsceneWords = `in`.readString()
        access = `in`.readInt()
        age = `in`.readInt()
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

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): GroupSettings {
        this.title = title
        return this
    }

    fun getAge(): Int {
        return age
    }

    fun setAge(age: Int): GroupSettings {
        this.age = age
        return this
    }

    fun getAccess(): Int {
        return access
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

    fun getDescription(): String? {
        return description
    }

    fun setDescription(description: String?): GroupSettings {
        this.description = description
        return this
    }

    fun getAddress(): String? {
        return address
    }

    fun setAddress(address: String?): GroupSettings {
        this.address = address
        return this
    }

    fun getCategory(): IdOption? {
        return category
    }

    fun setCategory(category: IdOption?): GroupSettings {
        this.category = category
        return this
    }

    fun getAvailableCategories(): List<IdOption>? {
        return availableCategories
    }

    fun setAvailableCategories(availableCategories: List<IdOption>?): GroupSettings {
        this.availableCategories = availableCategories
        return this
    }

    fun getSubcategory(): IdOption? {
        return subcategory
    }

    fun setSubcategory(subcategory: IdOption?): GroupSettings {
        this.subcategory = subcategory
        return this
    }

    fun getWebsite(): String? {
        return website
    }

    fun setWebsite(website: String?): GroupSettings {
        this.website = website
        return this
    }

    fun isFeedbackCommentsEnabled(): Boolean {
        return feedbackCommentsEnabled
    }

    fun setFeedbackCommentsEnabled(feedbackCommentsEnabled: Boolean): GroupSettings {
        this.feedbackCommentsEnabled = feedbackCommentsEnabled
        return this
    }

    fun getDateCreated(): Day? {
        return dateCreated
    }

    fun setDateCreated(dateCreated: Day?): GroupSettings {
        this.dateCreated = dateCreated
        return this
    }

    fun isObsceneFilterEnabled(): Boolean {
        return obsceneFilterEnabled
    }

    fun setObsceneFilterEnabled(obsceneFilterEnabled: Boolean): GroupSettings {
        this.obsceneFilterEnabled = obsceneFilterEnabled
        return this
    }

    fun isObsceneStopwordsEnabled(): Boolean {
        return obsceneStopwordsEnabled
    }

    fun setObsceneStopwordsEnabled(obsceneStopwordsEnabled: Boolean): GroupSettings {
        this.obsceneStopwordsEnabled = obsceneStopwordsEnabled
        return this
    }

    fun getObsceneWords(): String? {
        return obsceneWords
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