package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import android.util.ArrayMap
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2ArtistItem
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Link
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2RecommendationPlaylist
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.writeTypedObjectCompat

class ParcelableModelWrapper : Parcelable {
    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableModelWrapper> =
            object : Parcelable.Creator<ParcelableModelWrapper> {
                override fun createFromParcel(parcel: Parcel): ParcelableModelWrapper {
                    return ParcelableModelWrapper(parcel)
                }

                override fun newArray(size: Int): Array<ParcelableModelWrapper?> {
                    return arrayOfNulls(size)
                }
            }
        private val TYPES: ArrayMap<Int, (parcel: Parcel) -> AbsModel?> = ArrayMap(46)

        fun wrap(model: AbsModel): ParcelableModelWrapper {
            return ParcelableModelWrapper(model)
        }

        fun readModel(parcel: Parcel): AbsModel? {
            val ex = parcel.getBoolean()
            if (!ex) {
                return null
            }
            return parcel.readTypedObjectCompat(CREATOR)
                ?.get()
        }

        fun writeModel(dest: Parcel, flags: Int, owner: AbsModel?) {
            if (owner == null) {
                dest.putBoolean(false)
            } else {
                dest.putBoolean(true)
                dest.writeTypedObjectCompat(ParcelableModelWrapper(owner), flags)
            }
        }

        fun writeModels(dest: Parcel, flags: Int, list: ArrayList<AbsModel>?) {
            if (list == null) {
                dest.putBoolean(false)
            } else {
                dest.putBoolean(true)
                dest.writeInt(list.size)
                for (i in list) {
                    dest.writeTypedObjectCompat(ParcelableModelWrapper(i), flags)
                }
            }
        }

        fun readModels(parcel: Parcel): ArrayList<AbsModel>? {
            val ex = parcel.getBoolean()
            if (!ex) {
                return null
            }
            val sz = parcel.readInt()
            val ret: ArrayList<AbsModel> = ArrayList(sz)
            for (i in 0 until sz) {
                ret.add((parcel.readTypedObjectCompat(CREATOR) ?: return null).get())
            }
            return ret
        }

        init {
            TYPES[AbsModelType.MODEL_AUDIO] = { it.readTypedObjectCompat(Audio.CREATOR) }
            TYPES[AbsModelType.MODEL_ARTICLE] = { it.readTypedObjectCompat(Article.CREATOR) }
            TYPES[AbsModelType.MODEL_AUDIO_ARTIST] =
                { it.readTypedObjectCompat(AudioArtist.CREATOR) }
            TYPES[AbsModelType.MODEL_AUDIO_PLAYLIST] =
                { it.readTypedObjectCompat(AudioPlaylist.CREATOR) }
            TYPES[AbsModelType.MODEL_CALL] = { it.readTypedObjectCompat(Call.CREATOR) }
            TYPES[AbsModelType.MODEL_CHAT] = { it.readTypedObjectCompat(Chat.CREATOR) }
            TYPES[AbsModelType.MODEL_COMMENT] = { it.readTypedObjectCompat(Comment.CREATOR) }
            TYPES[AbsModelType.MODEL_COMMUNITY] = { it.readTypedObjectCompat(Community.CREATOR) }
            TYPES[AbsModelType.MODEL_DOCUMENT] = { it.readTypedObjectCompat(Document.CREATOR) }
            TYPES[AbsModelType.MODEL_DOCUMENT_GRAFFITI] =
                { it.readTypedObjectCompat(Document.Graffiti.CREATOR) }
            TYPES[AbsModelType.MODEL_DOCUMENT_VIDEO_PREVIEW] =
                { it.readTypedObjectCompat(Document.VideoPreview.CREATOR) }
            TYPES[AbsModelType.MODEL_EVENT] = { it.readTypedObjectCompat(Event.CREATOR) }
            TYPES[AbsModelType.MODEL_FAVE_LINK] = { it.readTypedObjectCompat(FaveLink.CREATOR) }
            TYPES[AbsModelType.MODEL_FWDMESSAGES] =
                { it.readTypedObjectCompat(FwdMessages.CREATOR) }
            TYPES[AbsModelType.MODEL_GIFT] = { it.readTypedObjectCompat(Gift.CREATOR) }
            TYPES[AbsModelType.MODEL_GIFT_ITEM] = { it.readTypedObjectCompat(GiftItem.CREATOR) }
            TYPES[AbsModelType.MODEL_GRAFFITI] = { it.readTypedObjectCompat(Graffiti.CREATOR) }
            TYPES[AbsModelType.MODEL_LINK] = { it.readTypedObjectCompat(Link.CREATOR) }
            TYPES[AbsModelType.MODEL_MARKET] = { it.readTypedObjectCompat(Market.CREATOR) }
            TYPES[AbsModelType.MODEL_MARKET_ALBUM] =
                { it.readTypedObjectCompat(MarketAlbum.CREATOR) }
            TYPES[AbsModelType.MODEL_MESSAGE] = { it.readTypedObjectCompat(Message.CREATOR) }
            TYPES[AbsModelType.MODEL_NEWS] = { it.readTypedObjectCompat(News.CREATOR) }
            TYPES[AbsModelType.MODEL_NOT_SUPPORTED] =
                { it.readTypedObjectCompat(NotSupported.CREATOR) }
            TYPES[AbsModelType.MODEL_PHOTO] = { it.readTypedObjectCompat(Photo.CREATOR) }
            TYPES[AbsModelType.MODEL_PHOTO_ALBUM] = { it.readTypedObjectCompat(PhotoAlbum.CREATOR) }
            TYPES[AbsModelType.MODEL_POLL] = { it.readTypedObjectCompat(Poll.CREATOR) }
            TYPES[AbsModelType.MODEL_POLL_ANSWER] =
                { it.readTypedObjectCompat(Poll.Answer.CREATOR) }
            TYPES[AbsModelType.MODEL_POLL_BACKGROUND] =
                { it.readTypedObjectCompat(Poll.PollBackground.CREATOR) }
            TYPES[AbsModelType.MODEL_POLL_BACKGROUND_POINT] =
                { it.readTypedObjectCompat(Poll.PollBackgroundPoint.CREATOR) }
            TYPES[AbsModelType.MODEL_POST] = { it.readTypedObjectCompat(Post.CREATOR) }
            TYPES[AbsModelType.MODEL_SHORT_LINK] = { it.readTypedObjectCompat(ShortLink.CREATOR) }
            TYPES[AbsModelType.MODEL_STICKER] = { it.readTypedObjectCompat(Sticker.CREATOR) }
            TYPES[AbsModelType.MODEL_STORY] = { it.readTypedObjectCompat(Story.CREATOR) }
            TYPES[AbsModelType.MODEL_TOPIC] = { it.readTypedObjectCompat(Topic.CREATOR) }
            TYPES[AbsModelType.MODEL_USER] = { it.readTypedObjectCompat(User.CREATOR) }
            TYPES[AbsModelType.MODEL_VIDEO] = { it.readTypedObjectCompat(Video.CREATOR) }
            TYPES[AbsModelType.MODEL_VIDEO_ALBUM] = { it.readTypedObjectCompat(VideoAlbum.CREATOR) }
            TYPES[AbsModelType.MODEL_VOICE_MESSAGE] =
                { it.readTypedObjectCompat(VoiceMessage.CREATOR) }
            TYPES[AbsModelType.MODEL_WALL_REPLY] = { it.readTypedObjectCompat(WallReply.CREATOR) }
            TYPES[AbsModelType.MODEL_WIKI_PAGE] = { it.readTypedObjectCompat(WikiPage.CREATOR) }
            TYPES[AbsModelType.MODEL_AUDIO_CATALOG_V2_ARTIST] =
                { it.readTypedObjectCompat(CatalogV2ArtistItem.CREATOR) }
            TYPES[AbsModelType.MODEL_UPLOAD] = { it.readTypedObjectCompat(Upload.CREATOR) }
            TYPES[AbsModelType.MODEL_GEO] = { it.readTypedObjectCompat(Geo.CREATOR) }
            TYPES[AbsModelType.MODEL_CATALOG_V2_BLOCK] =
                { it.readTypedObjectCompat(CatalogV2Block.CREATOR) }
            TYPES[AbsModelType.MODEL_CATALOG_V2_LINK] =
                { it.readTypedObjectCompat(CatalogV2Link.CREATOR) }
            TYPES[AbsModelType.MODEL_CATALOG_V2_RECOMMENDATION_PLAYLIST] =
                { it.readTypedObjectCompat(CatalogV2RecommendationPlaylist.CREATOR) }
        }
    }

    private val model: AbsModel

    private constructor(model: AbsModel) {
        this.model = model
    }

    internal constructor(parcel: Parcel) {
        val index = parcel.readInt()
        model = TYPES[index]!!.invoke(parcel)!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (!TYPES.contains(model.getModelType())) {
            throw UnsupportedOperationException("Unsupported class: " + model.javaClass)
        }
        dest.writeInt(model.getModelType())
        dest.writeTypedObjectCompat(model, flags)
    }

    fun get(): AbsModel {
        return model
    }
}