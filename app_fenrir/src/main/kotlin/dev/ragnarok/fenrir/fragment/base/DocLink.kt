package dev.ragnarok.fenrir.fragment.base

import android.content.Context
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.model.AttachmentsTypes
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.AudioArtist
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.Call
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Event
import dev.ragnarok.fenrir.model.Geo
import dev.ragnarok.fenrir.model.Graffiti
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.NotSupported
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.WallReply
import dev.ragnarok.fenrir.model.WikiPage
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import java.util.Calendar
import java.util.Locale

class DocLink(val attachment: AbsModel) {
    @AttachmentsTypes
    val type: Int = typeOf(attachment)
    val imageUrl: String?
        get() {
            when (type) {
                AttachmentsTypes.DOC -> {
                    val doc = attachment as Document
                    return doc.getPreviewWithSize(Settings.get().main().prefPreviewImageSize, true)
                }

                AttachmentsTypes.POST -> return (attachment as Post).authorPhoto
                AttachmentsTypes.EVENT -> return (attachment as Event).subjectPhoto
                AttachmentsTypes.WALL_REPLY -> return (attachment as WallReply).authorPhoto
                AttachmentsTypes.GRAFFITI -> return (attachment as Graffiti).url
                AttachmentsTypes.STORY -> return (attachment as Story).owner?.maxSquareAvatar
                AttachmentsTypes.ALBUM -> {
                    val album = attachment as PhotoAlbum
                    return album.getSizes()?.getUrlForSize(
                        Settings.get().main().prefPreviewImageSize,
                        true
                    )
                }

                AttachmentsTypes.MARKET_ALBUM -> {
                    val market_album = attachment as MarketAlbum
                    return market_album.getPhoto()?.sizes?.getUrlForSize(
                        Settings.get().main().prefPreviewImageSize, true
                    )
                }

                AttachmentsTypes.ARTIST -> return (attachment as AudioArtist).getMaxPhoto()
                AttachmentsTypes.MARKET -> return (attachment as Market).thumb_photo
                AttachmentsTypes.AUDIO_PLAYLIST -> return (attachment as AudioPlaylist).getThumb_image()
                AttachmentsTypes.LINK -> {
                    val link = attachment as Link
                    if (link.photo == null && link.previewPhoto != null) return link.previewPhoto
                    if (link.photo != null && link.photo?.sizes != null) {
                        val sizes = link.photo?.sizes
                        return sizes?.getUrlForSize(
                            Settings.get().main().prefPreviewImageSize,
                            true
                        )
                    }
                    return null
                }
            }
            return null
        }

    fun getTitle(context: Context): String? {
        var title: String?
        when (type) {
            AttachmentsTypes.DOC -> return (attachment as Document).title
            AttachmentsTypes.POST -> return (attachment as Post).authorName
            AttachmentsTypes.EVENT -> return (attachment as Event).subjectName
            AttachmentsTypes.WALL_REPLY -> return (attachment as WallReply).authorName
            AttachmentsTypes.AUDIO_PLAYLIST -> return (attachment as AudioPlaylist).getTitle()
            AttachmentsTypes.ALBUM -> return (attachment as PhotoAlbum).getTitle()
            AttachmentsTypes.MARKET -> return (attachment as Market).title
            AttachmentsTypes.ARTIST -> return (attachment as AudioArtist).getName()
            AttachmentsTypes.MARKET_ALBUM -> return (attachment as MarketAlbum).getTitle()
            AttachmentsTypes.LINK -> {
                title = (attachment as Link).title
                if (title.isNullOrEmpty()) {
                    title = "[" + context.getString(R.string.attachment_link)
                        .lowercase(Locale.getDefault()) + "]"
                }
                return title
            }

            AttachmentsTypes.NOT_SUPPORTED -> return context.getString(R.string.not_yet_implemented_message)
            AttachmentsTypes.POLL -> {
                val poll = attachment as Poll
                return context.getString(if (poll.isAnonymous) R.string.anonymous_poll else R.string.open_poll)
            }

            AttachmentsTypes.STORY -> return (attachment as Story).owner?.fullName
            AttachmentsTypes.WIKI_PAGE -> return context.getString(R.string.wiki_page)
            AttachmentsTypes.CALL -> {
                val initiator = (attachment as Call).initiator_id
                return if (initiator == Settings.get()
                        .accounts().current
                ) context.getString(R.string.input_call) else context.getString(R.string.output_call)
            }

            AttachmentsTypes.GEO -> {
                return (attachment as Geo).title
            }
        }
        return null
    }

    fun getExt(context: Context): String? {
        when (type) {
            AttachmentsTypes.DOC -> return (attachment as Document).ext
            AttachmentsTypes.POST, AttachmentsTypes.WALL_REPLY -> return null
            AttachmentsTypes.LINK -> return URL
            AttachmentsTypes.WIKI_PAGE -> return W
            AttachmentsTypes.STORY -> return context.getString(R.string.story)
            AttachmentsTypes.AUDIO_PLAYLIST -> return context.getString(R.string.playlist)
        }
        return null
    }

    fun getSecondaryText(context: Context): String? {
        when (type) {
            AttachmentsTypes.DOC -> return AppTextUtils.getSizeString(
                (attachment as Document).size
            )

            AttachmentsTypes.NOT_SUPPORTED -> {
                val ns = attachment as NotSupported
                return ns.getType() + ": " + ns.getBody()
            }

            AttachmentsTypes.POST -> {
                val post = attachment as Post
                return when {
                    post.hasText() -> post.text
                    post.hasAttachments() -> ""
                    else -> context.getString(
                        R.string.wall_post_view
                    )
                }
            }

            AttachmentsTypes.EVENT -> {
                val event = attachment as Event
                return Utils.firstNonEmptyString(
                    event.button_text,
                    " "
                ) + ", " + Utils.firstNonEmptyString(event.text)
            }

            AttachmentsTypes.WALL_REPLY -> {
                val comment = attachment as WallReply
                return comment.text
            }

            AttachmentsTypes.LINK -> return (attachment as Link).url
            AttachmentsTypes.ALBUM -> return Utils.firstNonEmptyString(
                (attachment as PhotoAlbum).getDescription(),
                " "
            ) +
                    " " + context.getString(R.string.photos_count, attachment.getSize())

            AttachmentsTypes.POLL -> return (attachment as Poll).question
            AttachmentsTypes.WIKI_PAGE -> return (attachment as WikiPage).title
            AttachmentsTypes.CALL -> return (attachment as Call).getLocalizedState(context)
            AttachmentsTypes.MARKET -> return (attachment as Market).price + ", " + AppTextUtils.reduceStringForPost(
                Utils.firstNonEmptyString(
                    attachment.description, " "
                )
            )

            AttachmentsTypes.MARKET_ALBUM -> return context.getString(
                R.string.markets_count,
                (attachment as MarketAlbum).getCount()
            )

            AttachmentsTypes.AUDIO_PLAYLIST -> return Utils.firstNonEmptyString(
                (attachment as AudioPlaylist).getArtist_name(),
                " "
            ) + " " +
                    attachment.getCount() + " " + context.getString(R.string.audios_pattern_count)

            AttachmentsTypes.STORY -> {
                val item = attachment as Story
                return if (item.expires <= 0) null else {
                    if (item.isIs_expired) {
                        context.getString(R.string.is_expired)
                    } else {
                        val exp = (item.expires - Calendar.getInstance().time.time / 1000) / 3600
                        context.getString(
                            R.string.expires,
                            exp.toString(),
                            context.getString(
                                Utils.declOfNum(
                                    exp,
                                    intArrayOf(R.string.hour, R.string.hour_sec, R.string.hours)
                                )
                            )
                        )
                    }
                }
            }

            AttachmentsTypes.GEO -> {
                val geo = attachment as Geo
                return geo.latitude.orEmpty() + " " + geo.longitude.orEmpty() + "\r\n" + geo.address.orEmpty()
            }
        }
        return null
    }

    companion object {
        private const val URL = "URL"
        private const val W = "WIKI"

        @AttachmentsTypes
        internal fun typeOf(model: AbsModel): Int {
            when (model.getModelType()) {
                AbsModelType.MODEL_DOCUMENT -> {
                    return AttachmentsTypes.DOC
                }

                AbsModelType.MODEL_POST -> {
                    return AttachmentsTypes.POST
                }

                AbsModelType.MODEL_LINK -> {
                    return AttachmentsTypes.LINK
                }

                AbsModelType.MODEL_POLL -> {
                    return AttachmentsTypes.POLL
                }

                AbsModelType.MODEL_WIKI_PAGE -> {
                    return AttachmentsTypes.WIKI_PAGE
                }

                AbsModelType.MODEL_STORY -> {
                    return AttachmentsTypes.STORY
                }

                AbsModelType.MODEL_CALL -> {
                    return AttachmentsTypes.CALL
                }

                AbsModelType.MODEL_GEO -> {
                    return AttachmentsTypes.GEO
                }

                AbsModelType.MODEL_AUDIO_ARTIST -> {
                    return AttachmentsTypes.ARTIST
                }

                AbsModelType.MODEL_WALL_REPLY -> {
                    return AttachmentsTypes.WALL_REPLY
                }

                AbsModelType.MODEL_AUDIO_PLAYLIST -> {
                    return AttachmentsTypes.AUDIO_PLAYLIST
                }

                AbsModelType.MODEL_GRAFFITI -> {
                    return AttachmentsTypes.GRAFFITI
                }

                AbsModelType.MODEL_PHOTO_ALBUM -> {
                    return AttachmentsTypes.ALBUM
                }

                AbsModelType.MODEL_NOT_SUPPORTED -> {
                    return AttachmentsTypes.NOT_SUPPORTED
                }

                AbsModelType.MODEL_EVENT -> {
                    return AttachmentsTypes.EVENT
                }

                AbsModelType.MODEL_MARKET -> {
                    return AttachmentsTypes.MARKET
                }

                AbsModelType.MODEL_MARKET_ALBUM -> {
                    return AttachmentsTypes.MARKET_ALBUM
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

}