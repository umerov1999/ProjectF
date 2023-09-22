package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("narrative")
class NarrativesDboEntity : DboEntity() {
    var id: Int = 0
    var owner_id: Long = 0L
    var accessKey: String? = null
        private set
    var title: String? = null
        private set
    var cover: String? = null
        private set
    var stories: IntArray? = null
        private set

    fun setId(id: Int): NarrativesDboEntity {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Long): NarrativesDboEntity {
        owner_id = ownerId
        return this
    }

    fun setAccessKey(access_key: String?): NarrativesDboEntity {
        accessKey = access_key
        return this
    }

    fun setTitle(title: String?): NarrativesDboEntity {
        this.title = title
        return this
    }

    fun setCover(cover: String?): NarrativesDboEntity {
        this.cover = cover
        return this
    }

    fun setStory_ids(stories: IntArray?): NarrativesDboEntity {
        this.stories = stories
        return this
    }
}
