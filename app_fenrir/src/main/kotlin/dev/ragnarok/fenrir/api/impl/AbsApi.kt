package dev.ragnarok.fenrir.api.impl

import android.os.SystemClock
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.OutOfDateException
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.model.Captcha
import dev.ragnarok.fenrir.api.model.Error
import dev.ragnarok.fenrir.api.model.Params
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.VKResponse
import dev.ragnarok.fenrir.api.rest.HttpException
import dev.ragnarok.fenrir.api.rest.IServiceRest
import dev.ragnarok.fenrir.isMsgPack
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.nullOrEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.service.ApiErrorCodes
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.refresh.RefreshToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.decodeFromBufferedSource
import kotlinx.serialization.msgpack.MsgPack
import okhttp3.FormBody
import okhttp3.Request
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

internal open class AbsApi(val accountId: Long, private val restProvider: IServiceProvider) {
    fun <T : IServiceRest> provideService(serviceClass: T, vararg tokenTypes: Int): Flow<T> {
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
    ): Flow<BaseResponse<T>> {
        val bodyBuilder = FormBody.Builder()
        for ((key, value) in postParams) {
            bodyBuilder.add(key, value)
        }
        return Includes.networkInterfaces.getVkRestProvider().provideNormalHttpClient(accountId)
            .flatMapConcat { client ->
                flow {
                    val request: Request = Request.Builder()
                        .url(
                            method
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
            .map { response ->
                val k = if (response.body.isMsgPack()) MsgPack().decodeFromOkioStream(
                    serializerType, response.body.source()
                ) as BaseResponse<T> else kJson.decodeFromBufferedSource(
                    serializerType, response.body.source()
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
    ): Flow<VKResponse> {
        val bodyBuilder = FormBody.Builder()
        for ((key, value) in postParams) {
            bodyBuilder.add(key, value)
        }
        return Includes.networkInterfaces.getVkRestProvider().provideNormalHttpClient(accountId)
            .flatMapConcat { client ->
                flow {
                    val request: Request = Request.Builder()
                        .url(
                            method
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
                if (it.body.isMsgPack()) MsgPack().decodeFromOkioStream(
                    VKResponse.serializer(),
                    it.body.source()
                ) else kJson.decodeFromBufferedSource(VKResponse.serializer(), it.body.source())
            }
    }

    private fun handleError(error: Error, params: HashMap<String, String>): Boolean {
        var handle = true
        when (error.errorCode) {
            ApiErrorCodes.TOO_MANY_REQUESTS_PER_SECOND -> {
                synchronized(lock) {
                    SystemClock.sleep((1000 + RANDOM.nextInt(500)).toLong())
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
                provider.requestValidate(error.redirectUri, accountId)
                var code = false
                while (true) {
                    try {
                        code = provider.lookupState(error.redirectUri ?: break)
                        if (code) {
                            break
                        } else {
                            SystemClock.sleep(1000)
                        }
                    } catch (_: OutOfDateException) {
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
                provider.requestCaptcha(captcha.sid, captcha)
                var code: String? = null
                while (true) {
                    try {
                        code = provider.lookupCode(captcha.sid ?: break)
                        if (code != null) {
                            break
                        } else {
                            SystemClock.sleep(1000)
                        }
                    } catch (_: OutOfDateException) {
                        break
                    }
                }
                if (code.nonNullNoEmpty() && captcha.sid.nonNullNoEmpty()) {
                    params["captcha_sid"] = captcha.sid
                    params["captcha_key"] = code
                } else {
                    handle = false
                }
            }

            else -> {
                handle = false
            }
        }
        return handle
    }

    fun <T : Any> extractResponseWithErrorHandling(): suspend (BaseResponse<T>) -> T = {
        val err = it.error
        if (err != null) {
            val params = err.requests()

            if (!handleError(err, params)) {
                throw ApiException(err)
            } else {
                var method = err["post_url"]
                if ("empty" == method) {
                    method = "https://" + Settings.get()
                        .main().apiDomain + "/method/" + err["method"]
                }
                rawVKRequest<T>(
                    method,
                    params,
                    err.serializer ?: throw UnsupportedOperationException()
                ).map(extractResponseWithErrorHandling()).single()
            }
        } else {
            it.response ?: throw NullPointerException("VK return null response")
        }
    }

    fun checkResponseWithErrorHandling(): suspend (VKResponse) -> Boolean = {
        var resp = true
        it.error.requireNonNull { err ->
            val params = err.requests()
            if (!handleError(err, params)) {
                throw ApiException(err)
            } else {
                var method = err["post_url"]
                if ("empty" == method) {
                    method = "https://" + Settings.get()
                        .main().apiDomain + "/method/" + err["method"]
                }
                resp = rawVKRequestOnly(
                    method,
                    params
                ).map(checkResponseWithErrorHandling()).single()
            }
        }
        /*
        it.executeErrors.nonNullNoEmpty {
            it[0].requireNonNull { sit ->
                throw ApiException(sit)
            }
        }
         */
        resp
    }

    companion object {
        val lock = Any()
        val RANDOM = Random(System.nanoTime())
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
