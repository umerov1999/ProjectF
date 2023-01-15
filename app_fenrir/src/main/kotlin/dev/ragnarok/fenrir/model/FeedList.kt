package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.api.model.interfaces.Identificable

class FeedList(private val id: Int, val title: String?) : Identificable {
    override fun getObjectId(): Int {
        return id
    }
}