package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getVideosContentUriFor
import dev.ragnarok.fenrir.db.column.VideoColumns
import dev.ragnarok.fenrir.db.interfaces.IVideoStorage
import dev.ragnarok.fenrir.db.model.entity.PrivacyEntity
import dev.ragnarok.fenrir.db.model.entity.VideoDboEntity
import dev.ragnarok.fenrir.model.VideoCriteria
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class VideoStorage(base: AppStorages) : AbsStorage(base), IVideoStorage {
    override fun findByCriteria(criteria: VideoCriteria): Single<List<VideoDboEntity>> {
        return Single.create { e: SingleEmitter<List<VideoDboEntity>> ->
            val uri = getVideosContentUriFor(criteria.getAccountId())
            val where: String
            val args: Array<String>
            val range = criteria.getRange()
            when {
                range != null -> {
                    where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                    args = arrayOf(range.first.toString(), range.last.toString())
                }
                criteria.getAlbumId() == 0 -> {
                    where = VideoColumns.OWNER_ID + " = ?"
                    args = arrayOf(criteria.getOwnerId().toString())
                }
                else -> {
                    where = VideoColumns.OWNER_ID + " = ? AND " + VideoColumns.ALBUM_ID + " = ?"
                    args =
                        arrayOf(criteria.getOwnerId().toString(), criteria.getAlbumId().toString())
                }
            }
            val cursor =
                contentResolver.query(uri, null, where, args, VideoColumns.ADDING_DATE + " DESC")
            val videos = ArrayList<VideoDboEntity>(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    videos.add(mapVideo(cursor))
                }
                cursor.close()
            }
            e.onSuccess(videos)
        }
    }

    private fun mapVideo(cursor: Cursor): VideoDboEntity {
        val id = cursor.getInt(VideoColumns.VIDEO_ID)
        val ownerId = cursor.getInt(VideoColumns.ORIGINAL_OWNER_ID)
        val video = VideoDboEntity().set(id, ownerId)
            .setAlbumId(cursor.getInt(VideoColumns.ALBUM_ID))
            .setTitle(cursor.getString(VideoColumns.TITLE))
            .setDescription(cursor.getString(VideoColumns.DESCRIPTION))
            .setLink(cursor.getString(VideoColumns.LINK))
            .setDuration(cursor.getInt(VideoColumns.DURATION))
            .setDate(cursor.getLong(VideoColumns.DATE))
            .setAddingDate(cursor.getLong(VideoColumns.ADDING_DATE))
            .setViews(cursor.getInt(VideoColumns.VIEWS))
            .setPlayer(cursor.getString(VideoColumns.PLAYER))
            .setImage(cursor.getString(VideoColumns.IMAGE))
            .setAccessKey(cursor.getString(VideoColumns.ACCESS_KEY))
            .setCommentsCount(cursor.getInt(VideoColumns.COMMENTS))
            .setCanComment(cursor.getBoolean(VideoColumns.CAN_COMMENT))
            .setPrivate(cursor.getBoolean(VideoColumns.IS_PRIVATE))
            .setFavorite(cursor.getBoolean(VideoColumns.IS_FAVORITE))
            .setCanRepost(cursor.getBoolean(VideoColumns.CAN_REPOST))
            .setUserLikes(cursor.getBoolean(VideoColumns.USER_LIKES))
            .setLikesCount(cursor.getInt(VideoColumns.LIKES))
            .setRepeat(cursor.getBoolean(VideoColumns.REPEAT))
            .setMp4link240(cursor.getString(VideoColumns.MP4_240))
            .setMp4link360(cursor.getString(VideoColumns.MP4_360))
            .setMp4link480(cursor.getString(VideoColumns.MP4_480))
            .setMp4link720(cursor.getString(VideoColumns.MP4_720))
            .setMp4link1080(cursor.getString(VideoColumns.MP4_1080))
            .setMp4link1440(cursor.getString(VideoColumns.MP4_1440))
            .setMp4link2160(cursor.getString(VideoColumns.MP4_2160))
            .setExternalLink(cursor.getString(VideoColumns.EXTERNAL))
            .setHls(cursor.getString(VideoColumns.HLS))
            .setLive(cursor.getString(VideoColumns.LIVE))
            .setPlatform(cursor.getString(VideoColumns.PLATFORM))
            .setCanEdit(cursor.getBoolean(VideoColumns.CAN_EDIT))
            .setCanAdd(cursor.getBoolean(VideoColumns.CAN_ADD))
        val privacyViewText =
            cursor.getBlob(VideoColumns.PRIVACY_VIEW)
        if (privacyViewText.nonNullNoEmpty()) {
            video.setPrivacyView(
                MsgPack.decodeFromByteArrayEx(
                    PrivacyEntity.serializer(),
                    privacyViewText
                )
            )
        }
        val privacyCommentText =
            cursor.getBlob(VideoColumns.PRIVACY_COMMENT)
        if (privacyCommentText.nonNullNoEmpty()) {
            video.setPrivacyComment(
                MsgPack.decodeFromByteArrayEx(
                    PrivacyEntity.serializer(),
                    privacyCommentText
                )
            )
        }
        return video
    }

    override fun insertData(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        videos: List<VideoDboEntity>,
        invalidateBefore: Boolean
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val operations = ArrayList<ContentProviderOperation>()
            val uri = getVideosContentUriFor(accountId)
            if (invalidateBefore) {
                if (albumId == 0) {
                    operations.add(
                        ContentProviderOperation
                            .newDelete(uri)
                            .withSelection(
                                VideoColumns.OWNER_ID + " = ?",
                                arrayOf(ownerId.toString())
                            )
                            .build()
                    )
                } else {
                    operations.add(
                        ContentProviderOperation
                            .newDelete(uri)
                            .withSelection(
                                VideoColumns.OWNER_ID + " = ? AND " + VideoColumns.ALBUM_ID + " = ?",
                                arrayOf(ownerId.toString(), albumId.toString())
                            )
                            .build()
                    )
                }
            }
            for (dbo in videos) {
                val cv = getCV(dbo, ownerId)
                cv.put(VideoColumns.ALBUM_ID, albumId)
                operations.add(
                    ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            e.onComplete()
        }
    }

    companion object {
        /* Дело в том, что вк передает в p.owner_id идентификатор оригинального владельца.
     * Поэтому необходимо отдельно сохранять идентикатор owner-а, у кого в видеозаписях мы нашли видео */
        fun getCV(dbo: VideoDboEntity, ownerId: Int): ContentValues {
            val cv = ContentValues()
            cv.put(VideoColumns.VIDEO_ID, dbo.id)
            cv.put(VideoColumns.OWNER_ID, ownerId)
            cv.put(VideoColumns.ORIGINAL_OWNER_ID, dbo.ownerId)
            cv.put(VideoColumns.ALBUM_ID, dbo.albumId)
            cv.put(VideoColumns.TITLE, dbo.title)
            cv.put(VideoColumns.DESCRIPTION, dbo.description)
            cv.put(VideoColumns.DURATION, dbo.duration)
            cv.put(VideoColumns.LINK, dbo.link)
            cv.put(VideoColumns.DATE, dbo.date)
            cv.put(VideoColumns.ADDING_DATE, dbo.addingDate)
            cv.put(VideoColumns.VIEWS, dbo.views)
            cv.put(VideoColumns.PLAYER, dbo.player)
            cv.put(VideoColumns.IMAGE, dbo.image)
            cv.put(VideoColumns.ACCESS_KEY, dbo.accessKey)
            cv.put(VideoColumns.COMMENTS, dbo.commentsCount)
            cv.put(VideoColumns.CAN_COMMENT, dbo.isCanComment)
            cv.put(VideoColumns.IS_PRIVATE, dbo.private)
            cv.put(VideoColumns.IS_FAVORITE, dbo.isFavorite)
            cv.put(VideoColumns.CAN_REPOST, dbo.isCanRepost)
            cv.put(VideoColumns.USER_LIKES, dbo.isUserLikes)
            cv.put(VideoColumns.REPEAT, dbo.isRepeat)
            cv.put(VideoColumns.LIKES, dbo.likesCount)

            dbo.privacyView.ifNonNull({
                cv.put(
                    VideoColumns.PRIVACY_VIEW,
                    MsgPack.encodeToByteArrayEx(PrivacyEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    VideoColumns.PRIVACY_VIEW
                )
            })

            dbo.privacyComment.ifNonNull({
                cv.put(
                    VideoColumns.PRIVACY_VIEW,
                    MsgPack.encodeToByteArrayEx(PrivacyEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    VideoColumns.PRIVACY_VIEW
                )
            })
            cv.put(VideoColumns.MP4_240, dbo.mp4link240)
            cv.put(VideoColumns.MP4_360, dbo.mp4link360)
            cv.put(VideoColumns.MP4_480, dbo.mp4link480)
            cv.put(VideoColumns.MP4_720, dbo.mp4link720)
            cv.put(VideoColumns.MP4_1080, dbo.mp4link1080)
            cv.put(VideoColumns.MP4_1440, dbo.mp4link1440)
            cv.put(VideoColumns.MP4_2160, dbo.mp4link2160)
            cv.put(VideoColumns.EXTERNAL, dbo.externalLink)
            cv.put(VideoColumns.HLS, dbo.hls)
            cv.put(VideoColumns.LIVE, dbo.live)
            cv.put(VideoColumns.PLATFORM, dbo.platform)
            cv.put(VideoColumns.CAN_EDIT, dbo.isCanEdit)
            cv.put(VideoColumns.CAN_ADD, dbo.isCanAdd)
            return cv
        }
    }
}