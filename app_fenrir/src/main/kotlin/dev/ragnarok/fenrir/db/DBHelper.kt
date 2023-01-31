package dev.ragnarok.fenrir.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.db.column.CommentsColumns
import dev.ragnarok.fenrir.db.column.CountriesColumns
import dev.ragnarok.fenrir.db.column.DialogsColumns
import dev.ragnarok.fenrir.db.column.DocColumns
import dev.ragnarok.fenrir.db.column.FaveArticlesColumns
import dev.ragnarok.fenrir.db.column.FaveLinksColumns
import dev.ragnarok.fenrir.db.column.FavePageColumns
import dev.ragnarok.fenrir.db.column.FavePhotosColumns
import dev.ragnarok.fenrir.db.column.FavePostsColumns
import dev.ragnarok.fenrir.db.column.FaveProductColumns
import dev.ragnarok.fenrir.db.column.FaveVideosColumns
import dev.ragnarok.fenrir.db.column.FeedListsColumns
import dev.ragnarok.fenrir.db.column.FriendListsColumns
import dev.ragnarok.fenrir.db.column.GroupColumns
import dev.ragnarok.fenrir.db.column.GroupsDetColumns
import dev.ragnarok.fenrir.db.column.KeyColumns
import dev.ragnarok.fenrir.db.column.MessageColumns
import dev.ragnarok.fenrir.db.column.NewsColumns
import dev.ragnarok.fenrir.db.column.NotificationColumns
import dev.ragnarok.fenrir.db.column.PeersColumns
import dev.ragnarok.fenrir.db.column.PhotoAlbumsColumns
import dev.ragnarok.fenrir.db.column.PhotosColumns
import dev.ragnarok.fenrir.db.column.PhotosExtendedColumns
import dev.ragnarok.fenrir.db.column.PostsColumns
import dev.ragnarok.fenrir.db.column.RelationshipColumns
import dev.ragnarok.fenrir.db.column.TopicsColumns
import dev.ragnarok.fenrir.db.column.UserColumns
import dev.ragnarok.fenrir.db.column.UsersDetColumns
import dev.ragnarok.fenrir.db.column.VideoAlbumsColumns
import dev.ragnarok.fenrir.db.column.VideoColumns
import dev.ragnarok.fenrir.db.column.attachments.CommentsAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.MessagesAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.WallAttachmentsColumns
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
        db.execSQL("ALTER TABLE $tableName ADD $column $type $defaultValue;")
    }

    private fun dropColumn(db: SQLiteDatabase, tableName: String, column: String) {
        db.execSQL("ALTER TABLE $tableName DROP COLUMN $column;")
    }
     */

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != Constants.DATABASE_FENRIR_VERSION) {
            dropAllTables(db)
            onCreate(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != Constants.DATABASE_FENRIR_VERSION) {
            dropAllTables(db)
            onCreate(db)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        createUsersTable(db)
        createMessagesTable(db)
        createZeroMessageProtectionTriggers(db)
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
        createRelativeshipTable(db)
        createCommentsTable(db)
        createCommentsAttachmentsTable(db)
        createPhotoAlbumsTable(db)
        createNewsTable(db)
        createGroupsDetTable(db)
        createVideoAlbumsTable(db)
        createTopicsTable(db)
        createNotoficationsTable(db)
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
        //createDeleteWallCopyHistoryTrigger(db)
        createKeysTableIfNotExist(db)
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        db.beginTransaction()

        // сначала удаляем триггеры, потому что они зависят от таблиц
        //db.execSQL("DROP TRIGGER IF EXISTS t_delete_wall_copy_history");
        //db.execSQL("DROP TRIGGER IF EXISTS t_delete_feed_copy_history");
        db.execSQL("DROP TABLE IF EXISTS " + MessagesAttachmentsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + CommentsAttachmentsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + CommentsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + DialogsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + PeersColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + DocColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + GroupColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + GroupsDetColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS links")

        //messages
        db.execSQL("DROP TRIGGER IF EXISTS zero_msg_upd")
        db.execSQL("DROP TRIGGER IF EXISTS zero_msg_del")
        db.execSQL("DROP TABLE IF EXISTS " + MessageColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS news_attachments")
        db.execSQL("DROP TABLE IF EXISTS " + NewsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + PhotoAlbumsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + PhotosColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + PhotosExtendedColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS polls")
        db.execSQL("DROP TABLE IF EXISTS " + WallAttachmentsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + PostsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + RelationshipColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + UserColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + VideoAlbumsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + VideoColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + TopicsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + NotificationColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + UsersDetColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FavePhotosColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FaveArticlesColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FaveProductColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FaveVideosColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FavePageColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FavePageColumns.GROUPSTABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FaveLinksColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FavePostsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + "peers")
        db.execSQL("DROP TABLE IF EXISTS " + CountriesColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FeedListsColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + FriendListsColumns.TABLENAME)
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    private fun createKeysTableIfNotExist(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + KeyColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + KeyColumns.VERSION + "] INTEGER, " +
                "  [" + KeyColumns.PEER_ID + "] INTEGER, " +
                "  [" + KeyColumns.SESSION_ID + "] INTEGER, " +
                "  [" + KeyColumns.DATE + "] INTEGER, " +
                "  [" + KeyColumns.START_SESSION_MESSAGE_ID + "] INTEGER, " +
                "  [" + KeyColumns.END_SESSION_MESSAGE_ID + "] INTEGER, " +
                "  [" + KeyColumns.OUT_KEY + "] TEXT, " +
                "  [" + KeyColumns.IN_KEY + "] TEXT," +
                "  CONSTRAINT [] UNIQUE ([" + KeyColumns.SESSION_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createZeroMessageProtectionTriggers(db: SQLiteDatabase) {
        val sqlUpdate =
            "CREATE TRIGGER zero_msg_upd BEFORE UPDATE ON " + MessageColumns.TABLENAME + " FOR EACH ROW " +
                    "WHEN OLD." + MessageColumns._ID + " = 0 BEGIN " +
                    "   SELECT RAISE(ABORT, 'Cannot update record with _id=0');" +
                    "END;"
        val sqlDelete =
            "CREATE TRIGGER zero_msg_del BEFORE DELETE ON " + MessageColumns.TABLENAME + " FOR EACH ROW " +
                    "WHEN OLD." + MessageColumns._ID + " = 0 BEGIN " +
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
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + PhotosColumns.PHOTO_ID + "] INTEGER, " +
                "  [" + PhotosColumns.ALBUM_ID + "] INTEGER, " +
                "  [" + PhotosColumns.OWNER_ID + "] INTEGER, " +
                "  [" + PhotosColumns.WIDTH + "] INTEGER, " +
                "  [" + PhotosColumns.HEIGHT + "] INTEGER, " +
                "  [" + PhotosColumns.TEXT + "] TEXT, " +
                "  [" + PhotosColumns.DATE + "] INTEGER, " +
                "  [" + PhotosColumns.SIZES + "] BLOB, " +
                "  [" + PhotosColumns.USER_LIKES + "] BOOLEAN, " +
                "  [" + PhotosColumns.CAN_COMMENT + "] BOOLEAN, " +
                "  [" + PhotosColumns.LIKES + "] INTEGER, " +
                "  [" + PhotosColumns.REPOSTS + "] INTEGER, " +
                "  [" + PhotosColumns.COMMENTS + "] INTEGER, " +
                "  [" + PhotosColumns.TAGS + "] INTEGER, " +
                "  [" + PhotosColumns.ACCESS_KEY + "] TEXT, " +
                "  [" + PhotosColumns.DELETED + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + PhotosColumns.PHOTO_ID + "], [" + PhotosColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createPhotoExtendedTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PhotosExtendedColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + PhotosExtendedColumns.DB_OWNER_ID + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.DB_ALBUM_ID + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.PHOTO_ID + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.ALBUM_ID + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.OWNER_ID + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.WIDTH + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.HEIGHT + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.TEXT + "] TEXT, " +
                "  [" + PhotosExtendedColumns.DATE + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.SIZES + "] BLOB, " +
                "  [" + PhotosExtendedColumns.USER_LIKES + "] BOOLEAN, " +
                "  [" + PhotosExtendedColumns.CAN_COMMENT + "] BOOLEAN, " +
                "  [" + PhotosExtendedColumns.LIKES + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.REPOSTS + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.COMMENTS + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.TAGS + "] INTEGER, " +
                "  [" + PhotosExtendedColumns.ACCESS_KEY + "] TEXT, " +
                "  [" + PhotosExtendedColumns.DELETED + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + PhotosExtendedColumns.PHOTO_ID + "], [" + PhotosExtendedColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
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
                " REFERENCES " + MessageColumns.TABLENAME + "([" + MessageColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);"
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
                " [" + CommentsColumns.DATE + "] INTEGER, " +
                " [" + CommentsColumns.TEXT + "] TEXT, " +
                " [" + CommentsColumns.REPLY_TO_USER + "] INTEGER, " +
                " [" + CommentsColumns.REPLY_TO_COMMENT + "] INTEGER, " +
                " [" + CommentsColumns.THREADS_COUNT + "] INTEGER, " +
                " [" + CommentsColumns.THREADS + "] BLOB, " +
                " [" + CommentsColumns.LIKES + "] INTEGER, " +
                " [" + CommentsColumns.USER_LIKES + "] BOOLEAN, " +
                " [" + CommentsColumns.CAN_LIKE + "] BOOLEAN, " +
                " [" + CommentsColumns.CAN_EDIT + "] BOOLEAN, " +
                " [" + CommentsColumns.ATTACHMENTS_COUNT + "] INTEGER, " +
                " [" + CommentsColumns.DELETED + "] BOOLEAN, " +
                " [" + CommentsColumns.SOURCE_ID + "] INTEGER, " +
                " [" + CommentsColumns.SOURCE_OWNER_ID + "] INTEGER, " +
                " [" + CommentsColumns.SOURCE_TYPE + "] INTEGER, " +
                " [" + CommentsColumns.SOURCE_ACCESS_KEY + "] TEXT, " +
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
                " [" + FaveVideosColumns.VIDEO + "] BLOB);"
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
                " [" + FaveArticlesColumns.ARTICLE + "] BLOB);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок товаров
     *
     * @param db БД
     */
    private fun createFaveProductTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FaveProductColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + FaveProductColumns.PRODUCT + "] BLOB);"
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
                " [" + FavePostsColumns.POST + "] BLOB);"
        db.execSQL(create)
    }

    /**
     * Создание таблицы закладок(пользователей) пользователей
     *
     * @param db БД
     */
    private fun createFavePageTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FavePageColumns.TABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + FavePageColumns.DESCRIPTION + "] TEXT, " +
                " [" + FavePageColumns.UPDATED_TIME + "] INTEGER, " +
                " [" + FavePageColumns.FAVE_TYPE + "] TEXT, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(create)
    }

    private fun createFaveGroupsTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + FavePageColumns.GROUPSTABLENAME + "] (" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + FavePageColumns.DESCRIPTION + "] TEXT, " +
                " [" + FavePageColumns.UPDATED_TIME + "] INTEGER, " +
                " [" + FavePageColumns.FAVE_TYPE + "] TEXT, " +
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
        val sql = "CREATE TABLE [" + WallAttachmentsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " [" + WallAttachmentsColumns.P_ID + "] INTEGER, " +
                " [" + WallAttachmentsColumns.DATA + "] BLOB, " +
                " FOREIGN KEY([" + WallAttachmentsColumns.P_ID + "]) " +
                " REFERENCES " + PostsColumns.TABLENAME + "([" + BaseColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE);"
        db.execSQL(sql)
    }

    private fun createMessagesTable(db: SQLiteDatabase) {
        val create = "CREATE TABLE [" + MessageColumns.TABLENAME + "] (\n" +
                " [" + MessageColumns._ID + "] INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT NOT NULL UNIQUE, " +
                " [" + MessageColumns.PEER_ID + "] INTEGER, " +
                " [" + MessageColumns.FROM_ID + "] INTEGER, " +
                " [" + MessageColumns.DATE + "] INTEGER, " +
                " [" + MessageColumns.OUT + "] BOOLEAN, " +
                " [" + MessageColumns.BODY + "] TEXT, " +
                " [" + MessageColumns.ENCRYPTED + "] BOOLEAN, " +
                " [" + MessageColumns.DELETED + "] BOOLEAN, " +
                " [" + MessageColumns.DELETED_FOR_ALL + "] BOOLEAN, " +
                " [" + MessageColumns.IMPORTANT + "] BOOLEAN, " +
                " [" + MessageColumns.FORWARD_COUNT + "] INTEGER, " +
                " [" + MessageColumns.HAS_ATTACHMENTS + "] BOOLEAN, " +
                " [" + MessageColumns.ATTACH_TO + "] INTEGER REFERENCES " + MessageColumns.TABLENAME + "([" + MessageColumns._ID + "]) ON DELETE CASCADE ON UPDATE CASCADE, " +
                " [" + MessageColumns.STATUS + "] INTEGER, " +
                " [" + MessageColumns.UPDATE_TIME + "] INTEGER, " +
                " [" + MessageColumns.ACTION + "] INTEGER, " +
                " [" + MessageColumns.ACTION_MID + "] INTEGER, " +
                " [" + MessageColumns.ACTION_EMAIL + "] TEXT, " +
                " [" + MessageColumns.ACTION_TEXT + "] TEXT, " +
                " [" + MessageColumns.PHOTO_50 + "] TEXT, " +
                " [" + MessageColumns.PHOTO_100 + "] TEXT, " +
                " [" + MessageColumns.PHOTO_200 + "] TEXT, " +
                " [" + MessageColumns.RANDOM_ID + "] INTEGER, " +
                " [" + MessageColumns.EXTRAS + "] BLOB, " +
                " [" + MessageColumns.ORIGINAL_ID + "] INTEGER, " +
                " [" + MessageColumns.KEYBOARD + "] BLOB, " +
                " [" + MessageColumns.PAYLOAD + "] TEXT);"
        val insertZeroRow =
            "INSERT INTO " + MessageColumns.TABLENAME + " (" + MessageColumns._ID + ") VALUES (0)"
        val insert =
            "INSERT INTO " + MessageColumns.TABLENAME + " (" + MessageColumns._ID + ") VALUES (1000000000)"
        val delete =
            "DELETE FROM " + MessageColumns.TABLENAME + " WHERE " + MessageColumns._ID + " = 1000000000"
        db.execSQL(create)
        db.execSQL(insertZeroRow)
        db.execSQL(insert)
        db.execSQL(delete)
    }

    private fun createFriendListsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + FriendListsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + FriendListsColumns.USER_ID + "] INTEGER, " +
                "  [" + FriendListsColumns.LIST_ID + "] INTEGER, " +
                "  [" + FriendListsColumns.NAME + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + FriendListsColumns.USER_ID + "], [" + FriendListsColumns.LIST_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createVideosTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + VideoColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + VideoColumns.VIDEO_ID + "] INTEGER, " +
                "  [" + VideoColumns.OWNER_ID + "] INTEGER, " +
                "  [" + VideoColumns.ORIGINAL_OWNER_ID + "] INTEGER, " +
                "  [" + VideoColumns.ALBUM_ID + "] INTEGER, " +
                "  [" + VideoColumns.TITLE + "] TEXT, " +
                "  [" + VideoColumns.DESCRIPTION + "] TEXT, " +
                "  [" + VideoColumns.DURATION + "] INTEGER, " +
                "  [" + VideoColumns.LINK + "] TEXT, " +
                "  [" + VideoColumns.DATE + "] INTEGER, " +
                "  [" + VideoColumns.ADDING_DATE + "] INTEGER, " +
                "  [" + VideoColumns.VIEWS + "] INTEGER, " +
                "  [" + VideoColumns.PLAYER + "] TEXT, " +
                "  [" + VideoColumns.IMAGE + "] TEXT, " +
                "  [" + VideoColumns.ACCESS_KEY + "] TEXT, " +
                "  [" + VideoColumns.COMMENTS + "] INTEGER, " +
                "  [" + VideoColumns.CAN_COMMENT + "] BOOLEAN, " +
                "  [" + VideoColumns.IS_PRIVATE + "] BOOLEAN, " +
                "  [" + VideoColumns.IS_FAVORITE + "] BOOLEAN, " +
                "  [" + VideoColumns.CAN_REPOST + "] INTEGER, " +
                "  [" + VideoColumns.USER_LIKES + "] INTEGER, " +
                "  [" + VideoColumns.REPEAT + "] INTEGER, " +
                "  [" + VideoColumns.LIKES + "] INTEGER, " +
                "  [" + VideoColumns.PRIVACY_VIEW + "] BLOB, " +
                "  [" + VideoColumns.PRIVACY_COMMENT + "] BLOB, " +
                "  [" + VideoColumns.MP4_240 + "] TEXT, " +
                "  [" + VideoColumns.MP4_360 + "] TEXT, " +
                "  [" + VideoColumns.MP4_480 + "] TEXT, " +
                "  [" + VideoColumns.MP4_720 + "] TEXT, " +
                "  [" + VideoColumns.MP4_1080 + "] TEXT, " +
                "  [" + VideoColumns.MP4_1440 + "] TEXT, " +
                "  [" + VideoColumns.MP4_2160 + "] TEXT, " +
                "  [" + VideoColumns.EXTERNAL + "] TEXT, " +
                "  [" + VideoColumns.HLS + "] TEXT, " +
                "  [" + VideoColumns.LIVE + "] TEXT, " +
                "  [" + VideoColumns.PLATFORM + "] TEXT, " +
                "  [" + VideoColumns.CAN_EDIT + "] BOOLEAN, " +
                "  [" + VideoColumns.CAN_ADD + "] BOOLEAN, " +
                "  CONSTRAINT [] UNIQUE ([" + VideoColumns.VIDEO_ID + "], [" + VideoColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createDocsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + DocColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + DocColumns.DOC_ID + "] INTEGER, " +
                "  [" + DocColumns.OWNER_ID + "] INTEGER, " +
                "  [" + DocColumns.TITLE + "] TEXT, " +
                "  [" + DocColumns.SIZE + "] INTEGER, " +
                "  [" + DocColumns.EXT + "] TEXT, " +
                "  [" + DocColumns.URL + "] TEXT, " +
                "  [" + DocColumns.DATE + "] INTEGER, " +
                "  [" + DocColumns.TYPE + "] INTEGER, " +
                "  [" + DocColumns.PHOTO + "] BLOB, " +
                "  [" + DocColumns.GRAFFITI + "] BLOB, " +
                "  [" + DocColumns.VIDEO + "] BLOB, " +
                "  [" + DocColumns.ACCESS_KEY + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + DocColumns.DOC_ID + "], [" + DocColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createDialogTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + DialogsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY ON CONFLICT REPLACE AUTOINCREMENT NOT NULL UNIQUE, " +
                "  [" + DialogsColumns.UNREAD + "] INTEGER, " +
                "  [" + DialogsColumns.TITLE + "] TEXT, " +
                "  [" + DialogsColumns.IN_READ + "] INTEGER, " +
                "  [" + DialogsColumns.OUT_READ + "] INTEGER, " +
                "  [" + DialogsColumns.ACL + "] INTEGER, " +
                "  [" + DialogsColumns.PHOTO_50 + "] TEXT, " +
                "  [" + DialogsColumns.PHOTO_100 + "] TEXT, " +
                "  [" + DialogsColumns.PHOTO_200 + "] TEXT, " +
                "  [" + DialogsColumns.MAJOR_ID + "] INTEGER, " +
                "  [" + DialogsColumns.MINOR_ID + "] INTEGER, " +
                "  [" + DialogsColumns.LAST_MESSAGE_ID + "] INTEGER, " +
                "  [" + DialogsColumns.IS_GROUP_CHANNEL + "] BOOLEAN);"
        db.execSQL(sql)
    }

    private fun createPeersTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PeersColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY ON CONFLICT REPLACE NOT NULL UNIQUE, " +
                "  [" + PeersColumns.UNREAD + "] INTEGER, " +
                "  [" + PeersColumns.TITLE + "] TEXT, " +
                "  [" + PeersColumns.IN_READ + "] INTEGER, " +
                "  [" + PeersColumns.OUT_READ + "] INTEGER, " +
                "  [" + PeersColumns.PINNED + "] BLOB, " +
                "  [" + PeersColumns.LAST_MESSAGE_ID + "] INTEGER, " +
                "  [" + PeersColumns.ACL + "] INTEGER, " +
                "  [" + PeersColumns.PHOTO_50 + "] TEXT, " +
                "  [" + PeersColumns.PHOTO_100 + "] TEXT, " +
                "  [" + PeersColumns.PHOTO_200 + "] TEXT, " +
                "  [" + PeersColumns.KEYBOARD + "] BLOB, " +
                "  [" + PeersColumns.MAJOR_ID + "] INTEGER, " +
                "  [" + PeersColumns.MINOR_ID + "] INTEGER, " +
                "  [" + PeersColumns.IS_GROUP_CHANNEL + "] BOOLEAN);"
        db.execSQL(sql)
    }

    private fun createRelativeshipTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + RelationshipColumns.TABLENAME + "] (" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + RelationshipColumns.OBJECT_ID + "] INTEGER NOT NULL, " +
                "  [" + RelationshipColumns.SUBJECT_ID + "] INTEGER NOT NULL, " +
                "  [" + RelationshipColumns.TYPE + "] INTEGER, " +
                "  CONSTRAINT [] UNIQUE ([" + RelationshipColumns.OBJECT_ID + "], [" + RelationshipColumns.SUBJECT_ID + "], [" + RelationshipColumns.TYPE + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createUsersTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + UserColumns.TABLENAME + "](" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + UserColumns.FIRST_NAME + "] TEXT, " +
                " [" + UserColumns.LAST_NAME + "] TEXT, " +
                " [" + UserColumns.ONLINE + "] BOOLEAN, " +
                " [" + UserColumns.ONLINE_MOBILE + "] BOOLEAN, " +
                " [" + UserColumns.ONLINE_APP + "] INTEGER, " +
                " [" + UserColumns.PHOTO_50 + "] TEXT, " +
                " [" + UserColumns.PHOTO_100 + "] TEXT, " +
                " [" + UserColumns.PHOTO_200 + "] TEXT, " +
                " [" + UserColumns.PHOTO_MAX + "] TEXT, " +
                " [" + UserColumns.LAST_SEEN + "] INTEGER, " +
                " [" + UserColumns.PLATFORM + "] INTEGER, " +
                " [" + UserColumns.USER_STATUS + "] TEXT, " +
                " [" + UserColumns.SEX + "] INTEGER, " +
                " [" + UserColumns.DOMAIN + "] TEXT, " +
                " [" + UserColumns.MAIDEN_NAME + "] TEXT, " +
                " [" + UserColumns.BDATE + "] TEXT, " +
                " [" + UserColumns.IS_FRIEND + "] BOOLEAN, " +
                " [" + UserColumns.FRIEND_STATUS + "] INTEGER, " +
                " [" + UserColumns.WRITE_MESSAGE_STATUS + "] BOOLEAN, " +
                " [" + UserColumns.IS_USER_BLACK_LIST + "] BOOLEAN, " +
                " [" + UserColumns.IS_BLACK_LISTED + "] BOOLEAN, " +
                " [" + UserColumns.IS_CAN_ACCESS_CLOSED + "] BOOLEAN, " +
                " [" + UserColumns.IS_VERIFIED + "] BOOLEAN, " +
                " [" + UserColumns.HAS_UNSEEN_STORIES + "] BOOLEAN, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    /* Триггер, который удаляет историю репостов при удалении самого поста */ /*private void createDeleteWallCopyHistoryTrigger(SQLiteDatabase db) {
        String sql = "CREATE TRIGGER [t_delete_wall_copy_history] AFTER DELETE ON [" + PostsColumns.TABLENAME + "] " +
                " WHEN [old].[" + PostsColumns.HAS_COPY_HISTORY + "] = 1 " +
                " BEGIN " +
                " DELETE FROM [" + PostsColumns.TABLENAME + "] " +
                " WHERE [" + PostsColumns.COPY_HISTORY_OF + "] = [old].[" + PostsColumns._ID + "] " +
                " AND [" + PostsColumns.COPY_PARENT_TYPE + "] = " + PostsColumns.COPY_PARENT_TYPE_WALL + ";" +
                " END;";
        db.execSQL(sql);
    }*/

    private fun createNewsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + NewsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + NewsColumns.TYPE + "] TEXT, " +
                "  [" + NewsColumns.SOURCE_ID + "] INTEGER, " +
                "  [" + NewsColumns.DATE + "] INTEGER, " +
                "  [" + NewsColumns.POST_ID + "] INTEGER, " +
                "  [" + NewsColumns.POST_TYPE + "] TEXT, " +
                "  [" + NewsColumns.FINAL_POST + "] BOOLEAN, " +
                "  [" + NewsColumns.COPY_OWNER_ID + "] INTEGER, " +
                "  [" + NewsColumns.COPY_POST_ID + "] INTEGER, " +
                "  [" + NewsColumns.COPY_POST_DATE + "] INTEGER, " +
                "  [" + NewsColumns.TEXT + "] TEXT, " +
                "  [" + NewsColumns.CAN_EDIT + "] BOOLEAN, " +
                "  [" + NewsColumns.CAN_DELETE + "] BOOLEAN, " +
                "  [" + NewsColumns.COMMENT_COUNT + "] INTEGER, " +
                "  [" + NewsColumns.COMMENT_CAN_POST + "] BOOLEAN, " +
                "  [" + NewsColumns.LIKE_COUNT + "] INTEGER, " +
                "  [" + NewsColumns.USER_LIKE + "] BOOLEAN, " +
                "  [" + NewsColumns.CAN_LIKE + "] BOOLEAN, " +
                "  [" + NewsColumns.CAN_PUBLISH + "] BOOLEAN, " +
                "  [" + NewsColumns.REPOSTS_COUNT + "] INTEGER, " +
                "  [" + NewsColumns.USER_REPOSTED + "] BOOLEAN, " +
                "  [" + NewsColumns.ATTACHMENTS_BLOB + "] BLOB, " +
                "  [" + NewsColumns.COPYRIGHT_BLOB + "] BLOB, " +
                "  [" + NewsColumns.VIEWS + "] INTEGER, " +
                "  [" + NewsColumns.TAG_FRIENDS + "] TEXT);"
        db.execSQL(sql)
    }

    private fun createPostsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PostsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + PostsColumns.POST_ID + "] INTEGER, " +
                "  [" + PostsColumns.OWNER_ID + "] INTEGER, " +
                "  [" + PostsColumns.FROM_ID + "] INTEGER, " +
                "  [" + PostsColumns.DATE + "] INTEGER, " +
                "  [" + PostsColumns.TEXT + "] TEXT, " +
                "  [" + PostsColumns.REPLY_OWNER_ID + "] INTEGER, " +
                "  [" + PostsColumns.REPLY_POST_ID + "] INTEGER, " +
                "  [" + PostsColumns.FRIENDS_ONLY + "] BOOLEAN, " +
                "  [" + PostsColumns.COMMENTS_COUNT + "] INTEGER, " +
                "  [" + PostsColumns.CAN_POST_COMMENT + "] BOOLEAN, " +
                "  [" + PostsColumns.LIKES_COUNT + "] INTEGER, " +
                "  [" + PostsColumns.USER_LIKES + "] BOOLEAN, " +
                "  [" + PostsColumns.CAN_LIKE + "] BOOLEAN, " +
                "  [" + PostsColumns.CAN_PUBLISH + "] BOOLEAN, " +
                "  [" + PostsColumns.CAN_EDIT + "] BOOLEAN, " +
                "  [" + PostsColumns.IS_FAVORITE + "] BOOLEAN, " +
                "  [" + PostsColumns.REPOSTS_COUNT + "] INTEGER, " +
                "  [" + PostsColumns.USER_REPOSTED + "] BOOLEAN, " +
                "  [" + PostsColumns.POST_TYPE + "] TEXT, " +
                "  [" + PostsColumns.ATTACHMENTS_COUNT + "] INTEGER, " +
                "  [" + PostsColumns.SIGNED_ID + "] INTEGER, " +
                "  [" + PostsColumns.CREATED_BY + "] INTEGER, " +
                "  [" + PostsColumns.CAN_PIN + "] BOOLEAN, " +
                "  [" + PostsColumns.IS_PINNED + "] BOOLEAN, " +
                "  [" + PostsColumns.IS_DONUT + "] BOOLEAN, " +
                "  [" + PostsColumns.DELETED + "] BOOLEAN, " +
                "  [" + PostsColumns.POST_SOURCE + "] BLOB, " +
                "  [" + PostsColumns.COPYRIGHT_BLOB + "] BLOB, " +
                "  [" + PostsColumns.VIEWS + "] INTEGER, " +
                "  CONSTRAINT [] UNIQUE ([" + PostsColumns.POST_ID + "], [" + PostsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createGroupsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + GroupColumns.TABLENAME + "](" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + GroupColumns.NAME + "] TEXT, " +
                " [" + GroupColumns.SCREEN_NAME + "] TEXT, " +
                " [" + GroupColumns.IS_CLOSED + "] INTEGER, " +
                " [" + GroupColumns.IS_VERIFIED + "] BOOLEAN, " +
                " [" + GroupColumns.IS_ADMIN + "] BOOLEAN, " +
                " [" + GroupColumns.ADMIN_LEVEL + "] INTEGER, " +
                " [" + GroupColumns.IS_MEMBER + "] BOOLEAN, " +
                " [" + GroupColumns.MEMBER_STATUS + "] INTEGER, " +
                " [" + GroupColumns.MEMBERS_COUNT + "] INTEGER, " +
                " [" + GroupColumns.TYPE + "] INTEGER, " +
                " [" + GroupColumns.PHOTO_50 + "] TEXT, " +
                " [" + GroupColumns.PHOTO_100 + "] TEXT, " +
                " [" + GroupColumns.PHOTO_200 + "] TEXT, " +
                " [" + GroupColumns.CAN_ADD_TOPICS + "] BOOLEAN, " +
                " [" + GroupColumns.TOPICS_ORDER + "] BOOLEAN, " +
                " [" + GroupColumns.IS_BLACK_LISTED + "] BOOLEAN, " +
                " [" + GroupColumns.HAS_UNSEEN_STORIES + "] BOOLEAN, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createGroupsDetTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + GroupsDetColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + GroupsDetColumns.DATA + "] BLOB, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createUserDetTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + UsersDetColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE, " +
                " [" + UsersDetColumns.DATA + "] BLOB, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createPhotoAlbumsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + PhotoAlbumsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + PhotoAlbumsColumns.ALBUM_ID + "] INTEGER, " +
                "  [" + PhotoAlbumsColumns.OWNER_ID + "] INTEGER, " +
                "  [" + PhotoAlbumsColumns.TITLE + "] TEXT, " +
                "  [" + PhotoAlbumsColumns.SIZE + "] INTEGER, " +
                "  [" + PhotoAlbumsColumns.PRIVACY_VIEW + "] BLOB, " +
                "  [" + PhotoAlbumsColumns.PRIVACY_COMMENT + "] BLOB, " +
                "  [" + PhotoAlbumsColumns.DESCRIPTION + "] TEXT, " +
                "  [" + PhotoAlbumsColumns.CAN_UPLOAD + "] BOOLEAN, " +
                "  [" + PhotoAlbumsColumns.UPDATED + "] INTEGER, " +
                "  [" + PhotoAlbumsColumns.CREATED + "] INTEGER, " +
                "  [" + PhotoAlbumsColumns.SIZES + "] BLOB, " +
                "  [" + PhotoAlbumsColumns.UPLOAD_BY_ADMINS + "] BOOLEAN, " +
                "  [" + PhotoAlbumsColumns.COMMENTS_DISABLED + "] BOOLEAN, " +
                "  CONSTRAINT [] UNIQUE ([" + PhotoAlbumsColumns.ALBUM_ID + "], [" + PhotoAlbumsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createVideoAlbumsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + VideoAlbumsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + VideoAlbumsColumns.ALBUM_ID + "] INTEGER, " +
                "  [" + VideoAlbumsColumns.OWNER_ID + "] INTEGER, " +
                "  [" + VideoAlbumsColumns.TITLE + "] TEXT, " +
                "  [" + VideoAlbumsColumns.COUNT + "] INTEGER, " +
                "  [" + VideoAlbumsColumns.IMAGE + "] TEXT, " +
                "  [" + VideoAlbumsColumns.UPDATE_TIME + "] INTEGER, " +
                "  [" + VideoAlbumsColumns.PRIVACY + "] BLOB, " +
                "  CONSTRAINT [] UNIQUE ([" + VideoAlbumsColumns.ALBUM_ID + "], [" + VideoAlbumsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createTopicsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + TopicsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + TopicsColumns.TOPIC_ID + "] INTEGER, " +
                "  [" + TopicsColumns.OWNER_ID + "] INTEGER, " +
                "  [" + TopicsColumns.TITLE + "] TEXT, " +
                "  [" + TopicsColumns.CREATED + "] INTEGER, " +
                "  [" + TopicsColumns.CREATED_BY + "] INTEGER, " +
                "  [" + TopicsColumns.UPDATED + "] INTEGER, " +
                "  [" + TopicsColumns.UPDATED_BY + "] INTEGER, " +
                "  [" + TopicsColumns.IS_CLOSED + "] BOOLEAN, " +
                "  [" + TopicsColumns.IS_FIXED + "] BOOLEAN, " +
                "  [" + TopicsColumns.COMMENTS + "] INTEGER, " +
                "  [" + TopicsColumns.FIRST_COMMENT + "] TEXT, " +
                "  [" + TopicsColumns.LAST_COMMENT + "] TEXT, " +
                "  [" + TopicsColumns.ATTACHED_POLL + "] BLOB, " +
                "  CONSTRAINT [] UNIQUE ([" + TopicsColumns.TOPIC_ID + "], [" + TopicsColumns.OWNER_ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createNotoficationsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE [" + NotificationColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + NotificationColumns.DATE + "] INTEGER, " +
                "  [" + NotificationColumns.CONTENT_PACK + "] BLOB);"
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
            return if (!FenrirNative.isNativeLoaded) "fenrir_uncompressed_$aid.sqlite" else "fenrir_$aid.sqlite"
        }
    }
}