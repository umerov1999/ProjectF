package dev.ragnarok.filegallery.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PlayerCoverBackgroundSettings {
    @SerialName("enabled_rotation")
    var enabled_rotation = false

    @SerialName("invert_rotation")
    var invert_rotation = false

    @SerialName("fade_saturation")
    var fade_saturation = false

    @SerialName("rotation_speed")
    var rotation_speed = 0f

    @SerialName("zoom")
    var zoom = 0f

    @SerialName("blur")
    var blur = 0
    fun set_default(): PlayerCoverBackgroundSettings {
        enabled_rotation = true
        invert_rotation = false
        fade_saturation = true
        rotation_speed = 0.3f
        zoom = 1.2f
        blur = 16
        return this
    }
}