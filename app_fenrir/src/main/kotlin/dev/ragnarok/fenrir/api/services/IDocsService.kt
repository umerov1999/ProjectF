package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.server.VKApiDocsUploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiVideosUploadServer
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IDocsService {
    //https://vk.com/dev/docs.delete
    @FormUrlEncoded
    @POST("docs.delete")
    fun delete(
        @Field("owner_id") ownerId: Int?,
        @Field("doc_id") docId: Int
    ): Single<BaseResponse<Int>>

    /**
     * Copies a document to a user's or community's document list.
     *
     * @param ownerId   ID of the user or community that owns the document. Use a negative value to designate a community ID.
     * @param docId     Document ID.
     * @param accessKey Access key. This parameter is required if access_key was returned with the document's data.
     * @return the ID of the created document.
     */
    @FormUrlEncoded
    @POST("docs.add")
    fun add(
        @Field("owner_id") ownerId: Int,
        @Field("doc_id") docId: Int,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<Int>>

    /**
     * Returns information about documents by their IDs.
     *
     * @param ids Document IDs. Example: 66748_91488,66748_91455.
     * List of comma-separated words, required parameter
     * @return an array of objects describing documents
     */
    @FormUrlEncoded
    @POST("docs.getById")
    fun getById(@Field("docs") ids: String?): Single<BaseResponse<List<VKApiDoc>>>

    /**
     * Returns a list of documents matching the search criteria.
     *
     * @param query  Search query string.
     * @param count  Number of results to return.
     * @param offset Offset needed to return a specific subset of results.
     * @return Returns the total results number in count field and an array of objects describing documents in items field
     */
    @FormUrlEncoded
    @POST("docs.search")
    fun search(
        @Field("q") query: String?,
        @Field("count") count: Int?,
        @Field("offset") offset: Int?
    ): Single<BaseResponse<Items<VKApiDoc>>>

    /**
     * Saves a document after uploading it to a server.
     *
     * @param file  This parameter is returned when the file is uploaded to the server
     * @param title Document title
     * @param tags  Document tags.
     * @return Returns an array of uploaded document objects.
     */
    @FormUrlEncoded
    @POST("docs.save")
    fun save(
        @Field("file") file: String?,
        @Field("title") title: String?,
        @Field("tags") tags: String?
    ): Single<BaseResponse<VKApiDoc.Entry>>

    /**
     * Returns the server address for document upload.
     *
     * @param peer_id Peer ID (if the document will be uploaded to the chat).
     * @param type    type of document, null or "audio_message" (undocumented option)
     * @return an object with an upload_url field. After the document is uploaded, use the [.save] method.
     */
    @FormUrlEncoded
    @POST("docs.getMessagesUploadServer")
    fun getMessagesUploadServer(
        @Field("peer_id") peer_id: Int?,
        @Field("type") type: String?
    ): Single<BaseResponse<VKApiDocsUploadServer>>

    @FormUrlEncoded
    @POST("docs.getUploadServer")
    fun getUploadServer(@Field("group_id") groupId: Int?): Single<BaseResponse<VKApiDocsUploadServer>>

    @FormUrlEncoded
    @POST("video.save")
    fun getVideoServer(
        @Field("is_private") is_private: Int?,
        @Field("group_id") group_id: Int?,
        @Field("name") name: String?
    ): Single<BaseResponse<VKApiVideosUploadServer>>

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
    @FormUrlEncoded
    @POST("docs.get")
    operator fun get(
        @Field("owner_id") ownerId: Int?,
        @Field("count") count: Int?,
        @Field("offset") offset: Int?,
        @Field("type") type: Int?
    ): Single<BaseResponse<Items<VKApiDoc>>>
}