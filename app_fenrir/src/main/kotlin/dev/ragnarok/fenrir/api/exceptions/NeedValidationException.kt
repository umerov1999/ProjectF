package dev.ragnarok.fenrir.api.exceptions

class NeedValidationException(
    val phone: String?,
    val validationType: String?,
    val validationURL: String?,
    val sid: String?,
    val description: String?,
) : Exception("Need Validation $description")