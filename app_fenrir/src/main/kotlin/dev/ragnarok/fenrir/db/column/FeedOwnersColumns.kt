package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.model.entity.FeedOwnersEntity

object FeedOwnersColumns : BaseColumns {
    const val TABLENAME = "feed_owners"
    const val TITLE = "title"
    const val OWNERS_IDS = "owners_ids"

    fun getCV(entity: FeedOwnersEntity): ContentValues {
        val cv = ContentValues()
        cv.put(BaseColumns._ID, entity.id)
        cv.put(TITLE, entity.title)
        var sources: String? = null
        val ids = entity.ownersIds
        if (ids != null) {
            val builder = StringBuilder()
            for (i in ids.indices) {
                builder.append(ids[i])
                if (i != ids.size - 1) {
                    builder.append(",")
                }
            }
            sources = builder.toString()
        }
        cv.put(OWNERS_IDS, sources)
        return cv
    }
}