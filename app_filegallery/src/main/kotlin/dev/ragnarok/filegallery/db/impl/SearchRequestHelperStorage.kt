package dev.ragnarok.filegallery.db.impl

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import dev.ragnarok.filegallery.db.SearchRequestHelper
import dev.ragnarok.filegallery.db.column.FilesColumns
import dev.ragnarok.filegallery.db.column.SearchRequestColumns
import dev.ragnarok.filegallery.db.column.TagDirsColumns
import dev.ragnarok.filegallery.db.column.TagOwnerColumns
import dev.ragnarok.filegallery.db.interfaces.ISearchRequestHelperStorage
import dev.ragnarok.filegallery.getBoolean
import dev.ragnarok.filegallery.getInt
import dev.ragnarok.filegallery.getLong
import dev.ragnarok.filegallery.getString
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.FileType
import dev.ragnarok.filegallery.model.tags.TagDir
import dev.ragnarok.filegallery.model.tags.TagFull
import dev.ragnarok.filegallery.model.tags.TagOwner
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.trimmedIsNullOrEmpty
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class SearchRequestHelperStorage internal constructor(context: Context) :
    ISearchRequestHelperStorage {
    private val app: Context = context.applicationContext
    private val helper: SearchRequestHelper by lazy {
        SearchRequestHelper(app)
    }

    override fun getQueries(sourceId: Int): Flow<List<String>> {
        return flow {
            val where = SearchRequestColumns.SOURCE_ID + " = ?"
            val args = arrayOf(sourceId.toString())
            val cursor = helper.writableDatabase.query(
                SearchRequestColumns.TABLENAME,
                QUERY_PROJECTION, where, args, null, null, BaseColumns._ID + " DESC"
            )
            val data: MutableList<String> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    data.add(it.getString(SearchRequestColumns.QUERY) ?: return@use)
                }
            }
            emit(data)
        }
    }

    override fun insertQuery(sourceId: Int, query: String?): Flow<Boolean> {
        return flow {
            if (query == null) {
                emit(false)
            } else {
                val queryClean = query.trim { it <= ' ' }
                emit(
                    if (queryClean.isEmpty()) {
                        false
                    } else {
                        val db = helper.writableDatabase
                        db.beginTransaction()
                        if (!isActive()) {
                            db.endTransaction()
                            false
                        } else {
                            db.delete(
                                SearchRequestColumns.TABLENAME,
                                SearchRequestColumns.QUERY + " = ?", arrayOf(queryClean)
                            )
                            try {
                                val cv = ContentValues()
                                cv.put(SearchRequestColumns.SOURCE_ID, sourceId)
                                cv.put(SearchRequestColumns.QUERY, queryClean)
                                db.insert(SearchRequestColumns.TABLENAME, null, cv)
                                if (isActive()) {
                                    db.setTransactionSuccessful()
                                }
                            } finally {
                                db.endTransaction()
                            }
                            true
                        }
                    }
                )
            }
        }
    }

    override fun getFiles(parent: String): Flow<List<FileItem>> {
        return flow {
            val where = FilesColumns.PARENT_DIR + " = ?"
            val args = arrayOf(parent)
            val cursor = helper.writableDatabase.query(
                FilesColumns.TABLENAME,
                FILES_PROJECTION,
                where,
                args,
                null,
                null,
                FilesColumns.IS_DIR + " DESC," + FilesColumns.MODIFICATIONS + " DESC"
            )
            if (!isActive()) {
                emit(emptyList())
                return@flow
            }
            val data: MutableList<FileItem> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    data.add(
                        FileItem(
                            it.getInt(FilesColumns.TYPE),
                            it.getString(FilesColumns.FILE_NAME),
                            it.getString(FilesColumns.FILE_PATH),
                            it.getString(FilesColumns.PARENT_NAME),
                            it.getString(FilesColumns.PARENT_PATH),
                            it.getLong(FilesColumns.MODIFICATIONS),
                            it.getLong(FilesColumns.SIZE),
                            it.getBoolean(FilesColumns.CAN_READ)
                        ).checkTag()
                    )
                }
            }
            emit(data)
        }
    }

    override fun insertFiles(parent: String, files: List<FileItem>): Flow<Boolean> {
        return flow {
            val db = helper.writableDatabase
            db.beginTransaction()
            if (!isActive()) {
                db.endTransaction()
                return@flow
            }
            db.delete(
                FilesColumns.TABLENAME,
                FilesColumns.PARENT_DIR + " = ?", arrayOf(parent)
            )
            try {
                for (i in files) {
                    val cv = ContentValues()
                    cv.put(FilesColumns.PARENT_DIR, parent)
                    cv.put(FilesColumns.TYPE, i.type)
                    cv.put(FilesColumns.IS_DIR, if (i.type == FileType.folder) 1 else 0)
                    cv.put(FilesColumns.FILE_NAME, i.file_name)
                    cv.put(FilesColumns.FILE_PATH, i.file_path)
                    cv.put(FilesColumns.PARENT_NAME, i.parent_name)
                    cv.put(FilesColumns.PARENT_PATH, i.parent_path)
                    cv.put(FilesColumns.MODIFICATIONS, i.modification)
                    cv.put(FilesColumns.SIZE, i.size)
                    cv.put(FilesColumns.CAN_READ, if (i.isCanRead) 1 else 0)
                    db.insert(FilesColumns.TABLENAME, null, cv)
                }
                if (isActive()) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            emit(true)
        }
    }

    override fun clearQueriesAll() {
        helper.writableDatabase.delete(SearchRequestColumns.TABLENAME, null, null)
    }

    override fun clearFilesAll() {
        helper.writableDatabase.delete(FilesColumns.TABLENAME, null, null)
    }

    override fun deleteQuery(sourceId: Int): Flow<Boolean> {
        return flow {
            helper.writableDatabase.delete(
                SearchRequestColumns.TABLENAME,
                SearchRequestColumns.SOURCE_ID + " = ?", arrayOf(sourceId.toString())
            )
            emit(true)
        }
    }

    override fun deleteTagOwner(ownerId: Long): Flow<Boolean> {
        return flow {
            helper.writableDatabase.delete(
                TagDirsColumns.TABLENAME,
                TagDirsColumns.OWNER_ID + " = ?", arrayOf(ownerId.toString())
            )
            helper.writableDatabase.delete(
                TagOwnerColumns.TABLENAME,
                BaseColumns._ID + " = ?", arrayOf(ownerId.toString())
            )
            emit(true)
        }
    }

    override fun deleteTagDir(sourceId: Long): Flow<Boolean> {
        return flow {
            helper.writableDatabase.delete(
                TagDirsColumns.TABLENAME,
                BaseColumns._ID + " = ?", arrayOf(sourceId.toString())
            )
            emit(true)
        }
    }

    override fun deleteTagDirByPath(path: String): Flow<Boolean> {
        return flow {
            helper.writableDatabase.delete(
                TagDirsColumns.TABLENAME,
                TagDirsColumns.PATH + " = ?", arrayOf(path)
            )
            MusicPlaybackController.tracksExist.deleteTag(path)
            emit(true)
        }
    }

    override fun clearTagsAll() {
        val db = helper.writableDatabase
        db.delete(TagDirsColumns.TABLENAME, null, null)
        db.delete(TagOwnerColumns.TABLENAME, null, null)
    }

    override fun insertTagOwner(name: String?): Flow<TagOwner> {
        return flow {
            if (name.trimmedIsNullOrEmpty()) {
                throw Throwable("require name not null!!!")
            } else {
                val nameClean = name.trim { it <= ' ' }
                val db = helper.writableDatabase
                db.beginTransaction()
                if (!isActive()) {
                    db.endTransaction()
                    emit(TagOwner())
                } else {
                    val cCheck = db.query(
                        TagOwnerColumns.TABLENAME,
                        arrayOf(BaseColumns._ID, TagOwnerColumns.NAME),
                        TagOwnerColumns.NAME + " = ?",
                        arrayOf(nameClean),
                        null,
                        null,
                        null
                    )
                    val checkCount = cCheck.count
                    cCheck.close()
                    if (checkCount > 0) {
                        if (isActive()) {
                            db.setTransactionSuccessful()
                        }
                        db.endTransaction()
                        throw Throwable("require name not equals!!!")
                    } else {
                        val cv = ContentValues()
                        cv.put(TagOwnerColumns.NAME, name)
                        val ret = db.insert(TagOwnerColumns.TABLENAME, null, cv)
                        if (isActive()) {
                            db.setTransactionSuccessful()
                        }
                        db.endTransaction()
                        emit(TagOwner().setId(ret).setName(nameClean))
                    }
                }
            }
        }
    }

    override fun updateNameTagOwner(id: Long, name: String?): Flow<Boolean> {
        return flow {
            if (name.trimmedIsNullOrEmpty()) {
                throw Throwable("require name not null!!!")
            } else {
                val nameClean = name.trim { it <= ' ' }
                val db = helper.writableDatabase
                db.beginTransaction()
                if (!isActive()) {
                    db.endTransaction()
                    emit(false)
                } else {
                    val cCheck = db.query(
                        TagOwnerColumns.TABLENAME,
                        arrayOf(BaseColumns._ID, TagOwnerColumns.NAME),
                        TagOwnerColumns.NAME + " = ?",
                        arrayOf(nameClean),
                        null,
                        null,
                        null
                    )
                    val checkCount = cCheck.count
                    cCheck.close()
                    if (checkCount > 0) {
                        if (isActive()) {
                            db.setTransactionSuccessful()
                        }
                        db.endTransaction()
                        emit(false)
                    } else {
                        val cv = ContentValues()
                        cv.put(TagOwnerColumns.NAME, nameClean)
                        db.update(
                            TagOwnerColumns.TABLENAME, cv,
                            BaseColumns._ID + " = ?", arrayOf(id.toString())
                        )
                        if (isActive()) {
                            db.setTransactionSuccessful()
                        }
                        db.endTransaction()
                        emit(true)
                    }
                }
            }
        }
    }

    override fun insertTagDir(ownerId: Long, item: FileItem): Flow<Boolean> {
        return flow {
            emit(
                if (item.file_name.isNullOrEmpty() || item.file_path.isNullOrEmpty()) {
                    false
                } else {
                    val db = helper.writableDatabase
                    db.beginTransaction()
                    if (!isActive()) {
                        db.endTransaction()
                        false
                    } else {
                        db.delete(
                            TagDirsColumns.TABLENAME,
                            TagDirsColumns.PATH + " = ?", arrayOf(item.file_path)
                        )
                        MusicPlaybackController.tracksExist.deleteTag(item.file_path)
                        try {
                            val cv = ContentValues()
                            cv.put(TagDirsColumns.OWNER_ID, ownerId)
                            cv.put(TagDirsColumns.NAME, item.file_name)
                            cv.put(TagDirsColumns.PATH, item.file_path)
                            cv.put(TagDirsColumns.TYPE, item.type)
                            db.insert(TagDirsColumns.TABLENAME, null, cv)
                            if (isActive()) {
                                db.setTransactionSuccessful()
                            }
                        } finally {
                            db.endTransaction()
                        }
                        MusicPlaybackController.tracksExist.addTag(item.file_path)
                        true
                    }
                }
            )
        }
    }

    private fun getFolderSize(path: String?): Long {
        path ?: return -1
        if (!Settings.get().main().isEnable_dirs_files_count) {
            return -1
        }
        return (File(path).list()?.size ?: -1).toLong()
    }

    private fun getFileSize(path: String?): Long {
        path ?: return -1
        return File(path).length()
    }

    private class ItemModificationComparator : Comparator<TagDir> {
        override fun compare(lhs: TagDir, rhs: TagDir): Int {
            return when {
                lhs.type == FileType.folder && rhs.type != FileType.folder -> {
                    -1
                }

                lhs.type != FileType.folder && rhs.type == FileType.folder -> {
                    1
                }

                else -> rhs.id.compareTo(lhs.id)
            }
        }
    }

    override fun getTagDirs(ownerId: Long): Flow<List<TagDir>> {
        return flow {
            val where = TagDirsColumns.OWNER_ID + " = ?"
            val args = arrayOf(ownerId.toString())
            val cursor = helper.writableDatabase.query(
                TagDirsColumns.TABLENAME,
                TAG_DIR_PROJECTION, where, args, null, null, BaseColumns._ID + " DESC"
            )
            val data: MutableList<TagDir> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    val path = it.getString(TagDirsColumns.PATH)
                    val type = it.getInt(TagDirsColumns.TYPE)
                    data.add(
                        TagDir().setId(it.getLong(BaseColumns._ID))
                            .setOwner_id(it.getLong(TagDirsColumns.OWNER_ID))
                            .setName(it.getString(TagDirsColumns.NAME))
                            .setPath(path)
                            .setType(type)
                            .setSize(
                                if (type == FileType.folder) getFolderSize(path) else getFileSize(
                                    path
                                )
                            )
                    )
                }
            }
            data.sortWith(ItemModificationComparator())
            emit(data)
        }
    }

    override fun getAllTagDirs(): Flow<List<TagDir>> {
        return flow {
            val cursor = helper.writableDatabase.query(
                TagDirsColumns.TABLENAME,
                TAG_DIR_PROJECTION, null, null, null, null, BaseColumns._ID + " DESC"
            )
            val data: MutableList<TagDir> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    val path = it.getString(TagDirsColumns.PATH)
                    val type = it.getInt(TagDirsColumns.TYPE)
                    data.add(
                        TagDir().setId(it.getLong(BaseColumns._ID))
                            .setOwner_id(it.getLong(TagDirsColumns.OWNER_ID))
                            .setName(it.getString(TagDirsColumns.NAME))
                            .setPath(path)
                            .setType(type)
                            .setSize(
                                if (type == FileType.folder) getFolderSize(path) else getFileSize(
                                    path
                                )
                            )
                    )
                }
            }
            emit(data)
        }
    }

    override fun putTagFull(pp: List<TagFull>): Flow<Boolean> {
        return flow {
            clearTagsAll()
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                if (!isActive()) {
                    db.endTransaction()
                    emit(false)
                    return@flow
                }
                for (p in pp) {
                    val cv = ContentValues()
                    cv.put(TagOwnerColumns.NAME, p.name)
                    val v = db.insert(TagOwnerColumns.TABLENAME, null, cv)
                    for (kk in (p.dirs ?: emptyList())) {
                        val cvDir = ContentValues()
                        cvDir.put(TagDirsColumns.OWNER_ID, v)
                        cvDir.put(TagDirsColumns.NAME, kk.name)
                        cvDir.put(TagDirsColumns.PATH, kk.path)
                        cvDir.put(TagDirsColumns.TYPE, kk.type)
                        db.insert(TagDirsColumns.TABLENAME, null, cvDir)
                    }
                }
                if (isActive()) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            emit(true)
        }
    }

    override fun getTagFull(): Flow<List<TagFull>> {
        return flow {
            val pp: MutableList<TagFull> = ArrayList()
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                if (!isActive()) {
                    db.endTransaction()
                    emit(pp)
                    return@flow
                }
                db.query(
                    TagOwnerColumns.TABLENAME,
                    TAG_OWNER_PROJECTION, null, null, null, null, BaseColumns._ID + " DESC"
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val kk = TagFull()
                            .setName(cursor.getString(TagOwnerColumns.NAME))
                        val where = TagDirsColumns.OWNER_ID + " = ?"
                        val args = arrayOf(cursor.getString(BaseColumns._ID).toString())
                        val dCursor = db.query(
                            TagDirsColumns.TABLENAME,
                            TAG_DIR_PROJECTION, where, args, null, null, BaseColumns._ID + " DESC"
                        )
                        val data: ArrayList<TagFull.TagDirEntry> = ArrayList(dCursor.count)
                        dCursor.use {
                            while (it.moveToNext()) {
                                data.add(
                                    TagFull.TagDirEntry().setName(it.getString(TagDirsColumns.NAME))
                                        .setPath(it.getString(TagDirsColumns.PATH))
                                        .setType(it.getInt(TagDirsColumns.TYPE))
                                )
                            }
                        }
                        kk.dirs = data
                        pp.add(kk)
                    }
                }
                if (isActive()) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            emit(pp)
        }
    }

    override fun getTagOwners(): Flow<List<TagOwner>> {
        return flow {
            val db = helper.writableDatabase
            val cursor = db.query(
                TagOwnerColumns.TABLENAME,
                TAG_OWNER_PROJECTION, null, null, null, null, BaseColumns._ID + " DESC"
            )
            val data: MutableList<TagOwner> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getLong(BaseColumns._ID)
                    val where = TagDirsColumns.OWNER_ID + " = ?"
                    val args = arrayOf(id.toString())
                    val c = db.query(
                        TagDirsColumns.TABLENAME,
                        arrayOf(BaseColumns._ID, TagDirsColumns.OWNER_ID),
                        where,
                        args,
                        null,
                        null,
                        null
                    )
                    data.add(
                        TagOwner().setId(id).setCount(c.count)
                            .setName(it.getString(TagOwnerColumns.NAME))
                    )
                    c.close()
                }
            }
            emit(data)
        }
    }

    companion object {
        private val QUERY_PROJECTION = arrayOf(
            BaseColumns._ID, SearchRequestColumns.SOURCE_ID, SearchRequestColumns.QUERY
        )
        private val TAG_OWNER_PROJECTION = arrayOf(
            BaseColumns._ID, TagOwnerColumns.NAME
        )
        private val TAG_DIR_PROJECTION = arrayOf(
            BaseColumns._ID,
            TagDirsColumns.OWNER_ID,
            TagDirsColumns.NAME,
            TagDirsColumns.PATH,
            TagDirsColumns.TYPE
        )
        private val FILES_PROJECTION = arrayOf(
            BaseColumns._ID, FilesColumns.PARENT_DIR,
            FilesColumns.TYPE,
            FilesColumns.IS_DIR,
            FilesColumns.FILE_NAME,
            FilesColumns.FILE_PATH,
            FilesColumns.PARENT_NAME,
            FilesColumns.PARENT_PATH,
            FilesColumns.MODIFICATIONS,
            FilesColumns.SIZE,
            FilesColumns.CAN_READ
        )
    }

}
