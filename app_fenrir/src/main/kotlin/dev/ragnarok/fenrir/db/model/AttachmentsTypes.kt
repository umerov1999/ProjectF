package dev.ragnarok.fenrir.db.model

import androidx.annotation.IntDef

@IntDef(
    AttachmentsTypes.PHOTO,
    AttachmentsTypes.VIDEO,
    AttachmentsTypes.AUDIO,
    AttachmentsTypes.DOC,
    AttachmentsTypes.POST,
    AttachmentsTypes.ARTICLE,
    AttachmentsTypes.LINK,
    AttachmentsTypes.STORY,
    AttachmentsTypes.NARRATIVE,
    AttachmentsTypes.CALL,
    AttachmentsTypes.POLL,
    AttachmentsTypes.PAGE,
    AttachmentsTypes.AUDIO_PLAYLIST,
    AttachmentsTypes.STICKER,
    AttachmentsTypes.TOPIC,
    AttachmentsTypes.AUDIO_MESSAGE,
    AttachmentsTypes.GIFT,
    AttachmentsTypes.GRAFFITI,
    AttachmentsTypes.ALBUM,
    AttachmentsTypes.NOT_SUPPORTED,
    AttachmentsTypes.WALL_REPLY,
    AttachmentsTypes.EVENT,
    AttachmentsTypes.MARKET,
    AttachmentsTypes.MARKET_ALBUM,
    AttachmentsTypes.ARTIST,
    AttachmentsTypes.WIKI_PAGE,
    AttachmentsTypes.GEO
)
@Retention(AnnotationRetention.SOURCE)
annotation class AttachmentsTypes {
    companion object {
        const val PHOTO = 1
        const val VIDEO = 2
        const val AUDIO = 3
        const val DOC = 4
        const val POST = 5
        const val ARTICLE = 6
        const val LINK = 7
        const val STORY = 8
        const val NARRATIVE = 9
        const val CALL = 10
        const val POLL = 11
        const val PAGE = 12
        const val AUDIO_PLAYLIST = 13
        const val STICKER = 14
        const val TOPIC = 15
        const val AUDIO_MESSAGE = 16
        const val GIFT = 17
        const val GRAFFITI = 18
        const val ALBUM = 19
        const val NOT_SUPPORTED = 20
        const val WALL_REPLY = 21
        const val EVENT = 22
        const val MARKET = 23
        const val MARKET_ALBUM = 24
        const val ARTIST = 25
        const val WIKI_PAGE = 26
        const val GEO = 27
    }
}