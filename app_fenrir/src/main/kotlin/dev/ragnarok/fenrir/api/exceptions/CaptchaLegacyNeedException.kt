package dev.ragnarok.fenrir.api.exceptions

class CaptchaLegacyNeedException(val captchaSid: String?, val captchaImg: String?) :
    Exception("Captcha required!")