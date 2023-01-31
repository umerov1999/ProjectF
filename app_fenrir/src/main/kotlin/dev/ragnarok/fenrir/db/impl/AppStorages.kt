package dev.ragnarok.fenrir.db.impl

import android.content.Context
import android.content.ContextWrapper
import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import dev.ragnarok.fenrir.db.interfaces.IAttachmentsStorage
import dev.ragnarok.fenrir.db.interfaces.ICommentsStorage
import dev.ragnarok.fenrir.db.interfaces.IDatabaseStore
import dev.ragnarok.fenrir.db.interfaces.IDialogsStorage
import dev.ragnarok.fenrir.db.interfaces.IDocsStorage
import dev.ragnarok.fenrir.db.interfaces.IFaveStorage
import dev.ragnarok.fenrir.db.interfaces.IFeedStorage
import dev.ragnarok.fenrir.db.interfaces.IFeedbackStorage
import dev.ragnarok.fenrir.db.interfaces.IKeysStorage
import dev.ragnarok.fenrir.db.interfaces.ILocalMediaStorage
import dev.ragnarok.fenrir.db.interfaces.IMessagesStorage
import dev.ragnarok.fenrir.db.interfaces.IOwnersStorage
import dev.ragnarok.fenrir.db.interfaces.IPhotoAlbumsStorage
import dev.ragnarok.fenrir.db.interfaces.IPhotosStorage
import dev.ragnarok.fenrir.db.interfaces.IRelativeshipStorage
import dev.ragnarok.fenrir.db.interfaces.IStickersStorage
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
import dev.ragnarok.fenrir.db.interfaces.ITopicsStore
import dev.ragnarok.fenrir.db.interfaces.IVideoAlbumsStorage
import dev.ragnarok.fenrir.db.interfaces.IVideoStorage
import dev.ragnarok.fenrir.db.interfaces.IWallStorage

class AppStorages(base: Context) : ContextWrapper(base), IStorages {
    private val tempData: ITempDataStorage = TempDataStorage(this)
    private var owners: IOwnersStorage? = null
    private var feed: IFeedStorage? = null
    private var relativeship: IRelativeshipStorage? = null
    private var photos: IPhotosStorage? = null
    private var fave: IFaveStorage? = null
    private var wall: IWallStorage? = null
    private var messages: IMessagesStorage? = null
    private var dialogs: IDialogsStorage? = null
    private var feedback: IFeedbackStorage? = null
    private var localMedia: ILocalMediaStorage? = null
    private var keysPersist: KeysPersistStorage? = null
    private var keysRam: KeysRamStorage? = null
    private var attachments: IAttachmentsStorage? = null

    @Volatile
    private var video: IVideoStorage? = null

    @Volatile
    private var videoAlbums: IVideoAlbumsStorage? = null

    @Volatile
    private var comments: ICommentsStorage? = null

    @Volatile
    private var photoAlbums: IPhotoAlbumsStorage? = null

    @Volatile
    private var topics: ITopicsStore? = null

    @Volatile
    private var docs: IDocsStorage? = null

    @Volatile
    private var stickers: IStickersStorage? = null

    @Volatile
    private var database: IDatabaseStore? = null
    override fun comments(): ICommentsStorage {
        if (comments == null) {
            synchronized(this) {
                if (comments == null) {
                    comments = CommentsStorage(this)
                }
            }
        }
        return comments!!
    }

    override fun photoAlbums(): IPhotoAlbumsStorage {
        if (photoAlbums == null) {
            synchronized(this) {
                if (photoAlbums == null) {
                    photoAlbums = PhotoAlbumsStorage(this)
                }
            }
        }
        return photoAlbums!!
    }

    override fun topics(): ITopicsStore {
        if (topics == null) {
            synchronized(this) {
                if (topics == null) {
                    topics = TopicsStorage(this)
                }
            }
        }
        return topics!!
    }

    override fun docs(): IDocsStorage {
        if (docs == null) {
            synchronized(this) {
                if (docs == null) {
                    docs = DocsStorage(this)
                }
            }
        }
        return docs!!
    }

    override fun stickers(): IStickersStorage {
        if (stickers == null) {
            synchronized(this) {
                if (stickers == null) {
                    stickers = StickersStorage(this)
                }
            }
        }
        return stickers!!
    }

    override fun database(): IDatabaseStore {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    database = DatabaseStorage(this)
                }
            }
        }
        return database!!
    }

    override fun tempStore(): ITempDataStorage {
        return tempData
    }

    override fun videoAlbums(): IVideoAlbumsStorage {
        if (videoAlbums == null) {
            synchronized(this) {
                if (videoAlbums == null) {
                    videoAlbums = VideoAlbumsStorage(this)
                }
            }
        }
        return videoAlbums!!
    }

    override fun videos(): IVideoStorage {
        if (video == null) {
            synchronized(this) {
                if (video == null) {
                    video = VideoStorage(this)
                }
            }
        }
        return video!!
    }

    @Synchronized
    override fun attachments(): IAttachmentsStorage {
        if (attachments == null) {
            attachments = AttachmentsStorage(this)
        }
        return attachments!!
    }

    @Synchronized
    override fun keys(@KeyLocationPolicy policy: Int): IKeysStorage {
        return when (policy) {
            KeyLocationPolicy.PERSIST -> {
                if (keysPersist == null) {
                    keysPersist = KeysPersistStorage(this)
                }
                keysPersist!!
            }

            KeyLocationPolicy.RAM -> {
                if (keysRam == null) {
                    keysRam = KeysRamStorage()
                }
                keysRam!!
            }

            else -> throw IllegalArgumentException("Unsupported key location policy")
        }
    }

    @Synchronized
    override fun localMedia(): ILocalMediaStorage {
        if (localMedia == null) {
            localMedia = LocalMediaStorage(this)
        }
        return localMedia!!
    }

    @Synchronized
    override fun notifications(): IFeedbackStorage {
        if (feedback == null) {
            feedback = FeedbackStorage(this)
        }
        return feedback!!
    }

    @Synchronized
    override fun dialogs(): IDialogsStorage {
        if (dialogs == null) {
            dialogs = DialogsStorage(this)
        }
        return dialogs!!
    }

    @Synchronized
    override fun messages(): IMessagesStorage {
        if (messages == null) {
            messages = MessagesStorage(this)
        }
        return messages!!
    }

    @Synchronized
    override fun wall(): IWallStorage {
        if (wall == null) {
            wall = WallStorage(this)
        }
        return wall!!
    }

    @Synchronized
    override fun fave(): IFaveStorage {
        if (fave == null) {
            fave = FaveStorage(this)
        }
        return fave!!
    }

    @Synchronized
    override fun photos(): IPhotosStorage {
        if (photos == null) {
            photos = PhotosStorage(this)
        }
        return photos!!
    }

    @Synchronized
    override fun relativeship(): IRelativeshipStorage {
        if (relativeship == null) {
            relativeship = RelativeshipStorage(this)
        }
        return relativeship!!
    }

    @Synchronized
    override fun feed(): IFeedStorage {
        if (feed == null) {
            feed = FeedStorage(this)
        }
        return feed!!
    }

    @Synchronized
    override fun owners(): IOwnersStorage {
        if (owners == null) {
            owners = OwnersStorage(this)
        }
        return owners!!
    }
}