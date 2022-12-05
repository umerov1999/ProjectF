package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.AttachmentsDtoAdapter
import dev.ragnarok.fenrir.api.adapters.AttachmentsEntryDtoAdapter
import kotlinx.serialization.Serializable
import java.util.*

@Serializable(with = AttachmentsDtoAdapter::class)
class VKApiAttachments {
    val entries: ArrayList<Entry>

    constructor() {
        entries = ArrayList(1)
    }

    constructor(initialSize: Int) {
        entries = ArrayList(initialSize)
    }

    fun entryList(): List<Entry> {
        return Collections.unmodifiableList(entries)
    }

    fun append(attachment: VKApiAttachment) {
        entries.add(Entry(attachment.getType(), attachment))
    }

    fun clear() {
        entries.clear()
    }

    fun append(data: List<VKApiAttachment>) {
        for (attachment in data) {
            append(attachment)
        }
    }

    val isEmpty: Boolean
        get() = size() <= 0

    fun nonEmpty(): Boolean {
        return size() > 0
    }

    fun size(): Int {
        return entries.size
    }

    @Serializable(with = AttachmentsEntryDtoAdapter::class)
    class Entry(val type: String, val attachment: VKApiAttachment)
}