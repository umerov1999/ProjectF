package dev.ragnarok.filegallery.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import dev.ragnarok.filegallery.db.column.FilesColumns
import dev.ragnarok.filegallery.db.column.SearchRequestColumns
import dev.ragnarok.filegallery.db.column.TagDirsColumns
import dev.ragnarok.filegallery.db.column.TagOwnerColumns

class SearchRequestHelper(context: Context) :
    SQLiteOpenHelper(context, "search_queries.sqlite", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        createSearchRequestTable(db)
        createTagOwnersTable(db)
        createTagDirsTable(db)
        createFilesTable(db)
    }

    private fun createSearchRequestTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + SearchRequestColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + SearchRequestColumns.SOURCE_ID + "] INTEGER, " +
                "  [" + SearchRequestColumns.QUERY + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createTagOwnersTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + TagOwnerColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + TagOwnerColumns.NAME + "] TEXT, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createTagDirsTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + TagDirsColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + TagDirsColumns.OWNER_ID + "] INTEGER, " +
                "  [" + TagDirsColumns.NAME + "] TEXT, " +
                "  [" + TagDirsColumns.PATH + "] TEXT, " +
                "  [" + TagDirsColumns.TYPE + "] INTEGER, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    private fun createFilesTable(db: SQLiteDatabase) {
        val sql = "CREATE TABLE IF NOT EXISTS [" + FilesColumns.TABLENAME + "] (\n" +
                "  [" + BaseColumns._ID + "] INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "  [" + FilesColumns.PARENT_DIR + "] TEXT, " +
                "  [" + FilesColumns.TYPE + "] INTEGER, " +
                "  [" + FilesColumns.IS_DIR + "] INTEGER, " +
                "  [" + FilesColumns.FILE_NAME + "] TEXT, " +
                "  [" + FilesColumns.FILE_PATH + "] TEXT, " +
                "  [" + FilesColumns.PARENT_NAME + "] TEXT, " +
                "  [" + FilesColumns.PARENT_PATH + "] TEXT, " +
                "  [" + FilesColumns.MODIFICATIONS + "] BIGINT, " +
                "  [" + FilesColumns.SIZE + "] BIGINT, " +
                "  [" + FilesColumns.CAN_READ + "] INTEGER, " +
                "  CONSTRAINT [] UNIQUE ([" + BaseColumns._ID + "]) ON CONFLICT REPLACE);"
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}