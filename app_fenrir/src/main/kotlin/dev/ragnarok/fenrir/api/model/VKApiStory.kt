package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.StoryDtoAdapter
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

@Serializable(with = StoryDtoAdapter::class)
class VKApiStory : VKApiAttachment {
    /**
     * Note ID, positive number
     */
    var id = 0

    /**
     * Note owner ID.
     */
    var owner_id = 0L

    /**
     * Date (in Unix time) when the note was created.
     */
    var date: Long = 0
    var expires_at: Long = 0
    var is_expired = false
    var is_ads = false
    var access_key: String? = null
    var target_url: String? = null
    var parent_story: VKApiStory? = null
    var photo: VKApiPhoto? = null
    var video: VKApiVideo? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_STORY
    }
}