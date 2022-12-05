package dev.ragnarok.fenrir.fragment.communitycontrol.communityinfocontacts

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.db.column.UserColumns
import dev.ragnarok.fenrir.domain.IGroupSettingsInteractor
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.GroupSettingsInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUser
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.ContactInfo
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull

class CommunityInfoContactsPresenter(
    accountId: Int,
    groupId: Community,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<ICommunityInfoContactsView>(accountId, savedInstanceState) {
    private val groupId: Community
    private val data: MutableList<Manager>
    private val interactor: IGroupSettingsInteractor
    private var loadingNow = false
    private fun onManagerActionReceived(manager: Manager) {
        val index =
            Utils.findIndexByPredicate(data) { m -> m.user?.getObjectId() == manager.user?.getObjectId() }
        val removing = manager.role.isNullOrEmpty()
        if (index != -1) {
            if (removing) {
                data.removeAt(index)
                view?.notifyItemRemoved(
                    index
                )
            } else {
                data[index] = manager
                view?.notifyItemChanged(
                    index
                )
            }
        } else {
            if (!removing) {
                data.add(0, manager)
                view?.notifyItemAdded(
                    0
                )
            }
        }
    }

    private fun findByIdU(contacts: List<ContactInfo>, user_id: Int): ContactInfo? {
        for (element in contacts) {
            if (element.getUserId() == user_id) {
                return element
            }
        }
        return null
    }

    private fun onContactsReceived(contacts: List<ContactInfo>) {
        val Ids: MutableList<Int> = ArrayList(contacts.size)
        for (it in contacts) Ids.add(it.getUserId())
        appendDisposable(
            networkInterfaces.vkDefault(accountId).users()[Ids, null, UserColumns.API_FIELDS, null]
                .fromIOToMain()
                .subscribe({ t: List<VKApiUser>? ->
                    setLoadingNow(false)
                    val users = listEmptyIfNull(t)
                    val managers: MutableList<Manager> = ArrayList(users.size)
                    for (user in users) {
                        val contact = findByIdU(contacts, user.id)
                        val manager = Manager(transformUser(user), user.role)
                        if (contact != null) {
                            manager.setDisplayAsContact(true).setContactInfo(contact)
                        }
                        managers.add(manager)
                        onDataReceived(managers)
                    }
                }) { throwable -> onRequestError(throwable) })
    }

    private fun requestContacts() {
        appendDisposable(interactor.getContacts(accountId, groupId.id)
            .fromIOToMain()
            .subscribe({ contacts -> onContactsReceived(contacts) }) { throwable ->
                onRequestError(
                    throwable
                )
            })
    }

    private fun requestData() {
        setLoadingNow(true)
        if (groupId.adminLevel < VKApiCommunity.AdminLevel.ADMIN) {
            requestContacts()
            return
        }
        appendDisposable(interactor.getManagers(accountId, groupId.id)
            .fromIOToMain()
            .subscribe({ managers -> onDataReceived(managers) }) { throwable ->
                onRequestError(
                    throwable
                )
            })
    }

    override fun onGuiCreated(viewHost: ICommunityInfoContactsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            loadingNow
        )
    }

    private fun onRequestError(throwable: Throwable) {
        setLoadingNow(false)
        showError(throwable)
    }

    private fun onDataReceived(managers: List<Manager>) {
        setLoadingNow(false)
        data.clear()
        data.addAll(managers)
        view?.notifyDataSetChanged()
    }

    fun fireRefresh() {
        requestData()
    }

    fun fireManagerClick(manager: User) {
        view?.showUserProfile(
            accountId,
            manager
        )
    }

    init {
        interactor = GroupSettingsInteractor(networkInterfaces, stores.owners(), owners)
        this.groupId = groupId
        data = ArrayList()
        appendDisposable(stores
            .owners()
            .observeManagementChanges()
            .filter { it.first == groupId.id }
            .observeOn(provideMainThreadScheduler())
            .subscribe({ pair -> onManagerActionReceived(pair.second) }) { obj -> obj.printStackTrace() })
        requestData()
    }
}