package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class Captcha(val sid: String?, val img: String?)