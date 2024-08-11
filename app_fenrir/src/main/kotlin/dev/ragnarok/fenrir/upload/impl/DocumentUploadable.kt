package dev.ragnarok.fenrir.upload.impl

import android.content.Context
import dev.ragnarok.fenrir.api.PercentagePublisher
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.server.UploadServer
import dev.ragnarok.fenrir.db.interfaces.IDocsStorage
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.upload.IUploadable
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils
import dev.ragnarok.fenrir.util.Utils.safelyClose
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlowThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException

class DocumentUploadable(
    private val context: Context,
    private val networker: INetworker,
    private val storage: IDocsStorage
) : IUploadable<Document> {
    override fun doUpload(
        upload: Upload,
        initialServer: UploadServer?,
        listener: PercentagePublisher?
    ): Flow<UploadResult<Document>> {
        val ownerId = upload.destination.ownerId
        val groupId = if (ownerId >= 0) null else ownerId
        val accountId = upload.accountId
        val serverSingle = if (initialServer == null) {
            networker.vkDefault(accountId)
                .docs()
                .getUploadServer(groupId)
        } else {
            toFlow(initialServer)
        }
        return serverSingle.flatMapConcat { server ->
            var inputStream: InputStream? = null
            try {
                val uri = upload.fileUri
                val file = File(uri?.path ?: throw NotFoundException("uri.path is empty"))
                inputStream = if (file.isFile) {
                    FileInputStream(file)
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                if (inputStream == null) {
                    toFlowThrowable(
                        NotFoundException(
                            "Unable to open InputStream, URI: $uri"
                        )
                    )
                } else {
                    val filename = UploadUtils.findFileName(context, uri)
                    networker.uploads()
                        .uploadDocumentRx(
                            server.url ?: throw NotFoundException("Upload url empty!"),
                            filename,
                            inputStream,
                            listener
                        )
                        .onCompletion { safelyClose(inputStream) }
                        .flatMapConcat { dto ->
                            if (dto.file.isNullOrEmpty()) {
                                toFlowThrowable(NotFoundException("VK doesn't upload this file"))
                            } else {
                                networker
                                    .vkDefault(accountId)
                                    .docs()
                                    .save(dto.file, filename, null)
                                    .flatMapConcat { tmpList ->
                                        if (tmpList.type.isEmpty()) {
                                            toFlowThrowable(
                                                NotFoundException()
                                            )
                                        } else {
                                            val document = Dto2Model.transform(tmpList.doc)
                                            val result = UploadResult(server, document)
                                            if (upload.isAutoCommit) {
                                                val entity = Dto2Entity.mapDoc(tmpList.doc)
                                                commit(
                                                    storage,
                                                    upload,
                                                    entity
                                                ).map {
                                                    result
                                                }
                                            } else {
                                                toFlow(result)
                                            }
                                        }
                                    }
                            }
                        }
                }
            } catch (e: Exception) {
                safelyClose(inputStream)
                if (e is CancellationException) {
                    throw e
                }
                toFlowThrowable(e)
            }
        }
    }

    private fun commit(
        storage: IDocsStorage,
        upload: Upload,
        entity: DocumentDboEntity
    ): Flow<Boolean> {
        return storage.store(upload.accountId, entity.ownerId, listOf(entity), false)
    }
}