package dev.ragnarok.fenrir.fragment.shortcutsview

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.model.ShortcutStored
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ShortcutUtils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO

class ShortcutsViewPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<IShortcutsView>(savedInstanceState) {
    private val pInteractor: ITempDataStorage = Includes.stores.tempStore()
    private val shortcuts: MutableList<ShortcutStored> = ArrayList()

    private val PATTERN_ACCOUNT: Regex = Regex("fenrir_account_(-?\\d*)")
    private val PATTERN_WALL: Regex = Regex("fenrir_wall_(-?\\d*)_aid_(-?\\d*)")
    private val PATTERN_PEER: Regex = Regex("fenrir_peer_(-?\\d*)_aid_(-?\\d*)")

    override fun onGuiCreated(viewHost: IShortcutsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(shortcuts)
    }

    private fun requestData() {
        appendJob(
            pInteractor.getShortcutAll()
                .fromIOToMain({ onDataReceived(it) }) { t ->
                    onDataGetError(
                        t
                    )
                })
    }

    private fun onDataGetError(t: Throwable) {
        showError(t)
    }

    private fun onDataReceived(shortcuts: List<ShortcutStored>) {
        this.shortcuts.clear()
        this.shortcuts.addAll(shortcuts)
        view?.notifyDataSetChanged()
    }

    private fun tryAccount(context: Context, shortcut: ShortcutStored): Boolean {
        try {
            val matcher = PATTERN_ACCOUNT.find(shortcut.action)
            if (matcher != null) {
                val id = matcher.groupValues.getOrNull(1)?.toLong() ?: return false
                appendJob(
                    Repository.owners.getBaseOwnerInfo(
                        Settings.get().accounts().current,
                        id,
                        IOwnersRepository.MODE_NET
                    )
                        .fromIOToMain({ its ->
                            appendJob(
                                ShortcutUtils.createAccountShortcutRx(
                                    context,
                                    id,
                                    its.fullName.nonNullNoEmpty({ it }, { shortcut.name }),
                                    its.maxSquareAvatar.nonNullNoEmpty({ it }, { shortcut.cover })
                                ).hiddenIO()
                            )
                        }, {
                            appendJob(
                                ShortcutUtils.createAccountShortcutRx(
                                    context,
                                    id,
                                    shortcut.name,
                                    shortcut.cover
                                ).hiddenIO()
                            )
                        })
                )
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun tryWall(context: Context, shortcut: ShortcutStored): Boolean {
        try {
            val matcher = PATTERN_WALL.find(shortcut.action)
            if (matcher != null) {
                val id = matcher.groupValues.getOrNull(1)?.toLong() ?: return false
                val account_id = matcher.groupValues.getOrNull(2)?.toLong() ?: return false
                appendJob(
                    Repository.owners.getBaseOwnerInfo(
                        Settings.get().accounts().current,
                        id,
                        IOwnersRepository.MODE_NET
                    )
                        .fromIOToMain({ its ->
                            appendJob(
                                ShortcutUtils.createWallShortcutRx(
                                    context,
                                    account_id,
                                    id,
                                    its.fullName.nonNullNoEmpty({ it }, { shortcut.name }),
                                    its.maxSquareAvatar.nonNullNoEmpty({ it }, { shortcut.cover })
                                ).hiddenIO()
                            )
                        }, {
                            appendJob(
                                ShortcutUtils.createWallShortcutRx(
                                    context,
                                    account_id,
                                    id,
                                    shortcut.name,
                                    shortcut.cover
                                ).hiddenIO()
                            )
                        })
                )
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun tryPeer(context: Context, shortcut: ShortcutStored): Boolean {
        try {
            val matcher = PATTERN_PEER.find(shortcut.action)
            if (matcher != null) {
                val id = matcher.groupValues.getOrNull(1)?.toLong() ?: return false
                val account_id = matcher.groupValues.getOrNull(2)?.toLong() ?: return false
                appendJob(
                    ShortcutUtils.createChatShortcutRx(
                        context,
                        shortcut.cover,
                        account_id,
                        id,
                        shortcut.name
                    ).hiddenIO()
                )
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    fun fireShortcutClick(context: Context, shortcut: ShortcutStored) {
        when {
            tryAccount(context, shortcut) -> {
                return
            }

            tryWall(context, shortcut) -> {
                return
            }

            tryPeer(context, shortcut) -> {
                return
            }
        }
    }

    fun fireShortcutDeleted(pos: Int, shortcut: ShortcutStored) {
        appendJob(
            pInteractor.deleteShortcut(shortcut.action).fromIOToMain {
                shortcuts.removeAt(pos)
                view?.notifyItemRemoved(pos)
            }
        )
    }

    init {
        requestData()
    }
}
