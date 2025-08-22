package dev.ragnarok.fenrir.api.exceptions

class VKIdCaptchaNeedException(val redirect_uri: String, val domain: String) :
    Exception("Captcha required!")
