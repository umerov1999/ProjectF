package dev.ragnarok.fenrir.upload

import dev.ragnarok.fenrir.api.model.server.UploadServer

class UploadResult<T>(val server: UploadServer, val result: T)