package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class CountryDboEntity {
    var id = 0
        private set
    var title: String? = null
        private set

    operator fun set(id: Int, title: String?): CountryDboEntity {
        this.id = id
        this.title = title
        return this
    }
}