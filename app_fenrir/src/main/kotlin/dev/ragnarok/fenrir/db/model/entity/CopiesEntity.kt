package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import dev.ragnarok.fenrir.db.model.IdPairEntity
import kotlinx.serialization.Serializable

@Keep
@Serializable
class CopiesEntity {
    var count = 0
        private set

    var pairDbos: ArrayList<IdPairEntity>? = null
        private set

    fun setCount(count: Int): CopiesEntity {
        this.count = count
        return this
    }

    fun setPairDbos(pairDbos: ArrayList<IdPairEntity>?): CopiesEntity {
        this.pairDbos = pairDbos
        return this
    }
}