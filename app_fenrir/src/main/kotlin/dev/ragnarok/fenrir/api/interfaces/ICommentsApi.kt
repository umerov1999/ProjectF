package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.response.CustomCommentsResponse
import io.reactivex.rxjava3.core.Single

interface ICommentsApi {
    // {"response":{"main":false,"first_id":null,"last_id":null,"admin_level":0},"execute_errors":[{"method":"video.getComments","error_code":18,"error_msg":"User was deleted or banned"},{"method":"video.getComments","error_code":18,"error_msg":"User was deleted or banned"},{"method":"video.getComments","error_code":18,"error_msg":"User was deleted or banned"},{"method":"execute.getComments","error_code":18,"error_msg":"User was deleted or banned"}]}
    @CheckResult
    operator fun get(
        sourceType: String?,
        ownerId: Long,
        sourceId: Int,
        offset: Int?,
        count: Int?,
        sort: String?,
        startCommentId: Int?,
        threadComment: Int?,
        accessKey: String?,
        fields: String?
    ): Single<CustomCommentsResponse>
}