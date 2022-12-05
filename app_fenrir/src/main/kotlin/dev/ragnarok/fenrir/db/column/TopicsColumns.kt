package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.api.model.VKApiTopic
import dev.ragnarok.fenrir.orZero

object TopicsColumns : BaseColumns {
    const val TABLENAME = "topics"
    const val TOPIC_ID = "topic_id"
    const val OWNER_ID = "owner_id"
    const val TITLE = "title"
    const val CREATED = "created"
    const val CREATED_BY = "created_by"
    const val UPDATED = "updated"
    const val UPDATED_BY = "updated_by"
    const val IS_CLOSED = "is_closed"
    const val IS_FIXED = "is_fixed"
    const val COMMENTS = "comments"
    const val FIRST_COMMENT = "first_comment"
    const val LAST_COMMENT = "last_comment"
    const val ATTACHED_POLL = "attached_poll"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_TOPIC_ID = "$TABLENAME.$TOPIC_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_TITLE = "$TABLENAME.$TITLE"
    const val FULL_CREATED = "$TABLENAME.$CREATED"
    const val FULL_CREATED_BY = "$TABLENAME.$CREATED_BY"
    const val FULL_UPDATED = "$TABLENAME.$UPDATED"
    const val FULL_UPDATED_BY = "$TABLENAME.$UPDATED_BY"
    const val FULL_IS_CLOSED = "$TABLENAME.$IS_CLOSED"
    const val FULL_IS_FIXED = "$TABLENAME.$IS_FIXED"
    const val FULL_COMMENTS = "$TABLENAME.$COMMENTS"
    const val FULL_FIRST_COMMENT = "$TABLENAME.$FIRST_COMMENT"
    const val FULL_LAST_COMMENT = "$TABLENAME.$LAST_COMMENT"
    const val FULL_ATTACHED_POLL = "$TABLENAME.$ATTACHED_POLL"
    fun getCV(p: VKApiTopic): ContentValues {
        val cv = ContentValues()
        cv.put(TOPIC_ID, p.id)
        cv.put(OWNER_ID, p.owner_id)
        cv.put(TITLE, p.title)
        cv.put(CREATED, p.created)
        cv.put(CREATED_BY, p.created_by)
        cv.put(UPDATED, p.updated)
        cv.put(UPDATED_BY, p.updated_by)
        cv.put(IS_CLOSED, p.is_closed)
        cv.put(IS_FIXED, p.is_fixed)
        cv.put(COMMENTS, p.comments?.count.orZero())
        cv.put(FIRST_COMMENT, p.first_comment)
        cv.put(LAST_COMMENT, p.last_comment)
        return cv
    }
}