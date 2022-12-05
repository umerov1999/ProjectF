package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.domain.impl.MessagesRepository
import dev.ragnarok.fenrir.domain.impl.OwnersRepository
import dev.ragnarok.fenrir.domain.impl.WallsRepository
import dev.ragnarok.fenrir.settings.Settings

object Repository {
    val owners: IOwnersRepository by lazy {
        OwnersRepository(Includes.networkInterfaces, Stores.instance.owners())
    }

    val walls: IWallsRepository by lazy {
        WallsRepository(Includes.networkInterfaces, Stores.instance, owners)
    }

    val messages: IMessagesRepository by lazy {
        MessagesRepository(
            Settings.get().accounts(),
            Includes.networkInterfaces,
            owners,
            Includes.stores,
            Includes.uploadManager
        )
    }
}