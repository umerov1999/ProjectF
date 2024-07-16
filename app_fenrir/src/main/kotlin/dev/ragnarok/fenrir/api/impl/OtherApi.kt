package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IVKRestProvider
import dev.ragnarok.fenrir.api.interfaces.IOtherApi
import dev.ragnarok.fenrir.api.rest.HttpException
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.ResponseBody
import kotlin.coroutines.cancellation.CancellationException

class OtherApi(private val accountId: Long, private val provider: IVKRestProvider) : IOtherApi {
    override fun rawRequest(
        method: String,
        postParams: Map<String, String>
    ): Flow<Optional<ResponseBody>> {
        val bodyBuilder = FormBody.Builder()
        for ((key, value) in postParams) {
            bodyBuilder.add(key, value)
        }
        return provider.provideNormalHttpClient(accountId)
            .flatMapConcat { client ->
                flow {
                    val request: Request = Request.Builder()
                        .url(
                            "https://" + Settings.get()
                                .main().apiDomain + "/method/" + method
                        )
                        .post(bodyBuilder.build())
                        .build()
                    val call = client.build().newCall(request)
                    try {
                        val response = call.execute()
                        if (!response.isSuccessful) {
                            throw HttpException(response.code)
                        } else {
                            emit(response)
                        }
                        response.close()
                    } catch (e: CancellationException) {
                        call.cancel()
                        throw e
                    }
                }
            }
            .map {
                wrap(it.body)
            }
    }
}
