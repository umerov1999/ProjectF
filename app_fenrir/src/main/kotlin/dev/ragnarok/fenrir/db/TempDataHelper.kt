package dev.ragnarok.fenrir.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.db.column.*
import dev.ragnarok.fenrir.module.FenrirNative

class TempDataHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        if (!FenrirNative.isNativeLoaded) "temp_app_data_uncompressed.sqlite" else "temp_app_data.sqlite",
        null,
        Constants.DATABASE_TEMPORARY_VERSION
    ) {
    override fun onCreate(db: SQLiteDatabase) {
        createTmpDataTable(db)
        createSearchRequestTable(db)
        createLogsTable(db)
        createShortcutsColumn(db)
        createAudiosTable(db)
        createStickerSetTable(db)
        createStickersKeywordsTable(db)
    }

    private fun createStickerSetTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + StickerSetColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                " [" + StickerSetColumns.ACCOUNT_ID + "] INTEGER, " +
                " [" + StickerSetColumns.POSITION + "] INTEGER, " +
                " [" + StickerSetColumns.TITLE + "] TEXT, " +
                " [" + StickerSetColumns.ICON + "] BLOB, " +
                " [" + StickerSetColumns.PURCHASED + "] BOOLEAN, " +
                " [" + StickerSetColumns.PROMOTED + "] BOOLEAN, " +
                " [" + StickerSetColumns.ACTIVE + "] BOOLEAN, " +
                " [" + StickerSetColumns.STICKERS + "] BLOB, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createStickersKeywordsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + StickersKeywordsColumns.TABLENAME + "] (\n" +
                " [" + BaseColumns._ID + "] INTEGER NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                " [" + StickersKeywordsColumns.ACCOUNT_ID + "] INTEGER, " +
                " [" + StickersKeywordsColumns.KEYWORDS + "] BLOB, " +
                " [" + StickersKeywordsColumns.STICKERS + "] BLOB, " +
                " CONSTRAINT [] PRIMARY KEY([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createTmpDataTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + TempDataColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + TempDataColumns.OWNER_ID + "] INTEGER, " +
                "  [" + TempDataColumns.SOURCE_ID + "] INTEGER, " +
                "  [" + TempDataColumns.DATA + "] BLOB, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createSearchRequestTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + SearchRequestColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + SearchRequestColumns.SOURCE_ID + "] INTEGER, " +
                "  [" + SearchRequestColumns.QUERY + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createShortcutsColumn(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + ShortcutColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + ShortcutColumns.ACTION + "] TEXT, " +
                "  [" + ShortcutColumns.COVER + "] TEXT, " +
                "  [" + ShortcutColumns.NAME + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createLogsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + LogColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + LogColumns.TYPE + "] INTEGER, " +
                "  [" + LogColumns.DATE + "] INTEGER, " +
                "  [" + LogColumns.TAG + "] TEXT, " +
                "  [" + LogColumns.BODY + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createAudiosTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + AudioColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + AudioColumns.SOURCE_OWNER_ID + "] INTEGER, " +
                "  [" + AudioColumns.AUDIO_ID + "] INTEGER, " +
                "  [" + AudioColumns.AUDIO_OWNER_ID + "] INTEGER, " +
                "  [" + AudioColumns.ARTIST + "] TEXT, " +
                "  [" + AudioColumns.TITLE + "] TEXT, " +
                "  [" + AudioColumns.DURATION + "] INTEGER, " +
                "  [" + AudioColumns.URL + "] TEXT, " +
                "  [" + AudioColumns.LYRICS_ID + "] INTEGER, " +
                "  [" + AudioColumns.DATE + "] INTEGER, " +
                "  [" + AudioColumns.ALBUM_ID + "] INTEGER, " +
                "  [" + AudioColumns.ALBUM_OWNER_ID + "] INTEGER, " +
                "  [" + AudioColumns.ALBUM_ACCESS_KEY + "] TEXT, " +
                "  [" + AudioColumns.GENRE + "] INTEGER, " +
                "  [" + AudioColumns.DELETED + "] BOOLEAN, " +
                "  [" + AudioColumns.ACCESS_KEY + "] TEXT, " +
                "  [" + AudioColumns.THUMB_IMAGE_BIG + "] TEXT, " +
                "  [" + AudioColumns.THUMB_IMAGE_VERY_BIG + "] TEXT, " +
                "  [" + AudioColumns.THUMB_IMAGE_LITTLE + "] TEXT, " +
                "  [" + AudioColumns.ALBUM_TITLE + "] TEXT, " +
                "  [" + AudioColumns.MAIN_ARTISTS + "] BLOB, " +
                "  [" + AudioColumns.IS_HQ + "] BOOLEAN, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    fun clear() {
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS " + TempDataColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + SearchRequestColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + AudioColumns.TABLENAME)
        onCreate(db)
    }

    fun clearLogs() {
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS " + LogColumns.TABLENAME)
        onCreate(db)
    }

    private fun purge(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS " + TempDataColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + SearchRequestColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + LogColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + ShortcutColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + AudioColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + StickerSetColumns.TABLENAME)
        db.execSQL("DROP TABLE IF EXISTS " + StickersKeywordsColumns.TABLENAME)
        onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != Constants.DATABASE_TEMPORARY_VERSION) {
            purge(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != Constants.DATABASE_TEMPORARY_VERSION) {
            purge(db)
        }
    }

    companion object {
        val helper: TempDataHelper by lazy {
            TempDataHelper(Includes.provideApplicationContext())
        }
    }
}