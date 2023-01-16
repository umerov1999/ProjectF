package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.CustomCommentsResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class ICommentsService : IServiceRest() {
    operator fun get(
        code: String?,
        sourceType: String?,
        ownerId: Long,
        sourceId: Int,
        offset: Int?,
        count: Int?,
        sort: String?,
        startCommentId: Int?,
        comment_id: Int,
        accessKey: String?,
        fields: String?
    ): Single<BaseResponse<CustomCommentsResponse>> {
        return rest.request(
            "execute",
            form(
                "code" to code,
                "source_type" to sourceType,
                "owner_id" to ownerId,
                "source_id" to sourceId,
                "offset" to offset,
                "count" to count,
                "sort" to sort,
                "start_comment_id" to startCommentId,
                "comment_id" to comment_id,
                "access_key" to accessKey,
                "fields" to fields
            ), base(CustomCommentsResponse.serializer())
        )
    }
}