package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.util.Optional
import io.reactivex.rxjava3.core.Single
import okhttp3.ResponseBody

interface IOtherApi {
    fun rawRequest(method: String, postParams: Map<String, String>): Single<Optional<ResponseBody>>
}
