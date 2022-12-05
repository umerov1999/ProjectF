package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadChatPhotoResponse {
    @SerialName("message_id")
    var message_id = 0

    @SerialName("chat")
    var chat: ChatPhoto? = null

    @Serializable
    class ChatPhoto {
        @SerialName("photo_50")
        var photo_50: String? = null

        @SerialName("photo_100")
        var photo_100: String? = null

        @SerialName("photo_200")
        var photo_200: String? = null
    }
}