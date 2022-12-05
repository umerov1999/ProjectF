package dev.ragnarok.fenrir.activity

import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SelectProfileCriteria

interface ProfileSelectable {
    fun select(owner: Owner)
    val acceptableCriteria: SelectProfileCriteria?
}