package dev.ragnarok.fenrir.model

import android.util.SparseArray
import dev.ragnarok.fenrir.settings.Settings.get

class SparseArrayOwnersBundle(capacity: Int) : IOwnersBundle {
    private val data: SparseArray<Owner>
    override fun findById(id: Int): Owner? {
        return data[id]
    }

    override fun getById(id: Int): Owner {
        var owner = findById(id)
        if (owner == null) {
            owner = if (id > 0) {
                User(id)
            } else if (id < 0) {
                Community(-id)
            } else {
                User(
                    get().accounts().current
                )
                //throw new IllegalArgumentException("Zero owner id!!!");
            }
        }
        return owner
    }

    override fun size(): Int {
        return data.size()
    }

    override fun putAll(owners: Collection<Owner>) {
        for (owner in owners) {
            put(owner)
        }
    }

    override fun put(owner: Owner) {
        when (owner.ownerType) {
            OwnerType.USER -> data.put((owner as User).getObjectId(), owner)
            OwnerType.COMMUNITY -> data.put(-(owner as Community).id, owner)
        }
    }

    override fun getMissing(ids: Collection<Int>?): Collection<Int> {
        if (ids == null) {
            return emptyList()
        }
        val missing: MutableCollection<Int> = ArrayList()
        for (id in ids) {
            if (data[id] == null) {
                missing.add(id)
            }
        }
        return missing
    }

    init {
        data = SparseArray(capacity)
    }
}