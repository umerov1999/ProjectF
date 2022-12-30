package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Error {
    @SerialName("error_code")
    var errorCode = 0

    @SerialName("error_msg")
    var errorMsg: String? = null

    @SerialName("captcha_sid")
    var captchaSid: String? = null

    @SerialName("captcha_img")
    var captchaImg: String? = null

    @SerialName("redirect_uri")
    var redirectUri: String? = null

    @SerialName("request_params")
    var requestParams: List<Params>? = null

    @Transient
    var serializer: KSerializer<*>? = null

    fun requests(): HashMap<String, String> {
        val params: HashMap<String, String> = HashMap(requestParams?.size.orZero())
        for (i in requestParams.orEmpty()) {
            if (i.key == "method") {
                continue
            }
            params[i.key ?: "empty"] = i.value ?: "empty"
        }
        return params
    }

    operator fun get(requestKey: String): String {
        for (i in requestParams.orEmpty()) {
            if (requestKey == i.key) {
                return i.value ?: "empty"
            }
        }
        return "empty"
    }
}

@Serializable
class Params {
    @SerialName("key")
    var key: String? = null

    @SerialName("value")
    var value: String? = null
}