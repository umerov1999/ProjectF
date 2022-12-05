package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.model.Error
import dev.ragnarok.fenrir.util.Utils

class ApiException(val error: Error) : Exception(
    Utils.firstNonEmptyString(
        error["method"],
        " "
    ) + ": " + Utils.firstNonEmptyString(error.errorMsg, " ")
)