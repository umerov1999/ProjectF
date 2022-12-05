package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IVkRetrofitProvider
import dev.ragnarok.fenrir.api.interfaces.IOtherApi
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import okhttp3.*
import java.io.IOException

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
                            .method("POST", bodyBuilder.build())
                            .build()
                        val call = client.newCall(request)
                        emitter.setCancellable { call.cancel() }
                        call.enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                emitter.onError(e)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                emitter.onSuccess(response)
                            }
                        })
                    }
            }
            .map {
                wrap(it.body)
            }
    }
}
