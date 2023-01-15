package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.model.interfaces.Copyable
import kotlinx.serialization.Serializable

@Serializable
class VKApiCopyFeedback : VKApiBaseFeedback() {
    var what: Copyable? = null
    var copies: Copies? = null
}