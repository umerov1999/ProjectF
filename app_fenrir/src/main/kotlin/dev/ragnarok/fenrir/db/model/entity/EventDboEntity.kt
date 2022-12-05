package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("event")
class EventDboEntity : DboEntity() {
    var id = 0
        private set
    var button_text: String? = null
        private set
    var text: String? = null
        private set

    fun setId(id: Int): EventDboEntity {
        this.id = id
        return this
    }

    fun setText(text: String?): EventDboEntity {
        this.text = text
        return this
    }

    fun setButton_text(button_text: String?): EventDboEntity {
        this.button_text = button_text
        return this
    }
}