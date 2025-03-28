package dev.ragnarok.fenrir.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import androidx.core.database.sqlite.transaction
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.db.column.CommentsColumns
import dev.ragnarok.fenrir.db.column.CountriesColumns
import dev.ragnarok.fenrir.db.column.DialogsColumns
import dev.ragnarok.fenrir.db.column.DocsColumns
import dev.ragnarok.fenrir.db.column.EncryptionKeysForMessagesColumns
import dev.ragnarok.fenrir.db.column.FaveArticlesColumns
import dev.ragnarok.fenrir.db.column.FaveLinksColumns
import dev.ragnarok.fenrir.db.column.FavePagesColumns
import dev.ragnarok.fenrir.db.column.FavePhotosColumns
import dev.ragnarok.fenrir.db.column.FavePostsColumns
import dev.ragnarok.fenrir.db.column.FaveProductsColumns
import dev.ragnarok.fenrir.db.column.FaveVideosColumns
import dev.ragnarok.fenrir.db.column.FeedListsColumns
import dev.ragnarok.fenrir.db.column.FriendListsColumns
import dev.ragnarok.fenrir.db.column.GroupsColumns
import dev.ragnarok.fenrir.db.column.GroupsDetailsColumns
import dev.ragnarok.fenrir.db.column.MessagesColumns
import dev.ragnarok.fenrir.db.column.NewsColumns
import dev.ragnarok.fenrir.db.column.NotificationsColumns
import dev.ragnarok.fenrir.db.column.PeersColumns
import dev.ragnarok.fenrir.db.column.PhotoAlbumsColumns
import dev.ragnarok.fenrir.db.column.PhotosColumns
import dev.ragnarok.fenrir.db.column.PhotosExtendedColumns
import dev.ragnarok.fenrir.db.column.PostsColumns
import dev.ragnarok.fenrir.db.column.RelationshipsColumns
import dev.ragnarok.fenrir.db.column.TopicsColumns
import dev.ragnarok.fenrir.db.column.UsersColumns
import dev.ragnarok.fenrir.db.column.UsersDetailsColumns
import dev.ragnarok.fenrir.db.column.VideoAlbumsColumns
import dev.ragnarok.fenrir.db.column.VideosColumns
import dev.ragnarok.fenrir.db.column.attachments.CommentsAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.MessagesAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.WallsAttachmentsColumns
import dev.ragnarok.fenrir.module.FenrirNative
import java.util.concurrent.ConcurrentHashMap

class DBHelper private constructor(context: Context, aid: Long) :
    SQLiteOpenHelper(context, getDatabaseFileName(aid), null, Constants.DATABASE_FENRIR_VERSION) {
    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    /*
    private fun insertColumn(
        db: SQLiteDatabase,
        tableName: String,
        column: String,
        type: String,
        defaultValue: String
    ) {
        db.execSQL("ALTER TABLE $tableName ADD $column $type DEFAULT $defaultValue;")
    }

    private fun dropColumn(db: SQLiteDatabase, tableName: String, column: String) {
        db.execSQL("ALTER TABLE $tableName DROP COLUMN $column;")
    }
     */

    private fun doRecreateBase(db: SQLiteDatabase) {
        dropAllTables(db)
        onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != Constants.DATABASE_FENRIR_VERSION) {
            doRecreateBase(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != Constants.DATABASE_FENRIR_VERSION) {
            doRecreateBase(db)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        createUsersTable(db)
        createMessagesTable(db)
        createAttachmentsTable(db)
        createDialogTable(db)
        createPeersTable(db)
        createPhotoTable(db)
        createPhotoExtendedTable(db)
        createDocsTable(db)
        createVideosTable(db)
        createPostAttachmentsTable(db)
        createPostsTable(db)
        createGroupsTable(db)
        createRelationshipTable(db)
        createCommentsTable(db)
        createCommentsAttachmentsTable(db)
        createPhotoAlbumsTable(db)
        createNewsTable(db)
        createGroupsDetTable(db)
        createVideoAlbumsTable(db)
        createTopicsTable(db)
        createNotificationsTable(db)
        createUserDetTable(db)
        createFavePhotosTable(db)
        createFaveVideosTable(db)
        createFaveArticlesTable(db)
        createFaveProductTable(db)
        createFavePageTable(db)
        createFaveGroupsTable(db)
        createFaveLinksTable(db)
        createFavePostsTable(db)
        createCountriesTable(db)
        createFeedListsTable(db)
        createFriendListsTable(db)
        createKeysTableIfNotExist(db)

        createZeroMessageProtectionTriggers(db)
        //createAttachmentsTriggers(db)
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        db.transaction {
            // сначала удаляем триггеры, потому что они зависят от таблиц
            //db.execSQL("DROP TRIGGER IF EXISTS t_delete_attachments_wall")
            //db.execSQL("DROP TRIGGER IF EXISTS t_delete_attachments_messages")
            //db.execSQL("DROP TRIGGER IF EXISTS t_delete_attachments_comments")
            execSQL("DROP TRIGGER IF EXISTS zero_msg_upd")
            execSQL("DROP TRIGGER IF EXISTS zero_msg_del")

            execSQL("DROP TABLE IF EXISTS " + MessagesAttachmentsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + CommentsAttachmentsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + CommentsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + DialogsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + PeersColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + DocsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + GroupsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + GroupsDetailsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + MessagesColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + NewsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + PhotoAlbumsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + PhotosColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + PhotosExtendedColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + WallsAttachmentsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + PostsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + RelationshipsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + UsersColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + VideoAlbumsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + VideosColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + TopicsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + NotificationsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + UsersDetailsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FavePhotosColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FaveArticlesColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FaveProductsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FaveVideosColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FavePagesColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FavePagesColumns.GROUPSTABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FaveLinksColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FavePostsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + CountriesColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FeedListsColumns.TABLENAME)
            execSQL("DROP TABLE IF EXISTS " + FriendListsColumns.TABLENAME)
        }
    }

    private fun createKeysTableIfNotExist(db: SQLiteDatabase) {
        val sql =
            "CREATE TABLE IF NOT EXISTS [" + EncryptionKeysForMessagesColumns.TABLENAME + "] (\n" +
                    " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " [" + EncryptionKeysForMessagesColumns.VERSION + "] INTEGER, " +
                    " [" + EncryptionKeysForMessagesColumns.PEER_ID + "] INTEGER, " +
                    " [" + EncryptionKeysForMessagesColumns.SESSION_ID + "] INTEGER, " +
                    " [" + EncryptionKeysForMessagesColumns.DATE + "] INTEGER, " +
                    " [" + EncryptionKeysForMessagesColumns.START_SESSION_MESSAGE_ID + "] INTEGER, " +
                    " [" + EncryptionKeysForMessagesColumns.END_SESSION_MESSAGE_ID + "] INTEGER, " +
                    " [" + EncryptionKeysForMessagesColumns.OUT_KEY + "] TEXT, " +
                    " [" + EncryptionKeysForMessagesColumns.IN_KEY + "] TEXT," +
                    " CONSTRAINT [] UNIQUE ([" + EncryptionKeysForMessagesColumns.SESSION_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createZeroMessageProtectionTriggers(db: SQLiteDatabase) {
        val sqlUpdate =
            "CREATE TRIGGER zero_msg_upd BEFORE UPDATE ON " + MessagesColumns.TABLENAME + " FOR EACH ROW " +
                    "WHEN OLD." + MessagesColumns._ID + " = 0 BEGIN " +
                    "   SELECT RAISE(ABORT, 'Cannot update record with _id=0');" +
                    "END;"
        val sqlDelete =
            "CREATE TRIGGER zero_msg_del BEFORE DELETE ON " + MessagesColumns.TABLENAME + " FOR EACH ROW " +
                    "WHEN OLD." + MessagesColumns._ID + " = 0 BEGIN " +
                    "   SELECT RAISE(ABORT, 'Cannot delete record with _id=0');" +
                    "END;"
        db.execSQL(sqlUpdate)
        db.execSQL(sqlDelete)
    }

    private fun createCountriesTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + CountriesColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                " [" + CountriesColumns.NAME + "] TEXT, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createPhotoTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PhotosColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + PhotosColumns.PHOTO_ID + "] INTEGER, " +
                " [" + PhotosColumns.ALBUM_ID + "] INTEGER, " +
                " [" + PhotosColumns.OWNER_ID + "] INTEGER, " +
                " [" + PhotosColumns.ACCESS_KEY + "] TEXT, " +
                " [" + PhotosColumns.WIDTH + "] INTEGER, " +
                " [" + PhotosColumns.HEIGHT + "] INTEGER, " +
                " [" + PhotosColumns.TEXT + "] TEXT, " +
                " [" + PhotosColumns.DATE + "] INTEGER, " +
                " [" + PhotosColumns.USER_LIKES + "] BOOLEAN, " +
                " [" + PhotosColumns.CAN_COMMENT + "] BOOLEAN, " +
                " [" + PhotosColumns.LIKES + "] INTEGER, " +
                " [" + PhotosColumns.REPOSTS + "] INTEGER, " +
                " [" + PhotosColumns.COMMENTS + "] INTEGER, " +
                " [" + PhotosColumns.TAGS + "] INTEGER, " +
                " [" + PhotosColumns.DELETED + "] BOOLEAN, " +
                " [" + PhotosColumns.SIZES + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + PhotosColumns.PHOTO_ID + "], [" + PhotosColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createPhotoExtendedTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PhotosExtendedColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + PhotosExtendedColumns.DB_OWNER_ID + "] INTEGER, " +
                " [" + PhotosExtendedColumns.DB_ALBUM_ID + "] INTEGER, " +
                " [" + PhotosExtendedColumns.PHOTO_ID + "] INTEGER, " +
                " [" + PhotosExtendedColumns.ALBUM_ID + "] INTEGER, " +
                " [" + PhotosExtendedColumns.OWNER_ID + "] INTEGER, " +
                " [" + PhotosExtendedColumns.ACCESS_KEY + "] TEXT, " +
                " [" + PhotosExtendedColumns.WIDTH + "] INTEGER, " +
                " [" + PhotosExtendedColumns.HEIGHT + "] INTEGER, " +
                " [" + PhotosExtendedColumns.TEXT + "] TEXT, " +
                " [" + PhotosExtendedColumns.DATE + "] INTEGER, " +
                " [" + PhotosExtendedColumns.USER_LIKES + "] BOOLEAN, " +
                " [" + PhotosExtendedColumns.CAN_COMMENT + "] BOOLEAN, " +
                " [" + PhotosExtendedColumns.LIKES + "] INTEGER, " +
                " [" + PhotosExtendedColumns.REPOSTS + "] INTEGER, " +
                " [" + PhotosExtendedColumns.COMMENTS + "] INTEGER, " +
                " [" + PhotosExtendedColumns.TAGS + "] INTEGER, " +
                " [" + PhotosExtendedColumns.DELETED + "] BOOLEAN, " +
                " [" + PhotosExtendedColumns.SIZES + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + PhotosExtendedColumns.PHOTO_ID + "], [" + PhotosExtendedColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createAttachmentsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + MessagesAttachmentsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + MessagesAttachmentsColumns.M_ID + "] INTEGER, " +
                " [" + MessagesAttachmentsColumns.DATA + "] BLOB, " +
                //" [" + MessagesAttachmentsColumns.ATTACHMENT_ID + "] INTEGER, " +
                //" [" + MessagesAttachmentsColumns.ATTACHMENT_OWNER_ID + "] INTEGER, " +
                //" CONSTRAINT [] UNIQUE ([" + MessagesAttachmentsColumns.M_ID + "], [" + MessagesAttachmentsColumns.ATTACHMENT_ID + "], [" + MessagesAttachmentsColumns.ATTACHMENT_OWNER_ID + "], [" + MessagesAttachmentsColumns.TYPE + "]) ON CONFLICT REPLACE," +
                " FOREIGN KEY([" + MessagesAttachmentsColumns.M_ID + "]) " +
                " REFERENCES " + MessagesColumns.TABLENAME + "([" + MessagesColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);"
        db.execSQL(sql)
    }

    /**
     * Создание таблицы комментариев
     *
     * @param db БД
     */
    private fun createCommentsTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + CommentsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + CommentsColumns.COMMENT_ID + "] INTEGER, " +
                " [" + CommentsColumns.FROM_ID + "] INTEGER, " +
                " [" + CommentsColumns.SOURCE_ID + "] INTEGER, " +
                " [" + CommentsColumns.SOURCE_OWNER_ID + "] INTEGER, " +
                " [" + CommentsColumns.SOURCE_TYPE + "] INTEGER, " +
                " [" + CommentsColumns.SOURCE_ACCESS_KEY + "] TEXT, " +
                " [" + CommentsColumns.DATE + "] INTEGER, " +
                " [" + CommentsColumns.REPLY_TO_USER + "] INTEGER, " +
                " [" + CommentsColumns.REPLY_TO_COMMENT + "] INTEGER, " +
                " [" + CommentsColumns.THREADS_COUNT + "] INTEGER, " +
                " [" + CommentsColumns.LIKES + "] INTEGER, " +
                " [" + CommentsColumns.USER_LIKES + "] BOOLEAN, " +
                " [" + CommentsColumns.CAN_LIKE + "] BOOLEAN, " +
                " [" + CommentsColumns.CAN_EDIT + "] BOOLEAN, " +
                " [" + CommentsColumns.ATTACHMENTS_COUNT + "] INTEGER, " +
                " [" + CommentsColumns.DELETED + "] BOOLEAN, " +
                " [" + CommentsColumns.TEXT + "] TEXT, " +
                " [" + CommentsColumns.THREADS + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + CommentsColumns.COMMENT_ID + "]," +
                " [" + CommentsColumns.FROM_ID + "], [" + CommentsColumns.SOURCE_ID + "]," +
                " [" + CommentsColumns.SOURCE_OWNER_ID + "], [" + CommentsColumns.SOURCE_TYPE + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок фото
     *
     * @param db БД
     */
    private fun createFavePhotosTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FavePhotosColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FavePhotosColumns.PHOTO_ID + "] INTEGER, " +
                " [" + FavePhotosColumns.OWNER_ID + "] INTEGER, " +
                " [" + FavePhotosColumns.POST_ID + "] INTEGER, " +
                " [" + FavePhotosColumns.PHOTO + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + FavePhotosColumns.PHOTO_ID + "], [" + FavePhotosColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок видео
     *
     * @param db БД
     */
    private fun createFaveVideosTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FaveVideosColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FaveVideosColumns.VIDEO_ID + "] INTEGER, " +
                " [" + FaveVideosColumns.OWNER_ID + "] INTEGER, " +
                " [" + FaveVideosColumns.VIDEO + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + FaveVideosColumns.VIDEO_ID + "], [" + FaveVideosColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок статей
     *
     * @param db БД
     */
    private fun createFaveArticlesTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FaveArticlesColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FaveArticlesColumns.ARTICLE_ID + "] INTEGER, " +
                " [" + FaveArticlesColumns.OWNER_ID + "] INTEGER, " +
                " [" + FaveArticlesColumns.ARTICLE + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + FaveArticlesColumns.ARTICLE_ID + "], [" + FaveArticlesColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок товаров
     *
     * @param db БД
     */
    private fun createFaveProductTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FaveProductsColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FaveProductsColumns.PRODUCT_ID + "] INTEGER, " +
                " [" + FaveProductsColumns.OWNER_ID + "] INTEGER, " +
                " [" + FaveProductsColumns.PRODUCT + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + FaveProductsColumns.PRODUCT_ID + "], [" + FaveProductsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок постов
     *
     * @param db БД
     */
    private fun createFavePostsTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FavePostsColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FavePostsColumns.POST_ID + "] INTEGER, " +
                " [" + FavePostsColumns.OWNER_ID + "] INTEGER, " +
                " [" + FavePostsColumns.POST + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + FavePostsColumns.POST_ID + "], [" + FavePostsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок(пользователей) пользователей
     *
     * @param db БД
     */
    private fun createFavePageTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FavePagesColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + FavePagesColumns.DESCRIPTION + "] TEXT, " +
                " [" + FavePagesColumns.UPDATED_TIME + "] INTEGER, " +
                " [" + FavePagesColumns.FAVE_TYPE + "] TEXT, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    private fun createFaveGroupsTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FavePagesColumns.GROUPSTABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + FavePagesColumns.DESCRIPTION + "] TEXT, " +
                " [" + FavePagesColumns.UPDATED_TIME + "] INTEGER, " +
                " [" + FavePagesColumns.FAVE_TYPE + "] TEXT, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок ссылок
     *
     * @param db БД
     */
    private fun createFaveLinksTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FaveLinksColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FaveLinksColumns.LINK_ID + "] TEXT, " +
                " [" + FaveLinksColumns.URL + "] TEXT, " +
                " [" + FaveLinksColumns.TITLE + "] TEXT, " +
                " [" + FaveLinksColumns.DESCRIPTION + "] TEXT, " +
                " [" + FaveLinksColumns.PHOTO + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + FaveLinksColumns.LINK_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы вложений для комментариев
     *
     * @param db БД
     */
    private fun createCommentsAttachmentsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + CommentsAttachmentsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + CommentsAttachmentsColumns.C_ID + "] INTEGER, " +
                " [" + CommentsAttachmentsColumns.DATA + "] BLOB, " +
                //" [" + CommentsAttachmentsColumns.ATTACHMENT_ID + "] INTEGER, " +
                //" [" + CommentsAttachmentsColumns.ATTACHMENT_OWNER_ID + "] INTEGER, " +
                //" CONSTRAINT [] UNIQUE ([" + CommentsAttachmentsColumns.C_ID + "], [" + CommentsAttachmentsColumns.ATTACHMENT_ID + "], [" + CommentsAttachmentsColumns.ATTACHMENT_OWNER_ID + "], [" + CommentsAttachmentsColumns.TYPE + "]) ON CONFLICT REPLACE," +
                " FOREIGN KEY([" + CommentsAttachmentsColumns.C_ID + "]) " +
                " REFERENCES " + CommentsColumns.TABLENAME + "([" + BaseColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);"
        db.execSQL(sql)
    }

    /**
     * Создание таблицы вложений для постов
     *
     * @param db БД
     */
    private fun createPostAttachmentsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + WallsAttachmentsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + WallsAttachmentsColumns.P_ID + "] INTEGER, " +
                " [" + WallsAttachmentsColumns.DATA + "] BLOB, " +
                " FOREIGN KEY([" + WallsAttachmentsColumns.P_ID + "]) " +
                " REFERENCES " + PostsColumns.TABLENAME + "([" + BaseColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);"
        db.execSQL(sql)
    }

    private fun createMessagesTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + MessagesColumns.TABLENAME + "] (\n" +
                " [" + MessagesColumns._ID + "] INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT NOT NULL UNIQUE, " +
                " [" + MessagesColumns.ATTACH_TO + "] INTEGER REFERENCES " + MessagesColumns.TABLENAME + "([" + MessagesColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE, " +
                " [" + MessagesColumns.ORIGINAL_ID + "] INTEGER, " +
                " [" + MessagesColumns.CONVERSATION_MESSAGE_ID + "] INTEGER, " +
                " [" + MessagesColumns.PEER_ID + "] INTEGER, " +
                " [" + MessagesColumns.FROM_ID + "] INTEGER, " +
                " [" + MessagesColumns.RANDOM_ID + "] INTEGER, " +
                " [" + MessagesColumns.DATE + "] INTEGER, " +
                " [" + MessagesColumns.UPDATE_TIME + "] INTEGER, " +
                " [" + MessagesColumns.OUT + "] BOOLEAN, " +
                " [" + MessagesColumns.ENCRYPTED + "] BOOLEAN, " +
                " [" + MessagesColumns.DELETED + "] BOOLEAN, " +
                " [" + MessagesColumns.DELETED_FOR_ALL + "] BOOLEAN, " +
                " [" + MessagesColumns.IMPORTANT + "] BOOLEAN, " +
                " [" + MessagesColumns.FORWARD_COUNT + "] INTEGER, " +
                " [" + MessagesColumns.HAS_ATTACHMENTS + "] BOOLEAN, " +
                " [" + MessagesColumns.STATUS + "] INTEGER, " +
                " [" + MessagesColumns.REACTION_ID + "] INTEGER, " +
                " [" + MessagesColumns.ACTION + "] INTEGER, " +
                " [" + MessagesColumns.ACTION_MID + "] INTEGER, " +
                " [" + MessagesColumns.ACTION_EMAIL + "] TEXT, " +
                " [" + MessagesColumns.ACTION_TEXT + "] TEXT, " +
                " [" + MessagesColumns.PHOTO_50 + "] TEXT, " +
                " [" + MessagesColumns.PHOTO_100 + "] TEXT, " +
                " [" + MessagesColumns.PHOTO_200 + "] TEXT, " +
                " [" + MessagesColumns.TEXT + "] TEXT, " +
                " [" + MessagesColumns.EXTRAS + "] BLOB, " +
                " [" + MessagesColumns.KEYBOARD + "] BLOB, " +
                " [" + MessagesColumns.REACTIONS + "] BLOB, " +
                " [" + MessagesColumns.PAYLOAD + "] TEXT);"
        val insertZeroRow =
            "INSERT INTO " + MessagesColumns.TABLENAME + " (" + MessagesColumns._ID + ") VALUES (0)"
        val insert =
            "INSERT INTO " + MessagesColumns.TABLENAME + " (" + MessagesColumns._ID + ") VALUES (1000000000)"
        val delete =
            "DELETE FROM " + MessagesColumns.TABLENAME + " WHERE " + MessagesColumns._ID + " = 1000000000"
        db.execSQL(create)
        db.execSQL(insertZeroRow)
        db.execSQL(insert)
        db.execSQL(delete)
        //db.execSQL("CREATE INDEX IF NOT EXISTS keys_index ON ${MessagesColumns.TABLENAME} (${MessagesColumns.PEER_ID}, ${MessagesColumns.CONVERSATION_MESSAGE_ID}, ${MessagesColumns.ORIGINAL_ID});")
    }

    private fun createFriendListsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + FriendListsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FriendListsColumns.USER_ID + "] INTEGER, " +
                " [" + FriendListsColumns.LIST_ID + "] INTEGER, " +
                " [" + FriendListsColumns.NAME + "] TEXT, " +
                " CONSTRAINT [] UNIQUE ([" + FriendListsColumns.USER_ID + "], [" + FriendListsColumns.LIST_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createVideosTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + VideosColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + VideosColumns.VIDEO_ID + "] INTEGER, " +
                " [" + VideosColumns.OWNER_ID + "] INTEGER, " +
                " [" + VideosColumns.ORIGINAL_OWNER_ID + "] INTEGER, " +
                " [" + VideosColumns.ALBUM_ID + "] INTEGER, " +
                " [" + VideosColumns.ACCESS_KEY + "] TEXT, " +
                " [" + VideosColumns.TITLE + "] TEXT, " +
                " [" + VideosColumns.DESCRIPTION + "] TEXT, " +
                " [" + VideosColumns.DURATION + "] INTEGER, " +
                " [" + VideosColumns.DATE + "] INTEGER, " +
                " [" + VideosColumns.ADDING_DATE + "] INTEGER, " +
                " [" + VideosColumns.VIEWS + "] INTEGER, " +
                " [" + VideosColumns.PLATFORM + "] TEXT, " +
                " [" + VideosColumns.CAN_EDIT + "] BOOLEAN, " +
                " [" + VideosColumns.CAN_ADD + "] BOOLEAN, " +
                " [" + VideosColumns.IMAGE + "] TEXT, " +
                " [" + VideosColumns.COMMENTS + "] INTEGER, " +
                " [" + VideosColumns.CAN_COMMENT + "] BOOLEAN, " +
                " [" + VideosColumns.IS_PRIVATE + "] BOOLEAN, " +
                " [" + VideosColumns.IS_FAVORITE + "] BOOLEAN, " +
                " [" + VideosColumns.CAN_REPOST + "] BOOLEAN, " +
                " [" + VideosColumns.USER_LIKES + "] BOOLEAN, " +
                " [" + VideosColumns.REPEAT + "] BOOLEAN, " +
                " [" + VideosColumns.LIKES + "] INTEGER, " +
                " [" + VideosColumns.LINK + "] TEXT, " +
                " [" + VideosColumns.PRIVACY_VIEW + "] BLOB, " +
                " [" + VideosColumns.PRIVACY_COMMENT + "] BLOB, " +
                " [" + VideosColumns.MP4_240 + "] TEXT, " +
                " [" + VideosColumns.MP4_360 + "] TEXT, " +
                " [" + VideosColumns.MP4_480 + "] TEXT, " +
                " [" + VideosColumns.MP4_720 + "] TEXT, " +
                " [" + VideosColumns.MP4_1080 + "] TEXT, " +
                " [" + VideosColumns.MP4_1440 + "] TEXT, " +
                " [" + VideosColumns.MP4_2160 + "] TEXT, " +
                " [" + VideosColumns.EXTERNAL + "] TEXT, " +
                " [" + VideosColumns.PLAYER + "] TEXT, " +
                " [" + VideosColumns.HLS + "] TEXT, " +
                " [" + VideosColumns.LIVE + "] TEXT, " +
                " [" + VideosColumns.TRAILER + "] TEXT, " +
                " [" + VideosColumns.TIMELINE_THUMBS + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + VideosColumns.VIDEO_ID + "], [" + VideosColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createDocsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + DocsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + DocsColumns.DOC_ID + "] INTEGER, " +
                " [" + DocsColumns.OWNER_ID + "] INTEGER, " +
                " [" + DocsColumns.ACCESS_KEY + "] TEXT, " +
                " [" + DocsColumns.TITLE + "] TEXT, " +
                " [" + DocsColumns.SIZE + "] INTEGER, " +
                " [" + DocsColumns.EXT + "] TEXT, " +
                " [" + DocsColumns.URL + "] TEXT, " +
                " [" + DocsColumns.DATE + "] INTEGER, " +
                " [" + DocsColumns.TYPE + "] INTEGER, " +
                " [" + DocsColumns.PHOTO + "] BLOB, " +
                " [" + DocsColumns.GRAFFITI + "] BLOB, " +
                " [" + DocsColumns.VIDEO + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + DocsColumns.DOC_ID + "], [" + DocsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createDialogTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + DialogsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT NOT NULL UNIQUE, " +
                " [" + DialogsColumns.MAJOR_ID + "] INTEGER, " +
                " [" + DialogsColumns.MINOR_ID + "] INTEGER, " +
                " [" + DialogsColumns.LAST_MESSAGE_ID + "] INTEGER, " +
                " [" + DialogsColumns.UNREAD + "] INTEGER, " +
                " [" + DialogsColumns.IN_READ + "] INTEGER, " +
                " [" + DialogsColumns.OUT_READ + "] INTEGER, " +
                " [" + DialogsColumns.IS_GROUP_CHANNEL + "] BOOLEAN, " +
                " [" + DialogsColumns.ACL + "] INTEGER, " +
                " [" + DialogsColumns.TITLE + "] TEXT, " +
                " [" + DialogsColumns.PHOTO_50 + "] TEXT, " +
                " [" + DialogsColumns.PHOTO_100 + "] TEXT, " +
                " [" + DialogsColumns.PHOTO_200 + "] TEXT);"
        db.execSQL(sql)
    }

    private fun createPeersTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PeersColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY ON CONFLICT REPLACE NOT NULL UNIQUE, " +
                " [" + PeersColumns.MAJOR_ID + "] INTEGER, " +
                " [" + PeersColumns.MINOR_ID + "] INTEGER, " +
                " [" + PeersColumns.LAST_MESSAGE_ID + "] INTEGER, " +
                " [" + PeersColumns.UNREAD + "] INTEGER, " +
                " [" + PeersColumns.IN_READ + "] INTEGER, " +
                " [" + PeersColumns.OUT_READ + "] INTEGER, " +
                " [" + PeersColumns.IS_GROUP_CHANNEL + "] BOOLEAN, " +
                " [" + PeersColumns.ACL + "] INTEGER, " +
                " [" + PeersColumns.TITLE + "] TEXT, " +
                " [" + PeersColumns.PHOTO_50 + "] TEXT, " +
                " [" + PeersColumns.PHOTO_100 + "] TEXT, " +
                " [" + PeersColumns.PHOTO_200 + "] TEXT, " +
                " [" + PeersColumns.KEYBOARD + "] BLOB, " +
                " [" + PeersColumns.PINNED + "] BLOB);"
        db.execSQL(sql)
    }

    private fun createRelationshipTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + RelationshipsColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + RelationshipsColumns.OBJECT_ID + "] INTEGER NOT NULL, " +
                " [" + RelationshipsColumns.SUBJECT_ID + "] INTEGER NOT NULL, " +
                " [" + RelationshipsColumns.TYPE + "] INTEGER, " +
                " CONSTRAINT [] UNIQUE ([" + RelationshipsColumns.OBJECT_ID + "], [" + RelationshipsColumns.SUBJECT_ID + "], [" + RelationshipsColumns.TYPE + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createUsersTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + UsersColumns.TABLENAME + "](" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + UsersColumns.FIRST_NAME + "] TEXT, " +
                " [" + UsersColumns.LAST_NAME + "] TEXT, " +
                " [" + UsersColumns.MAIDEN_NAME + "] TEXT, " +
                " [" + UsersColumns.DOMAIN + "] TEXT, " +
                " [" + UsersColumns.SEX + "] INTEGER, " +
                " [" + UsersColumns.BDATE + "] TEXT, " +
                " [" + UsersColumns.IS_FRIEND + "] BOOLEAN, " +
                " [" + UsersColumns.FRIEND_STATUS + "] INTEGER, " +
                " [" + UsersColumns.WRITE_MESSAGE_STATUS + "] BOOLEAN, " +
                " [" + UsersColumns.IS_USER_BLACK_LIST + "] BOOLEAN, " +
                " [" + UsersColumns.IS_BLACK_LISTED + "] BOOLEAN, " +
                " [" + UsersColumns.IS_CAN_ACCESS_CLOSED + "] BOOLEAN, " +
                " [" + UsersColumns.IS_VERIFIED + "] BOOLEAN, " +
                " [" + UsersColumns.ONLINE + "] BOOLEAN, " +
                " [" + UsersColumns.ONLINE_MOBILE + "] BOOLEAN, " +
                " [" + UsersColumns.ONLINE_APP + "] INTEGER, " +
                " [" + UsersColumns.HAS_UNSEEN_STORIES + "] BOOLEAN, " +
                " [" + UsersColumns.LAST_SEEN + "] INTEGER, " +
                " [" + UsersColumns.PLATFORM + "] INTEGER, " +
                " [" + UsersColumns.USER_STATUS + "] TEXT, " +
                " [" + UsersColumns.PHOTO_50 + "] TEXT, " +
                " [" + UsersColumns.PHOTO_100 + "] TEXT, " +
                " [" + UsersColumns.PHOTO_200 + "] TEXT, " +
                " [" + UsersColumns.PHOTO_MAX + "] TEXT, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    /*
    private fun createAttachmentsTriggers(db: SQLiteDatabase) {
        var sql =
            "CREATE TRIGGER [t_delete_attachments_wall] AFTER DELETE ON [" + PostsColumns.TABLENAME + "] " +
                    " WHEN [old].[" + PostsColumns.ATTACHMENTS_COUNT + "] > 0 " +
                    " BEGIN " +
                    " DELETE FROM [" + WallAttachmentsColumns.TABLENAME + "] " +
                    " WHERE [" + WallAttachmentsColumns.P_ID + "] = [old].[" + PostsColumns.POST_ID + "] ;" +
                    " END;"
        db.execSQL(sql)

        sql =
            "CREATE TRIGGER [t_delete_attachments_messages] AFTER DELETE ON [" + MessageColumns.TABLENAME + "] " +
                    " WHEN [old].[" + MessageColumns.HAS_ATTACHMENTS + "] > 0 " +
                    " BEGIN " +
                    " DELETE FROM [" + MessagesAttachmentsColumns.TABLENAME + "] " +
                    " WHERE [" + MessagesAttachmentsColumns.M_ID + "] = [old].[" + MessageColumns._ID + "] ;" +
                    " END;"
        db.execSQL(sql)

        sql =
            "CREATE TRIGGER [t_delete_attachments_comments] AFTER DELETE ON [" + CommentsColumns.TABLENAME + "] " +
                    " WHEN [old].[" + CommentsColumns.ATTACHMENTS_COUNT + "] > 0 " +
                    " BEGIN " +
                    " DELETE FROM [" + CommentsAttachmentsColumns.TABLENAME + "] " +
                    " WHERE [" + CommentsAttachmentsColumns.C_ID + "] = [old].[" + CommentsColumns.COMMENT_ID + "] ;" +
                    " END;"
        db.execSQL(sql)
    }
     */

    private fun createNewsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + NewsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + NewsColumns.TYPE + "] TEXT, " +
                " [" + NewsColumns.SOURCE_ID + "] INTEGER, " +
                " [" + NewsColumns.DATE + "] INTEGER, " +
                " [" + NewsColumns.POST_ID + "] INTEGER, " +
                " [" + NewsColumns.POST_TYPE + "] TEXT, " +
                " [" + NewsColumns.FINAL_POST + "] BOOLEAN, " +
                " [" + NewsColumns.COPY_OWNER_ID + "] INTEGER, " +
                " [" + NewsColumns.COPY_POST_ID + "] INTEGER, " +
                " [" + NewsColumns.COPY_POST_DATE + "] INTEGER, " +
                " [" + NewsColumns.CAN_EDIT + "] BOOLEAN, " +
                " [" + NewsColumns.CAN_DELETE + "] BOOLEAN, " +
                " [" + NewsColumns.COMMENT_COUNT + "] INTEGER, " +
                " [" + NewsColumns.COMMENT_CAN_POST + "] BOOLEAN, " +
                " [" + NewsColumns.LIKE_COUNT + "] INTEGER, " +
                " [" + NewsColumns.USER_LIKE + "] BOOLEAN, " +
                " [" + NewsColumns.CAN_LIKE + "] BOOLEAN, " +
                " [" + NewsColumns.CAN_PUBLISH + "] BOOLEAN, " +
                " [" + NewsColumns.REPOSTS_COUNT + "] INTEGER, " +
                " [" + NewsColumns.USER_REPOSTED + "] BOOLEAN, " +
                " [" + NewsColumns.IS_DONUT + "] BOOLEAN, " +
                " [" + NewsColumns.VIEWS + "] INTEGER, " +
                " [" + NewsColumns.TAG_FRIENDS + "] TEXT, " +
                " [" + NewsColumns.TEXT + "] TEXT, " +
                " [" + NewsColumns.COPYRIGHT_BLOB + "] BLOB, " +
                " [" + NewsColumns.ATTACHMENTS_BLOB + "] BLOB);"
        db.execSQL(sql)
    }

    private fun createPostsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PostsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + PostsColumns.POST_ID + "] INTEGER, " +
                " [" + PostsColumns.OWNER_ID + "] INTEGER, " +
                " [" + PostsColumns.FROM_ID + "] INTEGER, " +
                " [" + PostsColumns.DATE + "] INTEGER, " +
                " [" + PostsColumns.REPLY_OWNER_ID + "] INTEGER, " +
                " [" + PostsColumns.REPLY_POST_ID + "] INTEGER, " +
                " [" + PostsColumns.FRIENDS_ONLY + "] BOOLEAN, " +
                " [" + PostsColumns.COMMENTS_COUNT + "] INTEGER, " +
                " [" + PostsColumns.CAN_POST_COMMENT + "] BOOLEAN, " +
                " [" + PostsColumns.LIKES_COUNT + "] INTEGER, " +
                " [" + PostsColumns.USER_LIKES + "] BOOLEAN, " +
                " [" + PostsColumns.CAN_LIKE + "] BOOLEAN, " +
                " [" + PostsColumns.CAN_PUBLISH + "] BOOLEAN, " +
                " [" + PostsColumns.CAN_EDIT + "] BOOLEAN, " +
                " [" + PostsColumns.IS_FAVORITE + "] BOOLEAN, " +
                " [" + PostsColumns.REPOSTS_COUNT + "] INTEGER, " +
                " [" + PostsColumns.USER_REPOSTED + "] BOOLEAN, " +
                " [" + PostsColumns.POST_TYPE + "] TEXT, " +
                " [" + PostsColumns.ATTACHMENTS_COUNT + "] INTEGER, " +
                " [" + PostsColumns.SIGNED_ID + "] INTEGER, " +
                " [" + PostsColumns.CREATED_BY + "] INTEGER, " +
                " [" + PostsColumns.CAN_PIN + "] BOOLEAN, " +
                " [" + PostsColumns.IS_PINNED + "] BOOLEAN, " +
                " [" + PostsColumns.IS_DONUT + "] BOOLEAN, " +
                " [" + PostsColumns.VIEWS + "] INTEGER, " +
                " [" + PostsColumns.DELETED + "] BOOLEAN, " +
                " [" + PostsColumns.TEXT + "] TEXT, " +
                " [" + PostsColumns.COPYRIGHT_BLOB + "] BLOB, " +
                " [" + PostsColumns.POST_SOURCE + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + PostsColumns.POST_ID + "], [" + PostsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createGroupsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + GroupsColumns.TABLENAME + "](" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + GroupsColumns.NAME + "] TEXT, " +
                " [" + GroupsColumns.SCREEN_NAME + "] TEXT, " +
                " [" + GroupsColumns.IS_CLOSED + "] INTEGER, " +
                " [" + GroupsColumns.IS_VERIFIED + "] BOOLEAN, " +
                " [" + GroupsColumns.IS_ADMIN + "] BOOLEAN, " +
                " [" + GroupsColumns.ADMIN_LEVEL + "] INTEGER, " +
                " [" + GroupsColumns.IS_MEMBER + "] BOOLEAN, " +
                " [" + GroupsColumns.MEMBER_STATUS + "] INTEGER, " +
                " [" + GroupsColumns.MEMBERS_COUNT + "] INTEGER, " +
                " [" + GroupsColumns.TYPE + "] INTEGER, " +
                " [" + GroupsColumns.CAN_ADD_TOPICS + "] BOOLEAN, " +
                " [" + GroupsColumns.TOPICS_ORDER + "] BOOLEAN, " +
                " [" + GroupsColumns.IS_BLACK_LISTED + "] BOOLEAN, " +
                " [" + GroupsColumns.HAS_UNSEEN_STORIES + "] BOOLEAN, " +
                " [" + GroupsColumns.PHOTO_50 + "] TEXT, " +
                " [" + GroupsColumns.PHOTO_100 + "] TEXT, " +
                " [" + GroupsColumns.PHOTO_200 + "] TEXT, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createGroupsDetTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + GroupsDetailsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + GroupsDetailsColumns.DATA + "] BLOB, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createUserDetTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + UsersDetailsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + UsersDetailsColumns.DATA + "] BLOB, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createPhotoAlbumsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PhotoAlbumsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + PhotoAlbumsColumns.ALBUM_ID + "] INTEGER, " +
                " [" + PhotoAlbumsColumns.OWNER_ID + "] INTEGER, " +
                " [" + PhotoAlbumsColumns.TITLE + "] TEXT, " +
                " [" + PhotoAlbumsColumns.DESCRIPTION + "] TEXT, " +
                " [" + PhotoAlbumsColumns.SIZE + "] INTEGER, " +
                " [" + PhotoAlbumsColumns.CAN_UPLOAD + "] BOOLEAN, " +
                " [" + PhotoAlbumsColumns.UPDATED + "] INTEGER, " +
                " [" + PhotoAlbumsColumns.CREATED + "] INTEGER, " +
                " [" + PhotoAlbumsColumns.UPLOAD_BY_ADMINS + "] BOOLEAN, " +
                " [" + PhotoAlbumsColumns.COMMENTS_DISABLED + "] BOOLEAN, " +
                " [" + PhotoAlbumsColumns.PRIVACY_VIEW + "] BLOB, " +
                " [" + PhotoAlbumsColumns.PRIVACY_COMMENT + "] BLOB, " +
                " [" + PhotoAlbumsColumns.SIZES + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + PhotoAlbumsColumns.ALBUM_ID + "], [" + PhotoAlbumsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createVideoAlbumsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + VideoAlbumsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + VideoAlbumsColumns.ALBUM_ID + "] INTEGER, " +
                " [" + VideoAlbumsColumns.OWNER_ID + "] INTEGER, " +
                " [" + VideoAlbumsColumns.TITLE + "] TEXT, " +
                " [" + VideoAlbumsColumns.COUNT + "] INTEGER, " +
                " [" + VideoAlbumsColumns.IMAGE + "] TEXT, " +
                " [" + VideoAlbumsColumns.UPDATE_TIME + "] INTEGER, " +
                " [" + VideoAlbumsColumns.PRIVACY + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + VideoAlbumsColumns.ALBUM_ID + "], [" + VideoAlbumsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createTopicsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + TopicsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + TopicsColumns.TOPIC_ID + "] INTEGER, " +
                " [" + TopicsColumns.OWNER_ID + "] INTEGER, " +
                " [" + TopicsColumns.TITLE + "] TEXT, " +
                " [" + TopicsColumns.CREATED + "] INTEGER, " +
                " [" + TopicsColumns.CREATED_BY + "] INTEGER, " +
                " [" + TopicsColumns.UPDATED + "] INTEGER, " +
                " [" + TopicsColumns.UPDATED_BY + "] INTEGER, " +
                " [" + TopicsColumns.IS_CLOSED + "] BOOLEAN, " +
                " [" + TopicsColumns.IS_FIXED + "] BOOLEAN, " +
                " [" + TopicsColumns.COMMENTS + "] INTEGER, " +
                " [" + TopicsColumns.FIRST_COMMENT + "] TEXT, " +
                " [" + TopicsColumns.LAST_COMMENT + "] TEXT, " +
                " [" + TopicsColumns.ATTACHED_POLL + "] BLOB, " +
                " CONSTRAINT [] UNIQUE ([" + TopicsColumns.TOPIC_ID + "], [" + TopicsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createNotificationsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + NotificationsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + NotificationsColumns.DATE + "] INTEGER, " +
                " [" + NotificationsColumns.CONTENT_PACK + "] BLOB);"
        db.execSQL(sql)
    }

    private fun createFeedListsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + FeedListsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + FeedListsColumns.TITLE + "] TEXT, " +
                " [" + FeedListsColumns.NO_REPOSTS + "] BOOLEAN, " +
                " [" + FeedListsColumns.SOURCE_IDS + "] TEXT, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    companion object {
        const val TAG = "DBHelper"
        private val dbHelperMap: MutableMap<Long, DBHelper> = ConcurrentHashMap()

        @Synchronized
        fun getInstance(context: Context, aid: Long): DBHelper {
            var helper = dbHelperMap[aid]
            if (helper == null) {
                helper = DBHelper(context, aid)
                dbHelperMap[aid] = helper
            }
            return helper
        }

        fun removeDatabaseFor(context: Context, aid: Long) {
            dbHelperMap.remove(aid)
            context.deleteDatabase(getDatabaseFileName(aid))
        }

        internal fun getDatabaseFileName(aid: Long): String {
            return if (!FenrirNative.isNativeLoaded) "fenrir_$aid.sqlite" else "fenrir_lz4_$aid.sqlite"
        }
    }
}