package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class VKApiPhotoTags {
    var id = 0
    var user_id = 0L
    var placer_id = 0
    var tagged_name: String? = null
    var date: Long = 0
    var x = 0.0
    var y = 0.0
    var x2 = 0.0
    var y2 = 0.0
    var viewed = 0
}