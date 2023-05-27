package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef
import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Keep
@Serializable
class FeedbackVKOfficial {
    var footer: String? = null
    var header: String? = null
    var header_owner_avatar_url: String? = null
    var header_owner_id: Long? = null
    var text: String? = null
    var iconURL: String? = null
    var iconType: String? = null
    var hide_query: String? = null
    var time: Long = 0
    var images: ArrayList<ImageAdditional>? = null
    var images_action: Action? = null
    var attachments: ArrayList<Photo>? = null
    var action: Action? = null
    fun getImage(prefSize: Int): ImageAdditional? {
        var result: ImageAdditional? = null
        if (images.isNullOrEmpty()) return null
        for (image in images.orEmpty()) {
            if (result == null) {
                result = image
                continue
            }
            if (abs(image.calcAverageSize() - prefSize) < abs(result.calcAverageSize() - prefSize)) {
                result = image
            }
        }
        return result
    }

    @IntDef(Action_Types.MESSAGE, Action_Types.BROWSER_URL, Action_Types.URL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Action_Types {
        companion object {
            const val MESSAGE = 0
            const val BROWSER_URL = 1
            const val URL = 2
        }
    }

    @Keep
    @Serializable
    sealed class Action {
        @Action_Types
        abstract fun getActionType(): Int
    }

    @Keep
    @Serializable
    @SerialName("action_message")
    class ActionMessage(private val peerId: Long, private val messageId: Int) : Action() {
        fun getPeerId(): Long {
            return peerId
        }

        fun getMessageId(): Int {
            return messageId
        }

        override fun getActionType(): Int {
            return Action_Types.MESSAGE
        }
    }

    @Keep
    @Serializable
    @SerialName("action_url")
    class ActionURL(private val url: String?) : Action() {
        fun getUrl(): String? {
            return url
        }

        override fun getActionType(): Int {
            return Action_Types.URL
        }
    }

    @Keep
    @Serializable
    @SerialName("action_browser_url")
    class ActionBrowserURL(private val url: String?) : Action() {
        fun getUrl(): String? {
            return url
        }

        override fun getActionType(): Int {
            return Action_Types.BROWSER_URL
        }
    }

    @Keep
    @Serializable
    class ImageAdditional {
        @SerialName("url")
        var url: String? = null

        @SerialName("width")
        var width = 0

        @SerialName("height")
        var height = 0
        fun calcAverageSize(): Int {
            return (width + height) / 2
        }
    }

    @Keep
    @Serializable
    class Attachment {
        @SerialName("type")
        var type: String? = null

        @SerialName("object_id")
        var object_id: String? = null
    }
}