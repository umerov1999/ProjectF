package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object AudioColumns : BaseColumns {
    const val TABLENAME = "audio_cache_data"

    const val SOURCE_OWNER_ID = "source_owner_id"
    const val AUDIO_ID = "audio_id"
    const val AUDIO_OWNER_ID = "audio_owner_id"
    const val ARTIST = "artist"
    const val TITLE = "title"
    const val DURATION = "duration"
    const val URL = "url"
    const val LYRICS_ID = "lyricsId"
    const val DATE = "date"
    const val ALBUM_ID = "albumId"
    const val ALBUM_OWNER_ID = "album_owner_id"
    const val ALBUM_ACCESS_KEY = "album_access_key"
    const val GENRE = "genre"
    const val DELETED = "deleted"
    const val ACCESS_KEY = "access_key"
    const val THUMB_IMAGE_BIG = "thumb_image_big"
    const val THUMB_IMAGE_VERY_BIG = "thumb_image_very_big"
    const val THUMB_IMAGE_LITTLE = "thumb_image_little"
    const val ALBUM_TITLE = "album_title"
    const val MAIN_ARTISTS = "main_artists"
    const val IS_HQ = "is_hq"
}
