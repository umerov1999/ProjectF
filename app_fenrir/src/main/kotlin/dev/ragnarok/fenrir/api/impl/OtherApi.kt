package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IVkRetrofitProvider
import dev.ragnarok.fenrir.api.interfaces.IOtherApi
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.serializeble.retrofit.HttpCodeException
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

class OtherApi(private val accountId: Int, private val provider: IVkRetrofitProvider) : IOtherApi {
    override fun rawRequest(
        method: String,
        postParams: Map<String, String>
    ): Single<Optional<ResponseBody>> {
        val bodyBuilder = FormBody.Builder()
        for ((key, value) in postParams) {
            bodyBuilder.add(key, value)
        }
        return provider.provideNormalHttpClient(accountId)
            .flatMap { client ->
                Single
                    .create { emitter: SingleEmitter<Response> ->
                        val request: Request = Request.Builder()
                            .url(
                                "https://" + Settings.get()
                                    .other().get_Api_Domain() + "/method/" + method
                            )
                            .post(bodyBuilder.build())
                            .build()
                        val call = client.newCall(request)
                        emitter.setCancellable { call.cancel() }
                        try {
                            val response = call.execute()
                            if (!response.isSuccessful) {
                                emitter.onError(HttpCodeException(response.code))
                            } else {
                                emitter.onSuccess(response)
                            }
                            response.close()
                        } catch (e: Exception) {
                            emitter.onError(e)
                        }
                    }
            }
            .map {
                wrap(it.body)
            }
    }
}
