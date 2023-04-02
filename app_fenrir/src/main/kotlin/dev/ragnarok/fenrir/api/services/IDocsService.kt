package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.server.VKApiDocsUploadServer
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IDocsService : IServiceRest() {
    //https://vk.com/dev/docs.delete
    fun delete(
        ownerId: Long?,
        docId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "docs.delete", form(
                "owner_id" to ownerId,
                "doc_id" to docId
            ), baseInt
        )
    }

    /**
     * Copies a document to a user's or community's document list.
     *
     * @param ownerId   ID of the user or community that owns the document. Use a negative value to designate a community ID.
     * @param docId     Document ID.
     * @param accessKey Access key. This parameter is required if access_key was returned with the document's data.
     * @return the ID of the created document.
     */
    fun add(
        ownerId: Long,
        docId: Int,
        accessKey: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "docs.add", form(
                "owner_id" to ownerId,
                "doc_id" to docId,
                "access_key" to accessKey
            ), baseInt
        )
    }

    /**
     * Returns information about documents by their IDs.
     *
     * @param ids Document IDs. Example: 66748_91488,66748_91455.
     * List of comma-separated words, required parameter
     * @return an array of objects describing documents
     */
    fun getById(ids: String?): Single<BaseResponse<List<VKApiDoc>>> {
        return rest.request("docs.getById", form("docs" to ids), baseList(VKApiDoc.serializer()))
    }

    /**
     * Returns a list of documents matching the search criteria.
     *
     * @param query  Search query string.
     * @param count  Number of results to return.
     * @param offset Offset needed to return a specific subset of results.
     * @return Returns the total results number in count field and an array of objects describing documents in items field
     */
    fun search(
        query: String?,
        count: Int?,
        offset: Int?
    ): Single<BaseResponse<Items<VKApiDoc>>> {
        return rest.request(
            "docs.search", form(
                "q" to query,
                "count" to count,
                "offset" to offset
            ), items(VKApiDoc.serializer())
        )
    }

    /**
     * Saves a document after uploading it to a server.
     *
     * @param file  This parameter is returned when the file is uploaded to the server
     * @param title Document title
     * @param tags  Document tags.
     * @return Returns an array of uploaded document objects.
     */
    fun save(
        file: String?,
        title: String?,
        tags: String?
    ): Single<BaseResponse<VKApiDoc.Entry>> {
        return rest.request(
            "docs.save", form(
                "file" to file,
                "title" to title,
                "tags" to tags
            ), base(VKApiDoc.Entry.serializer())
        )
    }

    /**
     * Returns the server address for document upload.
     *
     * @param peer_id Peer ID (if the document will be uploaded to the chat).
     * @param type    type of document, null or "audio_message" (undocumented option)
     * @return an object with an upload_url field. After the document is uploaded, use the [.save] method.
     */
    fun getMessagesUploadServer(
        peer_id: Long?,
        type: String?
    ): Single<BaseResponse<VKApiDocsUploadServer>> {
        return rest.request(
            "docs.getMessagesUploadServer", form(
                "peer_id" to peer_id,
                "type" to type
            ), base(VKApiDocsUploadServer.serializer())
        )
    }

    fun getUploadServer(groupId: Long?): Single<BaseResponse<VKApiDocsUploadServer>> {
        return rest.request(
            "docs.getUploadServer",
            form("group_id" to groupId),
            base(VKApiDocsUploadServer.serializer())
        )
    }

    /**
     * Returns detailed information about user or community documents.
     *
     * @param ownerId ID of the user or community that owns the documents.
     * Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param count   Number of documents to return. By default, all documents.
     * @param offset  Offset needed to return a specific subset of documents.
     * @param type    Document type. Possible values:
     * 1 — text documents;
     * 2 — archives;
     * 3 — gif;
     * 4 — images;
     * 5 — audio;
     * 6 — video;
     * 7 — e-books;
     * 8 — unknown.
     * @return Returns the total results number in count field and an array of objects describing documents in items field
     */
    operator fun get(
        ownerId: Long?,
        count: Int?,
        offset: Int?,
        type: Int?
    ): Single<BaseResponse<Items<VKApiDoc>>> {
        return rest.request(
            "docs.get", form(
                "owner_id" to ownerId,
                "count" to count,
                "offset" to offset,
                "type" to type
            ), items(VKApiDoc.serializer())
        )
    }
}