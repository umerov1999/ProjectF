package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.model.entity.FeedListEntity

object FeedListsColumns : BaseColumns {
    const val TABLENAME = "feed_sources"
    const val TITLE = "title"
    const val NO_REPOSTS = "no_reposts"
    const val SOURCE_IDS = "source_ids"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_TITLE = "$TABLENAME.$TITLE"
    const val FULL_NO_REPOSTS = "$TABLENAME.$NO_REPOSTS"
    const val FULL_SOURCE_IDS = "$TABLENAME.$SOURCE_IDS"

    fun getCV(entity: FeedListEntity): ContentValues {
        val cv = ContentValues()
        cv.put(BaseColumns._ID, entity.id)
        cv.put(TITLE, entity.title)
        cv.put(NO_REPOSTS, entity.isNoReposts)
        var sources: String? = null
        val ids = entity.sourceIds
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
        cv.put(SOURCE_IDS, sources)
        return cv
    }
}