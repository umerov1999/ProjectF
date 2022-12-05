package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("gift_item")
class GiftItemDboEntity : DboEntity() {
    var id = 0
        private set
    var thumb256: String? = null
        private set
    var thumb96: String? = null
        private set
    var thumb48: String? = null
        private set

    fun setId(id: Int): GiftItemDboEntity {
        this.id = id
        return this
    }

    fun setThumb256(thumb256: String?): GiftItemDboEntity {
        this.thumb256 = thumb256
        return this
    }

    fun setThumb96(thumb96: String?): GiftItemDboEntity {
        this.thumb96 = thumb96
        return this
    }

    fun setThumb48(thumb48: String?): GiftItemDboEntity {
        this.thumb48 = thumb48
        return this
    }
}