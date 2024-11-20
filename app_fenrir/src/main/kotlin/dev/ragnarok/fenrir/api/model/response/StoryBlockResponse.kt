package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiStory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class StoryBlockResponse {
    @SerialName("stories")
    var stories: List<VKApiStory>? = null

    @SerialName("grouped")
    var grouped: List<StoryBlockResponse>? = null
}