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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IDialogsStorage : IStorage {
    fun getUnreadDialogsCount(accountId: Long): Int
    fun observeUnreadDialogsCount(): SharedFlow<Pair<Long, Int>>
    fun findPeerStates(accountId: Long, ids: Collection<Long>): Flow<List<PeerStateEntity>>
    fun setUnreadDialogsCount(accountId: Long, unreadCount: Int)
    fun findPeerDialog(accountId: Long, peerId: Long): Flow<Optional<PeerDialogEntity>>
    fun savePeerDialog(accountId: Long, entity: PeerDialogEntity): Flow<Boolean>
    fun updateDialogKeyboard(
        accountId: Long,
        peerId: Long,
        keyboardEntity: KeyboardEntity?
    ): Flow<Boolean>

    fun getDialogs(criteria: DialogsCriteria): Flow<List<DialogDboEntity>>
    fun removePeerWithId(accountId: Long, peerId: Long): Flow<Boolean>
    fun insertDialogs(
        accountId: Long,
        dbos: List<DialogDboEntity>,
        clearBefore: Boolean
    ): Flow<Boolean>

    /**
     * Получение списка идентификаторов диалогов, информация о которых отсутствует в базе данных
     *
     * @param ids список входящих идентификаторов
     * @return отсутствующие
     */
    fun getMissingGroupChats(accountId: Long, ids: Collection<Long>): Flow<Collection<Long>>
    fun insertChats(accountId: Long, chats: List<VKApiChat>): Flow<Boolean>
    fun applyPatches(accountId: Long, patches: List<PeerPatch>): Flow<Boolean>
    fun findChatById(accountId: Long, peerId: Long): Flow<Optional<Chat>>
}