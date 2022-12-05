package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.PrivacyDtoAdapter
import dev.ragnarok.fenrir.api.util.VKStringUtils.join
import kotlinx.serialization.Serializable

@Serializable(with = PrivacyDtoAdapter::class)
class VKApiPrivacy(var category: String?) {
    var entries: ArrayList<Entry> = ArrayList()
    private fun putIfNotExist(entry: Entry) {
        if (!entries.contains(entry)) {
            entries.add(entry)
        }
    }

    fun buildJsonArray(): String {
        val typeValue = category
        val entriesLine = join(", ", entries)
        val option = ArrayList<String?>()
        if (typeValue != null && typeValue.isNotEmpty()) {
            option.add(typeValue)
        }
        if (entriesLine.isNotEmpty()) {
            option.add(entriesLine)
        }
        return join(", ", option)
    }

    override fun toString(): String {
        return buildJsonArray()
    }

    fun includeOwner(id: Int) {
        putIfNotExist(Entry.includedOwner(id))
    }

    fun excludeOwner(id: Int) {
        putIfNotExist(Entry.excludedOwner(id))
    }

    fun includeFriendsList(id: Int) {
        putIfNotExist(Entry.includedFriendsList(id))
    }

    fun excludeFriendsList(id: Int) {
        putIfNotExist(Entry.excludedFriendsList(id))
    }

    @Serializable
    class Entry(var type: Int, var id: Int, var allowed: Boolean) {
        override fun toString(): String {
            return when (type) {
                TYPE_FRIENDS_LIST -> if (allowed) "list$id" else "-list$id"
                TYPE_OWNER -> if (allowed) id.toString() else (-id).toString()
                else -> throw IllegalStateException("Unknown type")
            }
        }

        companion object {
            const val TYPE_OWNER = 1
            const val TYPE_FRIENDS_LIST = 2
            fun excludedOwner(id: Int): Entry {
                return Entry(TYPE_OWNER, id, false)
            }

            fun includedOwner(id: Int): Entry {
                return Entry(TYPE_OWNER, id, true)
            }

            fun includedFriendsList(id: Int): Entry {
                return Entry(TYPE_FRIENDS_LIST, id, true)
            }

            fun excludedFriendsList(id: Int): Entry {
                return Entry(TYPE_FRIENDS_LIST, id, false)
            }
        }
    }

}