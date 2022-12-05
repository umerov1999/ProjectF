package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.Includes.blacklistRepository
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.settings
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.*
import dev.ragnarok.fenrir.settings.Settings

object InteractorFactory {
    fun createNewsfeedInteractor(): INewsfeedInteractor {
        return NewsfeedInteractor(networkInterfaces, owners)
    }

    fun createStickersInteractor(): IStickersInteractor {
        return StickersInteractor(networkInterfaces, stores.stickers())
    }

    fun createPollInteractor(): IPollInteractor {
        return PollInteractor(networkInterfaces)
    }

    fun createDocsInteractor(): IDocsInteractor {
        return DocsInteractor(networkInterfaces, stores.docs())
    }

    fun createLikesInteractor(): ILikesInteractor {
        return LikesInteractor(networkInterfaces)
    }

    fun createFeedbackInteractor(): IFeedbackInteractor {
        return FeedbackInteractor(stores, networkInterfaces, owners)
    }

    fun createDatabaseInteractor(): IDatabaseInteractor {
        return DatabaseInteractor(stores.database(), networkInterfaces)
    }

    fun createCommunitiesInteractor(): ICommunitiesInteractor {
        return CommunitiesInteractor(networkInterfaces, stores)
    }

    fun createBoardInteractor(): IBoardInteractor {
        return BoardInteractor(networkInterfaces, stores, owners)
    }

    fun createUtilsInteractor(): IUtilsInteractor {
        return UtilsInteractor(networkInterfaces, stores, owners)
    }

    fun createRelationshipInteractor(): IRelationshipInteractor {
        return RelationshipInteractor(stores, networkInterfaces)
    }

    fun createFeedInteractor(): IFeedInteractor {
        return FeedInteractor(networkInterfaces, stores, Settings.get().other(), owners)
    }

    fun createGroupSettingsInteractor(): IGroupSettingsInteractor {
        return GroupSettingsInteractor(networkInterfaces, stores.owners(), owners)
    }

    fun createDialogsInteractor(): IDialogsInteractor {
        return DialogsInteractor(networkInterfaces, stores)
    }

    fun createVideosInteractor(): IVideosInteractor {
        return VideosInteractor(networkInterfaces, stores)
    }


    fun createAccountInteractor(): IAccountsInteractor {
        return AccountsInteractor(
            networkInterfaces,
            settings.accounts(),
            blacklistRepository,
            owners
        )
    }

    fun createPhotosInteractor(): IPhotosInteractor {
        return PhotosInteractor(networkInterfaces, stores)
    }

    fun createFaveInteractor(): IFaveInteractor {
        return FaveInteractor(networkInterfaces, stores, owners)
    }

    fun createLocalServerInteractor(): ILocalServerInteractor {
        return LocalServerInteractor(networkInterfaces)
    }

    fun createAudioInteractor(): IAudioInteractor {
        return AudioInteractor(networkInterfaces)
    }
}