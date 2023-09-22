package dev.ragnarok.fenrir.api.model.interfaces

/**
 * An abstract class for all attachments
 */
interface VKApiAttachment {
    /**
     * @return type of this attachment
     */
    fun getType(): String

    companion object {
        const val TYPE_PHOTO = "photo"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
        const val TYPE_DOC = "doc"
        const val TYPE_POST = "wall"
        const val TYPE_FAVE_POST = "post"
        const val TYPE_WALL_REPLY = "wall_reply"
        const val TYPE_LINK = "link"
        const val TYPE_ARTICLE = "article"
        const val TYPE_STORY = "story"
        const val TYPE_NARRATIVE = "narrative"
        const val TYPE_CALL = "call"
        const val TYPE_NOT_SUPPORT = "not_support"
        const val TYPE_NOTE = "note"
        const val TYPE_POLL = "poll"
        const val TYPE_EVENT = "event"
        const val TYPE_WIKI_PAGE = "page"
        const val TYPE_ALBUM = "album"
        const val TYPE_STICKER = "sticker"
        const val TYPE_AUDIO_MESSAGE = "audio_message"
        const val TYPE_GIFT = "gift"
        const val TYPE_GRAFFITI = "graffiti"
        const val TYPE_AUDIO_PLAYLIST = "audio_playlist"
        const val TYPE_MARKET = "market"
        const val TYPE_PRODUCT = "product"
        const val TYPE_MARKET_ALBUM = "market_album"
        const val TYPE_ARTIST = "artist"
        const val TYPE_GEO = "geo"
        val IGNORE_ATTACHMENTS =
            arrayOf("mini_app", "photos_list", "podcast", "pretty_cards", "situational_theme")
    }
}