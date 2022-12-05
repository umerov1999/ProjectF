package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.VKApiStickerSetsData
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IStoreService {
    @FormUrlEncoded
    @POST("execute")
    fun getStickers(@Field("code") code: String?): Single<BaseResponse<VKApiStickerSetsData>>

    @FormUrlEncoded
    @POST("execute")
    fun getStickersKeywords(@Field("code") code: String?): Single<BaseResponse<VKApiStickersKeywords>>
}