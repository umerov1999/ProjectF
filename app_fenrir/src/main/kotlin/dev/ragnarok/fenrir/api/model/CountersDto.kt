package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CountersDto {
    @SerialName("friends")
    var friends = 0

    @SerialName("messages")
    var messages = 0

    @SerialName("photos")
    var photos = 0

    @SerialName("videos")
    var videos = 0

    @SerialName("notes")
    var notes = 0

    @SerialName("gifts")
    var gifts = 0

    @SerialName("events")
    var events = 0

    @SerialName("groups")
    var groups = 0

    @SerialName("notifications")
    var notifications = 0

    @SerialName("sdk")
    var sdk = 0

    @SerialName("app_requests")
    var app_requests = 0
}