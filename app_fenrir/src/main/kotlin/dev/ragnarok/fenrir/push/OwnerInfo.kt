package dev.ragnarok.fenrir.push

import android.content.Context
import android.graphics.Bitmap
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.empty
import io.reactivex.rxjava3.core.Single

class OwnerInfo private constructor(val owner: Owner, val avatar: Bitmap?) {
    val user: User
        get() = owner as User
    val community: Community
        get() = owner as Community

    companion object {
        fun getRx(context: Context, accountId: Int, ownerId: Int): Single<OwnerInfo> {
            val app = context.applicationContext
            val interactor = owners
            return interactor.getBaseOwnerInfo(accountId, ownerId, IOwnersRepository.MODE_ANY)
                .flatMap { owner ->
                    Single.fromCallable {
                        NotificationUtils.loadRoundedImage(
                            app,
                            owner.get100photoOrSmaller(),
                            R.drawable.ic_avatar_unknown
                        )
                    }
                        .map { Optional.wrap(it) }
                        .onErrorReturnItem(empty())
                        .map { optional -> OwnerInfo(owner, optional.get()) }
                }
        }
    }
}