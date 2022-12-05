package dev.ragnarok.fenrir.db.model.entity.feedback

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.entity.CopiesEntity
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("copy")
class CopyEntity : FeedbackEntity {
    var copies: CopiesEntity? = null
        private set

    private var copied: DboEntity? = null

    @Suppress("UNUSED")
    constructor()
    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    fun setCopies(copies: CopiesEntity?): CopyEntity {
        this.copies = copies
        return this
    }

    fun getCopied(): DboEntity? {
        return copied
    }

    fun setCopied(copied: DboEntity?): CopyEntity {
        this.copied = copied
        return this
    }
}