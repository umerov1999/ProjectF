package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.api.model.VKApiChat
import dev.ragnarok.fenrir.db.PeerStateEntity
import dev.ragnarok.fenrir.db.model.PeerPatch
import dev.ragnarok.fenrir.db.model.entity.DialogDboEntity
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity
import dev.ragnarok.fenrir.db.model.entity.PeerDialogEntity
import dev.ragnarok.fenrir.model.Chat
import dev.ragnarok.fenrir.model.criteria.DialogsCriteria
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface IDialogsStorage : IStorage {
    fun getUnreadDialogsCount(accountId: Long): Int
    fun observeUnreadDialogsCount(): Observable<Pair<Long, Int>>
    fun findPeerStates(accountId: Long, ids: Collection<Long>): Single<List<PeerStateEntity>>
    fun setUnreadDialogsCount(accountId: Long, unreadCount: Int)
    fun findPeerDialog(accountId: Long, peerId: Long): Single<Optional<PeerDialogEntity>>
    fun savePeerDialog(accountId: Long, entity: PeerDialogEntity): Completable
    fun updateDialogKeyboard(
        accountId: Long,
        peerId: Long,
        keyboardEntity: KeyboardEntity?
    ): Completable

    fun getDialogs(criteria: DialogsCriteria): Single<List<DialogDboEntity>>
    fun removePeerWithId(accountId: Long, peerId: Long): Completable
    fun insertDialogs(
        accountId: Long,
        dbos: List<DialogDboEntity>,
        clearBefore: Boolean
    ): Completable

    /**
     * Получение списка идентификаторов диалогов, информация о которых отсутствует в базе данных
     *
     * @param ids список входящих идентификаторов
     * @return отсутствующие
     */
    fun getMissingGroupChats(accountId: Long, ids: Collection<Long>): Single<Collection<Long>>
    fun insertChats(accountId: Long, chats: List<VKApiChat>): Completable
    fun applyPatches(accountId: Long, patches: List<PeerPatch>): Completable
    fun findChatById(accountId: Long, peerId: Long): Single<Optional<Chat>>
}