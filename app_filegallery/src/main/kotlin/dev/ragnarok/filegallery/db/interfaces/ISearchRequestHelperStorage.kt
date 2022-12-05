package dev.ragnarok.filegallery.db.interfaces

import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.tags.TagDir
import dev.ragnarok.filegallery.model.tags.TagFull
import dev.ragnarok.filegallery.model.tags.TagOwner
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface ISearchRequestHelperStorage {
    fun getQueries(sourceId: Int): Single<List<String>>
    fun insertQuery(sourceId: Int, query: String?): Completable
    fun deleteQuery(sourceId: Int): Completable
    fun clearQueriesAll()
    fun clearTagsAll()
    fun clearFilesAll()
    fun deleteTagOwner(ownerId: Int): Completable
    fun deleteTagDir(sourceId: Int): Completable
    fun insertTagOwner(name: String?): Single<TagOwner>
    fun insertTagDir(ownerId: Int, item: FileItem): Completable
    fun getTagDirs(ownerId: Int): Single<List<TagDir>>
    fun getAllTagDirs(): Single<List<TagDir>>
    fun getTagOwners(): Single<List<TagOwner>>
    fun getTagFull(): Single<List<TagFull>>
    fun putTagFull(pp: List<TagFull>): Completable
    fun getFiles(parent: String): Single<List<FileItem>>
    fun insertFiles(parent: String, files: List<FileItem>): Completable
    fun updateNameTagOwner(id: Int, name: String?): Completable
    fun deleteTagDirByPath(path: String): Completable
}