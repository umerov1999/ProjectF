package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityinfocontacts

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.domain.IGroupSettingsInteractor
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.GroupSettingsInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformUser
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.ContactInfo
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import kotlinx.coroutines.flow.filter

class CommunityInfoContactsPresenter(
    accountId: Long,
    private val groupId: Community,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<ICommunityInfoContactsView>(accountId, savedInstanceState) {
    private val data: MutableList<Manager> = ArrayList()
    private val interactor: IGroupSettingsInteractor =
        GroupSettingsInteractor(networkInterfaces, stores.owners(), owners)
    private var loadingNow = false
    private fun onManagerActionReceived(manager: Manager) {
        val index =
            Utils.findIndexByPredicate(data) { m -> m.user?.getOwnerObjectId() == manager.user?.getOwnerObjectId() }
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

    private fun findByIdU(contacts: List<ContactInfo>, user_id: Long): ContactInfo? {
        for (element in contacts) {
            if (element.userId == user_id) {
                return element
            }
        }
        return null
    }

    private fun onContactsReceived(contacts: List<ContactInfo>) {
        val Ids: MutableList<Long> = ArrayList(contacts.size)
        for (it in contacts) Ids.add(it.userId)
        appendJob(
            networkInterfaces.vkDefault(accountId).users()[Ids, null, Fields.FIELDS_BASE_USER, null]
                .fromIOToMain({
                    setLoadingNow(false)
                    val managers: MutableList<Manager> = ArrayList(it.size)
                    for (user in it) {
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
        appendJob(
            interactor.getContacts(accountId, groupId.id)
                .fromIOToMain({ contacts -> onContactsReceived(contacts) }) { throwable ->
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
        appendJob(
            interactor.getManagers(accountId, groupId.id)
                .fromIOToMain({ managers -> onDataReceived(managers) }) { throwable ->
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

    override fun onGuiResumed() {
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
        appendJob(
            stores
                .owners()
                .observeManagementChanges()
                .filter { it.first == groupId.id }
                .sharedFlowToMain { pair -> onManagerActionReceived(pair.second) })
        requestData()
    }
}