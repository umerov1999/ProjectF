package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getVideosContentUriFor
import dev.ragnarok.fenrir.db.column.VideosColumns
import dev.ragnarok.fenrir.db.interfaces.IVideoStorage
import dev.ragnarok.fenrir.db.model.entity.PrivacyEntity
import dev.ragnarok.fenrir.db.model.entity.VideoDboEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.VideoCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
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
                    where = VideosColumns.OWNER_ID + " = ?"
                    args = arrayOf(criteria.getOwnerId().toString())
                }

                else -> {
                    where = VideosColumns.OWNER_ID + " = ? AND " + VideosColumns.ALBUM_ID + " = ?"
                    args =
                        arrayOf(criteria.getOwnerId().toString(), criteria.getAlbumId().toString())
                }
            }
            val cursor =
                contentResolver.query(uri, null, where, args, VideosColumns.ADDING_DATE + " DESC")
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
        val id = cursor.getInt(VideosColumns.VIDEO_ID)
        val ownerId = cursor.getLong(VideosColumns.ORIGINAL_OWNER_ID)
        val video = VideoDboEntity().set(id, ownerId)
            .setAlbumId(cursor.getInt(VideosColumns.ALBUM_ID))
            .setTitle(cursor.getString(VideosColumns.TITLE))
            .setDescription(cursor.getString(VideosColumns.DESCRIPTION))
            .setLink(cursor.getString(VideosColumns.LINK))
            .setDuration(cursor.getLong(VideosColumns.DURATION))
            .setDate(cursor.getLong(VideosColumns.DATE))
            .setAddingDate(cursor.getLong(VideosColumns.ADDING_DATE))
            .setViews(cursor.getInt(VideosColumns.VIEWS))
            .setPlayer(cursor.getString(VideosColumns.PLAYER))
            .setImage(cursor.getString(VideosColumns.IMAGE))
            .setAccessKey(cursor.getString(VideosColumns.ACCESS_KEY))
            .setCommentsCount(cursor.getInt(VideosColumns.COMMENTS))
            .setCanComment(cursor.getBoolean(VideosColumns.CAN_COMMENT))
            .setPrivate(cursor.getBoolean(VideosColumns.IS_PRIVATE))
            .setFavorite(cursor.getBoolean(VideosColumns.IS_FAVORITE))
            .setCanRepost(cursor.getBoolean(VideosColumns.CAN_REPOST))
            .setUserLikes(cursor.getBoolean(VideosColumns.USER_LIKES))
            .setLikesCount(cursor.getInt(VideosColumns.LIKES))
            .setRepeat(cursor.getBoolean(VideosColumns.REPEAT))
            .setMp4link240(cursor.getString(VideosColumns.MP4_240))
            .setMp4link360(cursor.getString(VideosColumns.MP4_360))
            .setMp4link480(cursor.getString(VideosColumns.MP4_480))
            .setMp4link720(cursor.getString(VideosColumns.MP4_720))
            .setMp4link1080(cursor.getString(VideosColumns.MP4_1080))
            .setMp4link1440(cursor.getString(VideosColumns.MP4_1440))
            .setMp4link2160(cursor.getString(VideosColumns.MP4_2160))
            .setExternalLink(cursor.getString(VideosColumns.EXTERNAL))
            .setHls(cursor.getString(VideosColumns.HLS))
            .setLive(cursor.getString(VideosColumns.LIVE))
            .setPlatform(cursor.getString(VideosColumns.PLATFORM))
            .setCanEdit(cursor.getBoolean(VideosColumns.CAN_EDIT))
            .setCanAdd(cursor.getBoolean(VideosColumns.CAN_ADD))
            .setTrailer(cursor.getString(VideosColumns.TRAILER))
        val timelineThumbsText =
            cursor.getBlob(VideosColumns.TIMELINE_THUMBS)
        if (timelineThumbsText.nonNullNoEmpty()) {
            video.setTimelineThumbs(
                MsgPack.decodeFromByteArrayEx(
                    VideoDboEntity.VideoDboTimelineEntity.serializer(),
                    timelineThumbsText
                )
            )
        }

        val privacyViewText =
            cursor.getBlob(VideosColumns.PRIVACY_VIEW)
        if (privacyViewText.nonNullNoEmpty()) {
            video.setPrivacyView(
                MsgPack.decodeFromByteArrayEx(
                    PrivacyEntity.serializer(),
                    privacyViewText
                )
            )
        }
        val privacyCommentText =
            cursor.getBlob(VideosColumns.PRIVACY_COMMENT)
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
        accountId: Long,
        ownerId: Long,
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
                                VideosColumns.OWNER_ID + " = ?",
                                arrayOf(ownerId.toString())
                            )
                            .build()
                    )
                } else {
                    operations.add(
                        ContentProviderOperation
                            .newDelete(uri)
                            .withSelection(
                                VideosColumns.OWNER_ID + " = ? AND " + VideosColumns.ALBUM_ID + " = ?",
                                arrayOf(ownerId.toString(), albumId.toString())
                            )
                            .build()
                    )
                }
            }
            for (dbo in videos) {
                val cv = getCV(dbo, ownerId)
                cv.put(VideosColumns.ALBUM_ID, albumId)
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
        fun getCV(dbo: VideoDboEntity, ownerId: Long): ContentValues {
            val cv = ContentValues()
            cv.put(VideosColumns.VIDEO_ID, dbo.id)
            cv.put(VideosColumns.OWNER_ID, ownerId)
            cv.put(VideosColumns.ORIGINAL_OWNER_ID, dbo.ownerId)
            cv.put(VideosColumns.ALBUM_ID, dbo.albumId)
            cv.put(VideosColumns.TITLE, dbo.title)
            cv.put(VideosColumns.DESCRIPTION, dbo.description)
            cv.put(VideosColumns.DURATION, dbo.duration)
            cv.put(VideosColumns.LINK, dbo.link)
            cv.put(VideosColumns.DATE, dbo.date)
            cv.put(VideosColumns.ADDING_DATE, dbo.addingDate)
            cv.put(VideosColumns.VIEWS, dbo.views)
            cv.put(VideosColumns.PLAYER, dbo.player)
            cv.put(VideosColumns.IMAGE, dbo.image)
            cv.put(VideosColumns.ACCESS_KEY, dbo.accessKey)
            cv.put(VideosColumns.COMMENTS, dbo.commentsCount)
            cv.put(VideosColumns.CAN_COMMENT, dbo.isCanComment)
            cv.put(VideosColumns.IS_PRIVATE, dbo.private)
            cv.put(VideosColumns.IS_FAVORITE, dbo.isFavorite)
            cv.put(VideosColumns.CAN_REPOST, dbo.isCanRepost)
            cv.put(VideosColumns.USER_LIKES, dbo.isUserLikes)
            cv.put(VideosColumns.REPEAT, dbo.isRepeat)
            cv.put(VideosColumns.LIKES, dbo.likesCount)

            dbo.privacyView.ifNonNull({
                cv.put(
                    VideosColumns.PRIVACY_VIEW,
                    MsgPack.encodeToByteArrayEx(PrivacyEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    VideosColumns.PRIVACY_VIEW
                )
            })

            dbo.privacyComment.ifNonNull({
                cv.put(
                    VideosColumns.PRIVACY_VIEW,
                    MsgPack.encodeToByteArrayEx(PrivacyEntity.serializer(), it)
                )
            }, {
                cv.putNull(
                    VideosColumns.PRIVACY_VIEW
                )
            })
            dbo.timelineThumbs.ifNonNull({
                cv.put(
                    VideosColumns.TIMELINE_THUMBS,
                    MsgPack.encodeToByteArrayEx(
                        VideoDboEntity.VideoDboTimelineEntity.serializer(),
                        it
                    )
                )
            }, {
                cv.putNull(
                    VideosColumns.TIMELINE_THUMBS
                )
            })
            cv.put(VideosColumns.TRAILER, dbo.trailer)

            cv.put(VideosColumns.MP4_240, dbo.mp4link240)
            cv.put(VideosColumns.MP4_360, dbo.mp4link360)
            cv.put(VideosColumns.MP4_480, dbo.mp4link480)
            cv.put(VideosColumns.MP4_720, dbo.mp4link720)
            cv.put(VideosColumns.MP4_1080, dbo.mp4link1080)
            cv.put(VideosColumns.MP4_1440, dbo.mp4link1440)
            cv.put(VideosColumns.MP4_2160, dbo.mp4link2160)
            cv.put(VideosColumns.EXTERNAL, dbo.externalLink)
            cv.put(VideosColumns.HLS, dbo.hls)
            cv.put(VideosColumns.LIVE, dbo.live)
            cv.put(VideosColumns.PLATFORM, dbo.platform)
            cv.put(VideosColumns.CAN_EDIT, dbo.isCanEdit)
            cv.put(VideosColumns.CAN_ADD, dbo.isCanAdd)
            return cv
        }
    }
}