package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.Error
import dev.ragnarok.fenrir.api.model.response.LoginResponse
import dev.ragnarok.fenrir.kJson
import kotlinx.serialization.json.JsonElement

class LoginResponseDtoAdapter : AbsDtoAdapter<LoginResponse>("LoginResponse") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): LoginResponse {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = LoginResponse()
        dto.access_token = optString(json, "access_token")
        dto.user_id = optLong(json, "user_id")
        if (hasObject(json, "error")) {
            dto.errorBasic =
                json["error"]?.let { kJson.decodeFromJsonElement(Error.serializer(), it) }
        } else {
            dto.error = optString(json, "error")
        }
        dto.errorDescription = optString(json, "error_description")
        dto.captchaSid = optString(json, "captcha_sid")
        dto.captchaImg = optString(json, "captcha_img")
        dto.validationType = optString(json, "validation_type")
        dto.redirect_uri = optString(json, "redirect_uri")
        dto.phoneMask = optString(json, "phone_mask")
        dto.validation_sid = optString(json, "validation_sid")
        dto.expired_at = optLong(json, "expired_at")
        return dto
    }

    companion object {
        private val TAG = LoginResponseDtoAdapter::class.simpleName.orEmpty()
    }
}
