package dev.ragnarok.filegallery.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SlidrSettings {
    @SerialName("vertical_sensitive")
    var vertical_sensitive = 0f

    @SerialName("horizontal_sensitive")
    var horizontal_sensitive = 0f

    @SerialName("vertical_velocity_threshold")
    var vertical_velocity_threshold = 0f

    @SerialName("horizontal_velocity_threshold")
    var horizontal_velocity_threshold = 0f

    @SerialName("vertical_distance_threshold")
    var vertical_distance_threshold = 0f

    @SerialName("horizontal_distance_threshold")
    var horizontal_distance_threshold = 0f
    fun set_default(): SlidrSettings {
        horizontal_sensitive = 0.5f
        vertical_sensitive = 0.7f
        vertical_velocity_threshold = 5f
        horizontal_velocity_threshold = 5f
        vertical_distance_threshold = 0.11f
        horizontal_distance_threshold = 0.25f
        return this
    }
}