package dev.ragnarok.fenrir.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class SaveAccount {
    @SerialName("login")
    var login: String? = null

    @SerialName("password")
    var password: String? = null

    @SerialName("two_factor_auth")
    var two_factor_auth: String? = null
    operator fun set(login: String?, password: String?, two_factor_auth: String?): SaveAccount {
        this.login = login
        this.password = password
        this.two_factor_auth = two_factor_auth
        return this
    }
}