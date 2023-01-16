package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.settings.Settings.get

class SparseArrayOwnersBundle(capacity: Int) : IOwnersBundle {
    private val data: HashMap<Long, Owner>
    override fun findById(id: Long): Owner? {
        return data[id]
    }

    override fun getById(id: Long): Owner {
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
        return data.size
    }

    override fun putAll(owners: Collection<Owner>) {
        for (owner in owners) {
            put(owner)
        }
    }

    override fun put(owner: Owner) {
        when (owner.ownerType) {
            OwnerType.USER -> data[(owner as User).getOwnerObjectId()] = owner
            OwnerType.COMMUNITY -> data[-(owner as Community).id] = owner
        }
    }

    override fun getMissing(ids: Collection<Long>?): Collection<Long> {
        if (ids == null) {
            return emptyList()
        }
        val missing: MutableCollection<Long> = ArrayList()
        for (id in ids) {
            if (data[id] == null) {
                missing.add(id)
            }
        }
        return missing
    }

    init {
        data = HashMap(capacity)
    }
}