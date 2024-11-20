package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

class IStatusService : IServiceRest() {
    /**
     * Sets a new status for the current user.
     *
     * @param text    Text of the new status.
     * @param groupId Identifier of a community to set a status in. If left blank the status is set to current user.
     * @return 1
     */
    fun set(
        text: String?,
        groupId: Long?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "status.set", form(
                "text" to text,
                "group_id" to groupId
            ), baseInt
        )
    }
}