package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum

object VideoAlbumsColumns : BaseColumns {
    const val TABLENAME = "video_albums"
    const val ALBUM_ID = "album_id"
    const val OWNER_ID = "owner_id"
    const val TITLE = "title"
    const val IMAGE = "image"
    const val COUNT = "count"
    const val UPDATE_TIME = "update_time"
    const val PRIVACY = "privacy"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_ALBUM_ID = "$TABLENAME.$ALBUM_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_TITLE = "$TABLENAME.$TITLE"
    const val FULL_IMAGE = "$TABLENAME.$IMAGE"
    const val FULL_COUNT = "$TABLENAME.$COUNT"
    const val FULL_UPDATE_TIME = "$TABLENAME.$UPDATE_TIME"
    const val FULL_PRIVACY = "$TABLENAME.$PRIVACY"
    fun getCV(p: VKApiVideoAlbum): ContentValues {
        val cv = ContentValues()
        cv.put(OWNER_ID, p.owner_id)
        cv.put(ALBUM_ID, p.id)
        cv.put(TITLE, p.title)
        cv.put(IMAGE, p.image)
        cv.put(COUNT, p.count)
        cv.put(UPDATE_TIME, p.updated_time)
        cv.put(PRIVACY, if (p.privacy == null) null else p.privacy.toString())
        return cv
    }
}