package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.LoginResponseDtoAdapter
import dev.ragnarok.fenrir.api.model.Error
import kotlinx.serialization.Serializable

@Serializable(with = LoginResponseDtoAdapter::class)
class LoginResponse {
    // {"error":"need_captcha","captcha_sid":"665120559674","captcha_img":"https:\/\/api.vk.com\/captcha.php?sid=665120559674"}
    var access_token: String? = null
    var user_id = 0L
    var error: String? = null
    var errorBasic: Error? = null
    var errorDescription: String? = null
    var captchaSid: String? = null
    var captchaImg: String? = null
    var validationType // 2fa_sms or 2fa_app
            : String? = null
    var redirect_uri: String? = null
    var phoneMask: String? = null
    var validation_sid: String? = null
    var expired_at: Long = 0
}