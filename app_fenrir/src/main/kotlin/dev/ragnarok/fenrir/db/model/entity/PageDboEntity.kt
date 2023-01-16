package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("page")
class PageDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var creatorId = 0L
        private set
    var title: String? = null
        private set
    var source: String? = null
        private set
    var editionTime: Long = 0
        private set
    var creationTime: Long = 0
        private set
    var parent: String? = null
        private set
    var parent2: String? = null
        private set
    var views = 0
        private set
    var viewUrl: String? = null
        private set

    operator fun set(id: Int, ownerId: Long): PageDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setCreatorId(creatorId: Long): PageDboEntity {
        this.creatorId = creatorId
        return this
    }

    fun setTitle(title: String?): PageDboEntity {
        this.title = title
        return this
    }

    fun setSource(source: String?): PageDboEntity {
        this.source = source
        return this
    }

    fun setEditionTime(editionTime: Long): PageDboEntity {
        this.editionTime = editionTime
        return this
    }

    fun setCreationTime(creationTime: Long): PageDboEntity {
        this.creationTime = creationTime
        return this
    }

    fun setParent(parent: String?): PageDboEntity {
        this.parent = parent
        return this
    }

    fun setParent2(parent2: String?): PageDboEntity {
        this.parent2 = parent2
        return this
    }

    fun setViews(views: Int): PageDboEntity {
        this.views = views
        return this
    }

    fun setViewUrl(viewUrl: String?): PageDboEntity {
        this.viewUrl = viewUrl
        return this
    }
}