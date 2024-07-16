package dev.ragnarok.filegallery.db.interfaces

import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.tags.TagDir
import dev.ragnarok.filegallery.model.tags.TagFull
import dev.ragnarok.filegallery.model.tags.TagOwner
import kotlinx.coroutines.flow.Flow

interface ISearchRequestHelperStorage {
    fun getQueries(sourceId: Int): Flow<List<String>>
    fun insertQuery(sourceId: Int, query: String?): Flow<Boolean>
    fun deleteQuery(sourceId: Int): Flow<Boolean>
    fun clearQueriesAll()
    fun clearTagsAll()
    fun clearFilesAll()
    fun deleteTagOwner(ownerId: Long): Flow<Boolean>
    fun deleteTagDir(sourceId: Long): Flow<Boolean>
    fun insertTagOwner(name: String?): Flow<TagOwner>
    fun insertTagDir(ownerId: Long, item: FileItem): Flow<Boolean>
    fun getTagDirs(ownerId: Long): Flow<List<TagDir>>
    fun getAllTagDirs(): Flow<List<TagDir>>
    fun getTagOwners(): Flow<List<TagOwner>>
    fun getTagFull(): Flow<List<TagFull>>
    fun putTagFull(pp: List<TagFull>): Flow<Boolean>
    fun getFiles(parent: String): Flow<List<FileItem>>
    fun insertFiles(parent: String, files: List<FileItem>): Flow<Boolean>
    fun updateNameTagOwner(id: Long, name: String?): Flow<Boolean>
    fun deleteTagDirByPath(path: String): Flow<Boolean>
}