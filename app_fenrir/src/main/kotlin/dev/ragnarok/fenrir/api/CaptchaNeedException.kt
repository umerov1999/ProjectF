package dev.ragnarok.fenrir.api

class CaptchaNeedException(val sid: String?, val img: String?) : Exception("Captcha required!")