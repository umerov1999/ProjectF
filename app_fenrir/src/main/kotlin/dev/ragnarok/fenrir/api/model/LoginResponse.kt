package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LoginResponse {
    // {"error":"need_captcha","captcha_sid":"665120559674","captcha_img":"https:\/\/api.vk.com\/captcha.php?sid=665120559674"}
    @SerialName("access_token")
    var access_token: String? = null

    @SerialName("user_id")
    var user_id = 0

    @SerialName("error")
    var error: String? = null

    @SerialName("error_description")
    var errorDescription: String? = null

    @SerialName("captcha_sid")
    var captchaSid: String? = null

    @SerialName("captcha_img")
    var captchaImg: String? = null

    @SerialName("validation_type")
    var validationType // 2fa_sms or 2fa_app
            : String? = null

    @SerialName("redirect_uri")
    var redirect_uri: String? = null

    @SerialName("phone_mask")
    var phoneMask: String? = null

    @SerialName("validation_sid")
    var validation_sid: String? = null
}