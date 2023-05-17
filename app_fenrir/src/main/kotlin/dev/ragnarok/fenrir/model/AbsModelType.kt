package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    AbsModelType.MODEL_AUDIO,
    AbsModelType.MODEL_ARTICLE,
    AbsModelType.MODEL_AUDIO_ARTIST,
    AbsModelType.MODEL_AUDIO_PLAYLIST,
    AbsModelType.MODEL_CALL,
    AbsModelType.MODEL_CHAT,
    AbsModelType.MODEL_COMMENT,
    AbsModelType.MODEL_COMMUNITY,
    AbsModelType.MODEL_DOCUMENT,
    AbsModelType.MODEL_DOCUMENT_GRAFFITI,
    AbsModelType.MODEL_DOCUMENT_VIDEO_PREVIEW,
    AbsModelType.MODEL_EVENT,
    AbsModelType.MODEL_FAVE_LINK,
    AbsModelType.MODEL_FWDMESSAGES,
    AbsModelType.MODEL_GIFT,
    AbsModelType.MODEL_GIFT_ITEM,
    AbsModelType.MODEL_GRAFFITI,
    AbsModelType.MODEL_LINK,
    AbsModelType.MODEL_MARKET,
    AbsModelType.MODEL_MARKET_ALBUM,
    AbsModelType.MODEL_MESSAGE,
    AbsModelType.MODEL_NEWS,
    AbsModelType.MODEL_NOT_SUPPORTED,
    AbsModelType.MODEL_PHOTO,
    AbsModelType.MODEL_PHOTO_ALBUM,
    AbsModelType.MODEL_POLL,
    AbsModelType.MODEL_POLL_ANSWER,
    AbsModelType.MODEL_POLL_BACKGROUND,
    AbsModelType.MODEL_POLL_BACKGROUND_POINT,
    AbsModelType.MODEL_POST,
    AbsModelType.MODEL_SHORT_LINK,
    AbsModelType.MODEL_STICKER,
    AbsModelType.MODEL_STORY,
    AbsModelType.MODEL_TOPIC,
    AbsModelType.MODEL_USER,
    AbsModelType.MODEL_VIDEO,
    AbsModelType.MODEL_VIDEO_ALBUM,
    AbsModelType.MODEL_VOICE_MESSAGE,
    AbsModelType.MODEL_WALL_REPLY,
    AbsModelType.MODEL_WIKI_PAGE,
    AbsModelType.MODEL_AUDIO_CATALOG_V2_ARTIST,
    AbsModelType.MODEL_UPLOAD,
    AbsModelType.MODEL_GEO,
    AbsModelType.MODEL_CATALOG_V2_BLOCK,
    AbsModelType.MODEL_CATALOG_V2_LINK
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AbsModelType {
    companion object {
        const val MODEL_AUDIO = 0
        const val MODEL_ARTICLE = 1
        const val MODEL_AUDIO_ARTIST = 2
        const val MODEL_AUDIO_PLAYLIST = 3
        const val MODEL_CALL = 4
        const val MODEL_CHAT = 5
        const val MODEL_COMMENT = 6
        const val MODEL_COMMUNITY = 7
        const val MODEL_DOCUMENT = 8
        const val MODEL_DOCUMENT_GRAFFITI = 9
        const val MODEL_DOCUMENT_VIDEO_PREVIEW = 10
        const val MODEL_EVENT = 11
        const val MODEL_FAVE_LINK = 12
        const val MODEL_FWDMESSAGES = 13
        const val MODEL_GIFT = 14
        const val MODEL_GIFT_ITEM = 15
        const val MODEL_GRAFFITI = 16
        const val MODEL_LINK = 17
        const val MODEL_MARKET = 18
        const val MODEL_MARKET_ALBUM = 19
        const val MODEL_MESSAGE = 20
        const val MODEL_NEWS = 21
        const val MODEL_NOT_SUPPORTED = 22
        const val MODEL_PHOTO = 23
        const val MODEL_PHOTO_ALBUM = 24
        const val MODEL_POLL = 25
        const val MODEL_POLL_ANSWER = 26
        const val MODEL_POLL_BACKGROUND = 27
        const val MODEL_POLL_BACKGROUND_POINT = 28
        const val MODEL_POST = 29
        const val MODEL_SHORT_LINK = 30
        const val MODEL_STICKER = 31
        const val MODEL_STORY = 32
        const val MODEL_TOPIC = 33
        const val MODEL_USER = 34
        const val MODEL_VIDEO = 35
        const val MODEL_VIDEO_ALBUM = 36
        const val MODEL_VOICE_MESSAGE = 37
        const val MODEL_WALL_REPLY = 38
        const val MODEL_WIKI_PAGE = 39
        const val MODEL_AUDIO_CATALOG_V2_ARTIST = 40
        const val MODEL_UPLOAD = 41
        const val MODEL_GEO = 42
        const val MODEL_CATALOG_V2_BLOCK = 43
        const val MODEL_CATALOG_V2_LINK = 44
    }
}
