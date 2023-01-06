package dev.ragnarok.fenrir.api.impl

import android.os.SystemClock
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.api.*
import dev.ragnarok.fenrir.api.model.Captcha
import dev.ragnarok.fenrir.api.model.Error
import dev.ragnarok.fenrir.api.model.IAttachmentToken
import dev.ragnarok.fenrir.api.model.Params
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.VkResponse
import dev.ragnarok.fenrir.api.rest.HttpException
import dev.ragnarok.fenrir.api.rest.IServiceRest
import dev.ragnarok.fenrir.service.ApiErrorCodes
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.refresh.RefreshToken
import dev.ragnarok.fenrir.util.serializeble.json.decodeFromStream
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.functions.Function
import kotlinx.serialization.KSerializer
import okhttp3.*
import java.util.*

internal open class AbsApi(val accountId: Int, private val restProvider: IServiceProvider) {
    fun <T : IServiceRest> provideService(serviceClass: T, vararg tokenTypes: Int): Single<T> {
        var pTokenTypes: IntArray = tokenTypes
        if (pTokenTypes.nullOrEmpty()) {
            pTokenTypes = intArrayOf(TokenType.USER) // user by default
        }
        return restProvider.provideService(accountId, serviceClass, *pTokenTypes)
    }

    @Suppress("unchecked_cast")
    private fun <T : Any> rawVKRequest(
        method: String,
        postParams: Map<String, String>,
        serializerType: KSerializer<*>
    ): Single<BaseResponse<T>> {
        val bodyBuilder = FormBody.Builder()
        for ((key, value) in postParams) {
            bodyBuilder.add(key, value)
        }
        return Includes.networkInterfaces.getVkRestProvider().provideNormalHttpClient(accountId)
            .flatMap { client ->
                Single.create { emitter: SingleEmitter<Response> ->
                    val request: Request = Request.Builder()
                        .url(
                            method
                        )
                        .post(bodyBuilder.build())
                        .build()
                    val call = client.build().newCall(request)
                    emitter.setCancellable { call.cancel() }
                    try {
                        val response = call.execute()
                        if (!response.isSuccessful) {
                            emitter.tryOnError(HttpException(response.code))
                        } else {
                            emitter.onSuccess(response)
                        }
                        response.close()
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    }
                }
            }
            .map { response ->
                val k = if (response.body.isMsgPack()) MsgPack().decodeFromOkioStream(
                    serializerType, response.body.source()
                ) as BaseResponse<T> else kJson.decodeFromStream(
                    serializerType, response.body.byteStream()
                ) as BaseResponse<T>
                k.error?.let {
                    it.serializer = serializerType
                    val o = ArrayList<Params>()
                    for ((key, value) in postParams) {
                        val tmp = Params()
                        tmp.key = key
                        tmp.value = value
                        o.add(tmp)
                    }
                    val tmp = Params()
                    tmp.key = "post_url"
                    tmp.value = method
                    o.add(tmp)
                    it.requestParams = o
                }
                k
            }
    }

    private fun rawVKRequestOnly(
        method: String,
        postParams: Map<String, String>
    ): Single<VkResponse> {
        val bodyBuilder = FormBody.Builder()
        for ((key, value) in postParams) {
            bodyBuilder.add(key, value)
        }
        return Includes.networkInterfaces.getVkRestProvider().provideNormalHttpClient(accountId)
            .flatMap { client ->
                Single.create { emitter: SingleEmitter<Response> ->
                    val request: Request = Request.Builder()
                        .url(
                            method
                        )
                        .post(bodyBuilder.build())
                        .build()
                    val call = client.build().newCall(request)
                    emitter.setCancellable { call.cancel() }
                    try {
                        val response = call.execute()
                        if (!response.isSuccessful) {
                            emitter.tryOnError(HttpException(response.code))
                        } else {
                            emitter.onSuccess(response)
                        }
                        response.close()
                    } catch (e: Exception) {
                        emitter.tryOnError(e)
                    }
                }
            }
            .map {
                if (it.body.isMsgPack()) MsgPack().decodeFromOkioStream(
                    VkResponse.serializer(),
                    it.body.source()
                ) else kJson.decodeFromStream(VkResponse.serializer(), it.body.byteStream())
            }
    }

    private fun handleError(error: Error, params: HashMap<String, String>): Boolean {
        var handle = true
        when (error.errorCode) {
            ApiErrorCodes.TOO_MANY_REQUESTS_PER_SECOND -> {
                synchronized(AbsVkApiInterceptor::class.java) {
                    val sleepMs = 1000 + RANDOM.nextInt(500)
                    SystemClock.sleep(sleepMs.toLong())
                }
            }
            ApiErrorCodes.REFRESH_TOKEN, ApiErrorCodes.CLIENT_VERSION_DEPRECATED -> {
                val token = error.requests()["access_token"] ?: Settings.get().accounts()
                    .getAccessToken(accountId)
                if (token.isNullOrEmpty() || !RefreshToken.upgradeToken(
                        accountId,
                        token
                    )
                ) {
                    handle = false
                } else {
                    params["access_token"] =
                        Settings.get().accounts().getAccessToken(accountId).orEmpty()
                }
            }
            ApiErrorCodes.VALIDATE_NEED -> {
                val provider = Includes.validationProvider
                provider.requestValidate(error.redirectUri)
                var code = false
                while (true) {
                    try {
                        code = provider.lookupState(error.redirectUri ?: break)
                        if (code) {
                            break
                        } else {
                            SystemClock.sleep(1000)
                        }
                    } catch (e: OutOfDateException) {
                        break
                    }
                }
                handle = code
                if (handle) {
                    params["access_token"] =
                        Settings.get().accounts().getAccessToken(accountId).orEmpty()
                }
            }
            ApiErrorCodes.CAPTCHA_NEED -> {
                val captcha = Captcha(error.captchaSid, error.captchaImg)
                val provider = Includes.captchaProvider
                provider.requestCaptha(captcha.sid, captcha)
                var code: String? = null
                while (true) {
                    try {
                        code = provider.lookupCode(captcha.sid ?: break)
                        if (code != null) {
                            break
                        } else {
                            SystemClock.sleep(1000)
                        }
                    } catch (e: OutOfDateException) {
                        break
                    }
                }
                if (code != null) {
                    params["captcha_sid"] = captcha.sid
                    params["captcha_key"] = code
                }
            }
            else -> {
                handle = false
            }
        }
        return handle
    }

    fun <T : Any> extractResponseWithErrorHandling(): Function<BaseResponse<T>, T> {
        return Function { response: BaseResponse<T> ->
            response.error.requireNonNull {
                val params = it.requests()

                if (!handleError(it, params)) {
                    throw Exceptions.propagate(ApiException(it))
                } else {
                    var method = it["post_url"]
                    if ("empty" == method) {
                        method = "https://" + Settings.get()
                            .other().get_Api_Domain() + "/method/" + it["method"]
                    }
                    return@Function rawVKRequest<T>(
                        method,
                        params,
                        it.serializer ?: throw UnsupportedOperationException()
                    )
                        .map(extractResponseWithErrorHandling())
                        .blockingGet()
                }
            }
            response.response ?: throw NullPointerException("VK return null response")
        }
    }

    fun checkResponseWithErrorHandling(): Function<VkResponse, Completable> {
        return Function { response: VkResponse ->
            response.error.requireNonNull {
                val params = it.requests()
                if (!handleError(it, params)) {
                    throw Exceptions.propagate(ApiException(it))
                } else {
                    var method = it["post_url"]
                    if ("empty" == method) {
                        method = "https://" + Settings.get()
                            .other().get_Api_Domain() + "/method/" + it["method"]
                    }
                    return@Function rawVKRequestOnly(method, params)
                        .map(checkResponseWithErrorHandling())
                        .blockingGet()
                }
            }
            Completable.complete()
        }
    }

    companion object {
        val RANDOM = Random()
        inline fun <reified T> join(
            tokens: Iterable<T>?,
            delimiter: String?,
            crossinline function: (T) -> String
        ): String? {
            if (tokens == null) {
                return null
            }
            val sb = StringBuilder()
            var firstTime = true
            for (token in tokens) {
                if (firstTime) {
                    firstTime = false
                } else {
                    sb.append(delimiter)
                }
                sb.append(function.invoke(token))
            }
            return sb.toString()
        }

        fun join(tokens: Iterable<*>?, delimiter: String): String? {
            if (tokens == null) {
                return null
            }
            val sb = StringBuilder()
            var firstTime = true
            for (token in tokens) {
                if (firstTime) {
                    firstTime = false
                } else {
                    sb.append(delimiter)
                }
                sb.append(token)
            }
            return sb.toString()
        }

        fun formatAttachmentToken(token: IAttachmentToken): String {
            return token.format()
        }

        fun toQuotes(word: String?): String? {
            return if (word == null) {
                null
            } else "\"" + word + "\""
        }

        fun integerFromBoolean(value: Boolean?): Int? {
            return if (value == null) null else if (value) 1 else 0
        }
    }
}