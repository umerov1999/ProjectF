package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class KeyboardEntity {
    var one_time = false
        private set
    var inline = false
        private set

    var author_id = 0L
        private set

    var buttons: List<List<ButtonEntity>>? = null
        private set

    fun setOne_time(one_time: Boolean): KeyboardEntity {
        this.one_time = one_time
        return this
    }

    fun setInline(inline: Boolean): KeyboardEntity {
        this.inline = inline
        return this
    }

    fun setAuthor_id(author_id: Long): KeyboardEntity {
        this.author_id = author_id
        return this
    }

    fun setButtons(buttons: List<List<ButtonEntity>>?): KeyboardEntity {
        this.buttons = buttons
        return this
    }

    @Keep
    @Serializable
    class ButtonEntity {
        var color: String? = null
            private set

        var type: String? = null
            private set

        var label: String? = null
            private set

        var link: String? = null
            private set

        var payload: String? = null
            private set

        fun setColor(color: String?): ButtonEntity {
            this.color = color
            return this
        }

        fun setType(type: String?): ButtonEntity {
            this.type = type
            return this
        }

        fun setLabel(label: String?): ButtonEntity {
            this.label = label
            return this
        }

        fun setLink(link: String?): ButtonEntity {
            this.link = link
            return this
        }

        fun setPayload(payload: String?): ButtonEntity {
            this.payload = payload
            return this
        }
    }
}