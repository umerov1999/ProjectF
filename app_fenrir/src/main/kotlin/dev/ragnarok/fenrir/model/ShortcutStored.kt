package dev.ragnarok.fenrir.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class ShortcutStored {
    lateinit var action: String
        private set
    lateinit var cover: String
        private set
    lateinit var name: String
        private set

    fun setAction(action: String): ShortcutStored {
        this.action = action
        return this
    }

    fun setCover(url: String): ShortcutStored {
        this.cover = url
        return this
    }

    fun setName(name: String): ShortcutStored {
        this.name = name
        return this
    }
}