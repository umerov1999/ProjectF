package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.api.model.VKApiChat
import dev.ragnarok.fenrir.db.PeerStateEntity
import dev.ragnarok.fenrir.db.model.PeerPatch
import dev.ragnarok.fenrir.db.model.entity.DialogDboEntity
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity
import dev.ragnarok.fenrir.db.model.entity.SimpleDialogEntity
import dev.ragnarok.fenrir.model.Chat
import dev.ragnarok.fenrir.model.criteria.DialogsCriteria
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface IDialogsStorage : IStorage {
    fun getUnreadDialogsCount(accountId: Int): Int
    fun observeUnreadDialogsCount(): Observable<Pair<Int, Int>>
    fun findPeerStates(accountId: Int, ids: Collection<Int>): Single<List<PeerStateEntity>>
    fun setUnreadDialogsCount(accountId: Int, unreadCount: Int)
    fun findSimple(accountId: Int, peerId: Int): Single<Optional<SimpleDialogEntity>>
    fun saveSimple(accountId: Int, entity: SimpleDialogEntity): Completable
    fun updateDialogKeyboard(
        accountId: Int,
        peerId: Int,
        keyboardEntity: KeyboardEntity?
    ): Completable

    fun getDialogs(criteria: DialogsCriteria): Single<List<DialogDboEntity>>
    fun removePeerWithId(accountId: Int, peerId: Int): Completable
    fun insertDialogs(
        accountId: Int,
        dbos: List<DialogDboEntity>,
        clearBefore: Boolean
    ): Completable

    /**
     * Получение списка идентификаторов диалогов, информация о которых отсутствует в базе данных
     *
     * @param ids список входящих идентификаторов
     * @return отсутствующие
     */
    fun getMissingGroupChats(accountId: Int, ids: Collection<Int>): Single<Collection<Int>>
    fun insertChats(accountId: Int, chats: List<VKApiChat>): Completable
    fun applyPatches(accountId: Int, patches: List<PeerPatch>): Completable
    fun findChatById(accountId: Int, peerId: Int): Single<Optional<Chat>>
}