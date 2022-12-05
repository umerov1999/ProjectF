package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.NewsfeedCommentDtoAdapter
import dev.ragnarok.fenrir.api.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NewsfeedCommentsResponse {
    @SerialName("items")
    var items: List<Dto>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null

    @SerialName("next_from")
    var nextFrom: String? = null

    @Serializable(with = NewsfeedCommentDtoAdapter::class)
    abstract class Dto

    @Serializable
    class PostDto(val post: VKApiPost?) : Dto()

    @Serializable
    class PhotoDto(val photo: VKApiPhoto?) : Dto()

    @Serializable
    class VideoDto(val video: VKApiVideo?) : Dto()

    @Serializable
    class TopicDto(val topic: VKApiTopic?) : Dto()
}