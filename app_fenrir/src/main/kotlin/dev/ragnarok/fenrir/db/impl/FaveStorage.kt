package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFaveArticlesContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFaveGroupsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFaveLinksContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFavePhotosContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFavePostsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFaveProductsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFaveUsersContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFaveVideosContentUriFor
import dev.ragnarok.fenrir.db.column.FaveArticlesColumns
import dev.ragnarok.fenrir.db.column.FaveLinksColumns
import dev.ragnarok.fenrir.db.column.FavePagesColumns
import dev.ragnarok.fenrir.db.column.FavePhotosColumns
import dev.ragnarok.fenrir.db.column.FavePostsColumns
import dev.ragnarok.fenrir.db.column.FaveProductsColumns
import dev.ragnarok.fenrir.db.column.FaveVideosColumns
import dev.ragnarok.fenrir.db.interfaces.IFaveStorage
import dev.ragnarok.fenrir.db.model.entity.ArticleDboEntity
import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FaveLinkEntity
import dev.ragnarok.fenrir.db.model.entity.FavePageEntity
import dev.ragnarok.fenrir.db.model.entity.MarketDboEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import dev.ragnarok.fenrir.db.model.entity.VideoDboEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.criteria.FaveArticlesCriteria
import dev.ragnarok.fenrir.model.criteria.FavePhotosCriteria
import dev.ragnarok.fenrir.model.criteria.FavePostsCriteria
import dev.ragnarok.fenrir.model.criteria.FaveProductsCriteria
import dev.ragnarok.fenrir.model.criteria.FaveVideosCriteria
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.serialization.msgpack.MsgPack
import kotlin.math.abs

internal class FaveStorage(mRepositoryContext: AppStorages) : AbsStorage(mRepositoryContext),
    IFaveStorage {
    override fun getFavePosts(criteria: FavePostsCriteria): Flow<List<PostDboEntity>> {
        return flow {
            val uri = getFavePostsContentUriFor(criteria.accountId)
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val dbos: MutableList<PostDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapFavePosts(cursor))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun storePosts(
        accountId: Long,
        posts: List<PostDboEntity>,
        owners: OwnerEntities?,
        clearBeforeStore: Boolean
    ): Flow<Boolean> {
        return flow {
            val uri = getFavePostsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeStore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            for (dbo in posts) {
                val cv = ContentValues()
                cv.put(FavePostsColumns.POST_ID, dbo.id)
                cv.put(FavePostsColumns.OWNER_ID, dbo.ownerId)
                cv.put(
                    FavePostsColumns.POST,
                    MsgPack.encodeToByteArrayEx(PostDboEntity.serializer(), dbo)
                )
                operations.add(
                    ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            if (owners != null) {
                OwnersStorage.appendOwnersInsertOperations(operations, accountId, owners)
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun getFaveLinks(accountId: Long): Flow<List<FaveLinkEntity>> {
        return flow {
            val uri = getFaveLinksContentUriFor(accountId)
            val cursor = contentResolver.query(uri, null, null, null, null)
            val data: MutableList<FaveLinkEntity> = ArrayList()
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    data.add(mapFaveLink(cursor))
                }
                cursor.close()
            }
            emit(data)
        }
    }

    override fun removeLink(accountId: Long, id: String?): Flow<Boolean> {
        return flow {
            val uri = getFaveLinksContentUriFor(accountId)
            val where = FaveLinksColumns.LINK_ID + " LIKE ?"
            val args = arrayOf(id)
            contentResolver.delete(uri, where, args)
            emit(true)
        }
    }

    override fun storeLinks(
        accountId: Long,
        entities: List<FaveLinkEntity>,
        clearBefore: Boolean
    ): Flow<Boolean> {
        return flow {
            val uri = getFaveLinksContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBefore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            for (entity in entities) {
                val cv = ContentValues()
                cv.put(FaveLinksColumns.LINK_ID, entity.id)
                cv.put(FaveLinksColumns.URL, entity.url)
                cv.put(FaveLinksColumns.TITLE, entity.title)
                cv.put(FaveLinksColumns.DESCRIPTION, entity.description)
                entity.photo.ifNonNull({
                    cv.put(
                        FaveLinksColumns.PHOTO,
                        MsgPack.encodeToByteArrayEx(PhotoDboEntity.serializer(), it)
                    )
                }, {
                    cv.putNull(
                        FaveLinksColumns.PHOTO
                    )
                })
                operations.add(
                    ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun removePage(accountId: Long, ownerId: Long, isUser: Boolean): Flow<Boolean> {
        return flow {
            val uri =
                if (isUser) getFaveUsersContentUriFor(accountId) else getFaveGroupsContentUriFor(
                    accountId
                )
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(ownerId.toString())
            contentResolver.delete(uri, where, args)
            emit(true)
        }
    }

    override fun getFaveUsers(accountId: Long): Flow<List<FavePageEntity>> {
        return flow {
            val uri = getFaveUsersContentUriFor(accountId)
            val cursor = contentResolver.query(uri, null, null, null, null)
            val dbos: MutableList<FavePageEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapFaveUserDbo(cursor, accountId))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun getFaveGroups(accountId: Long): Flow<List<FavePageEntity>> {
        return flow {
            val uri = getFaveGroupsContentUriFor(accountId)
            val cursor = contentResolver.query(uri, null, null, null, null)
            val dbos: MutableList<FavePageEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapFaveGroupDbo(cursor, accountId))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun storePhotos(
        accountId: Long,
        photos: List<PhotoDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray> {
        return flow {
            val operations = ArrayList<ContentProviderOperation>()
            val uri = getFavePhotosContentUriFor(accountId)
            if (clearBeforeStore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }

            // массив для хранения индексов операций вставки для каждого фото
            val indexes = IntArray(photos.size)
            for (i in photos.indices) {
                val dbo = photos[i]
                val cv = ContentValues()
                cv.put(FavePhotosColumns.PHOTO_ID, dbo.id)
                cv.put(FavePhotosColumns.OWNER_ID, dbo.ownerId)
                cv.put(FavePhotosColumns.POST_ID, dbo.postId)
                cv.put(
                    FavePhotosColumns.PHOTO,
                    MsgPack.encodeToByteArrayEx(PhotoDboEntity.serializer(), dbo)
                )
                val index = addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
                indexes[i] = index
            }
            val results = contentResolver
                .applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(results.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emit(ids)
        }
    }

    override fun getPhotos(criteria: FavePhotosCriteria): Flow<List<PhotoDboEntity>> {
        return flow {
            val where: String?
            val args: Array<String>?
            val uri = getFavePhotosContentUriFor(criteria.accountId)
            val range = criteria.range
            if (range == null) {
                where = null
                args = null
            } else {
                where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                args = arrayOf(range.first.toString(), range.last.toString())
            }
            val cursor = contentResolver.query(uri, null, where, args, null)
            val dbos: MutableList<PhotoDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    mapFavePhoto(cursor)?.let { dbos.add(it) }
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun getVideos(criteria: FaveVideosCriteria): Flow<List<VideoDboEntity>> {
        return flow {
            val uri = getFaveVideosContentUriFor(criteria.accountId)
            val where: String?
            val args: Array<String>?
            val range = criteria.range
            if (range != null) {
                where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                args = arrayOf(range.first.toString(), range.last.toString())
            } else {
                where = null
                args = null
            }
            val cursor = contentResolver.query(uri, null, where, args, null)
            val dbos: MutableList<VideoDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapVideo(cursor))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    private fun mapVideo(cursor: Cursor): VideoDboEntity {
        val json = cursor.getBlob(FaveVideosColumns.VIDEO)!!
        return MsgPack.decodeFromByteArrayEx(VideoDboEntity.serializer(), json)
    }

    override fun getArticles(criteria: FaveArticlesCriteria): Flow<List<ArticleDboEntity>> {
        return flow {
            val uri = getFaveArticlesContentUriFor(criteria.accountId)
            val where: String?
            val args: Array<String>?
            val range = criteria.range
            if (range != null) {
                where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                args = arrayOf(range.first.toString(), range.last.toString())
            } else {
                where = null
                args = null
            }
            val cursor = contentResolver.query(uri, null, where, args, null)
            val dbos: MutableList<ArticleDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapArticle(cursor))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun getProducts(criteria: FaveProductsCriteria): Flow<List<MarketDboEntity>> {
        return flow {
            val uri = getFaveProductsContentUriFor(criteria.accountId)
            val where: String?
            val args: Array<String>?
            val range = criteria.range
            if (range != null) {
                where = BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?"
                args = arrayOf(range.first.toString(), range.last.toString())
            } else {
                where = null
                args = null
            }
            val cursor = contentResolver.query(uri, null, where, args, null)
            val dbos: MutableList<MarketDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapProduct(cursor))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    private fun mapArticle(cursor: Cursor): ArticleDboEntity {
        val json = cursor.getBlob(FaveArticlesColumns.ARTICLE)!!
        return MsgPack.decodeFromByteArrayEx(ArticleDboEntity.serializer(), json)
    }

    private fun mapProduct(cursor: Cursor): MarketDboEntity {
        val json = cursor.getBlob(FaveProductsColumns.PRODUCT)!!
        return MsgPack.decodeFromByteArrayEx(MarketDboEntity.serializer(), json)
    }

    override fun storeVideos(
        accountId: Long,
        videos: List<VideoDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray> {
        return flow {
            val uri = getFaveVideosContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeStore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            val indexes = IntArray(videos.size)
            for (i in videos.indices) {
                val dbo = videos[i]
                val cv = ContentValues()
                cv.put(FaveVideosColumns.VIDEO_ID, dbo.id)
                cv.put(FaveVideosColumns.OWNER_ID, dbo.ownerId)
                cv.put(
                    FaveVideosColumns.VIDEO,
                    MsgPack.encodeToByteArrayEx(VideoDboEntity.serializer(), dbo)
                )
                val index = addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
                indexes[i] = index
            }
            val results = contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(results.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emit(ids)
        }
    }

    override fun storeArticles(
        accountId: Long,
        articles: List<ArticleDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray> {
        return flow {
            val uri = getFaveArticlesContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeStore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            val indexes = IntArray(articles.size)
            for (i in articles.indices) {
                val dbo = articles[i]
                val cv = ContentValues()
                cv.put(FaveArticlesColumns.ARTICLE_ID, dbo.id)
                cv.put(FaveArticlesColumns.OWNER_ID, dbo.ownerId)
                cv.put(
                    FaveArticlesColumns.ARTICLE,
                    MsgPack.encodeToByteArrayEx(ArticleDboEntity.serializer(), dbo)
                )
                val index = addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
                indexes[i] = index
            }
            val results = contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(results.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emit(ids)
        }
    }

    override fun storeProducts(
        accountId: Long,
        products: List<MarketDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray> {
        return flow {
            val uri = getFaveProductsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeStore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            val indexes = IntArray(products.size)
            for (i in products.indices) {
                val dbo = products[i]
                val cv = ContentValues()
                cv.put(FaveProductsColumns.PRODUCT_ID, dbo.id)
                cv.put(FaveProductsColumns.OWNER_ID, dbo.owner_id)
                cv.put(
                    FaveProductsColumns.PRODUCT,
                    MsgPack.encodeToByteArrayEx(MarketDboEntity.serializer(), dbo)
                )
                val index = addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
                indexes[i] = index
            }
            val results = contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(results.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emit(ids)
        }
    }

    override fun storePages(
        accountId: Long,
        users: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Flow<Boolean> {
        return flow {
            val uri = getFaveUsersContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeStore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            for (i in users.indices) {
                val dbo = users[i]
                val cv = createFaveCv(dbo)
                addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            if (operations.isNotEmpty()) {
                contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            }
            emit(true)
        }
    }

    override fun storeGroups(
        accountId: Long,
        groups: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Flow<Boolean> {
        return flow {
            val uri = getFaveGroupsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeStore) {
                operations.add(
                    ContentProviderOperation
                        .newDelete(uri)
                        .build()
                )
            }
            for (i in groups.indices) {
                val dbo = groups[i]
                val cv = createFaveCv(dbo)
                addToListAndReturnIndex(
                    operations, ContentProviderOperation
                        .newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            if (operations.isNotEmpty()) {
                contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            }
            emit(true)
        }
    }

    private fun mapFaveLink(cursor: Cursor): FaveLinkEntity {
        val id = cursor.getString(FaveLinksColumns.LINK_ID)
        val url = cursor.getString(FaveLinksColumns.URL)
        return FaveLinkEntity(id, url)
            .setTitle(cursor.getString(FaveLinksColumns.TITLE))
            .setDescription(cursor.getString(FaveLinksColumns.DESCRIPTION))
            .setPhoto(mapFaveLinkPhoto(cursor))
    }

    private fun mapFavePosts(cursor: Cursor): PostDboEntity {
        val json = cursor.getBlob(FavePostsColumns.POST)!!
        return MsgPack.decodeFromByteArrayEx(PostDboEntity.serializer(), json)
    }

    companion object {
        internal fun createFaveCv(dbo: FavePageEntity): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, dbo.id)
            cv.put(FavePagesColumns.DESCRIPTION, dbo.description)
            cv.put(FavePagesColumns.FAVE_TYPE, dbo.faveType)
            cv.put(FavePagesColumns.UPDATED_TIME, dbo.updateDate)
            return cv
        }

        private suspend fun mapUser(accountId: Long, id: Long): UserEntity? {
            return stores.owners().findUserDboById(accountId, id).single().get()
        }

        private suspend fun mapGroup(accountId: Long, id: Long): CommunityEntity? {
            return stores.owners().findCommunityDboById(accountId, abs(id)).single().get()
        }

        internal suspend fun mapFaveUserDbo(cursor: Cursor, accountId: Long): FavePageEntity {
            return FavePageEntity(cursor.getLong(BaseColumns._ID))
                .setDescription(cursor.getString(FavePagesColumns.DESCRIPTION))
                .setUpdateDate(cursor.getLong(FavePagesColumns.UPDATED_TIME))
                .setFaveType(cursor.getString(FavePagesColumns.FAVE_TYPE))
                .setUser(
                    mapUser(
                        accountId,
                        cursor.getLong(BaseColumns._ID)
                    )
                )
        }

        internal suspend fun mapFaveGroupDbo(cursor: Cursor, accountId: Long): FavePageEntity {
            return FavePageEntity(cursor.getLong(BaseColumns._ID))
                .setDescription(cursor.getString(FavePagesColumns.DESCRIPTION))
                .setUpdateDate(cursor.getLong(FavePagesColumns.UPDATED_TIME))
                .setFaveType(cursor.getString(FavePagesColumns.FAVE_TYPE))
                .setGroup(
                    mapGroup(
                        accountId,
                        cursor.getLong(BaseColumns._ID)
                    )
                )
        }

        internal fun mapFavePhoto(cursor: Cursor): PhotoDboEntity? {
            val json = cursor.getBlob(FavePhotosColumns.PHOTO)
            return json?.let { MsgPack.decodeFromByteArrayEx(PhotoDboEntity.serializer(), it) }
        }

        internal fun mapFaveLinkPhoto(cursor: Cursor): PhotoDboEntity? {
            val json = cursor.getBlob(FaveLinksColumns.PHOTO)
            return json?.let { MsgPack.decodeFromByteArrayEx(PhotoDboEntity.serializer(), it) }
        }
    }
}