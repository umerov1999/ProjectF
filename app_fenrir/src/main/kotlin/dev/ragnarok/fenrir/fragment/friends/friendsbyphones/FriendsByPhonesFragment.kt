package dev.ragnarok.fenrir.fragment.friends.friendsbyphones

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants.PICASSO_TAG
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.FileManagerSelectActivity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.ContactConversation
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.trimmedIsNullOrEmpty
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.MySearchView
import java.io.File

class FriendsByPhonesFragment : BaseMvpFragment<FriendsByPhonesPresenter, IFriendsByPhonesView>(),
    ContactsAdapter.ClickListener, IFriendsByPhonesView, MenuProvider {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: ContactsAdapter? = null
    private var mEmpty: TextView? = null

    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        startExportContacts()
    }

    private val requestReadPermission = requestPermissionsAbs(
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ) {
        startImportContacts()
    }

    @Suppress("DEPRECATION")
    private fun startExportContacts() {
        exportPhones.launch(
            FileManagerSelectActivity.makeFileManager(
                requireActivity(),
                Environment.getExternalStorageDirectory().absolutePath,
                "dirs"
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun startImportContacts() {
        importContacts.launch(
            FileManagerSelectActivity.makeFileManager(
                requireActivity(),
                Environment.getExternalStorageDirectory().absolutePath,
                "json"
            )
        )
    }

    private val importContacts = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            result.data?.getStringExtra(Extra.PATH)?.let { presenter?.fireImportContacts(it) }
        }
    }

    private val exportPhones =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val file = File(
                    result.data?.getStringExtra(Extra.PATH),
                    "vk_phone_numbers.json"
                )
                presenter?.fireExport(file)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_friends_by_phones, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmpty = root.findViewById(R.id.empty)
        recyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(PICASSO_TAG))
        mAdapter = ContactsAdapter(requireActivity(), emptyList())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()

        val mySearchView: MySearchView = root.findViewById(R.id.searchview)
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireQuery(
                    query
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireQuery(
                    newText
                )
                return false
            }
        })

        root.findViewById<FloatingActionButton>(R.id.sync_button).let {
            it.setOnClickListener {
                presenter?.fireRefresh(requireActivity())
            }
            it.visibility = if (Utils.isHiddenCurrent) View.GONE else View.VISIBLE
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_contacts, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.action_reset).isVisible = !Utils.isHiddenCurrent
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_reset -> {
                presenter?.fireReset()
            }

            R.id.action_read -> {
                if (!AppPerms.hasReadStoragePermission(requireActivity())) {
                    requestReadPermission.launch()
                    return true
                }
                startImportContacts()
                return true
            }

            R.id.action_export -> {
                if (!AppPerms.hasReadWriteStoragePermission(requireActivity())) {
                    requestWritePermission.launch()
                    return true
                }
                startExportContacts()
                return true
            }
        }
        return false
    }

    override fun displayData(owners: List<ContactConversation>) {
        if (mAdapter != null) {
            mAdapter?.setItems(owners)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    override fun displayLoading(loading: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = loading }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.friends_by_phone)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FriendsByPhonesPresenter> {
        return object : IPresenterFactory<FriendsByPhonesPresenter> {
            override fun create(): FriendsByPhonesPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                return FriendsByPhonesPresenter(accountId, requireActivity(), saveInstanceState)
            }
        }
    }

    override fun onContactClick(contact: ContactConversation) {
        presenter?.onUserOwnerClicked(
            contact
        )
    }

    override fun onContactLongClick(contact: ContactConversation): Boolean {
        if (contact.phone.trimmedIsNullOrEmpty()) {
            return false
        }
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("response", contact.phone)
        clipboard?.setPrimaryClip(clip)
        CustomToast.createCustomToast(requireActivity()).showToast(R.string.copied)
        return true
    }

    override fun showChat(accountId: Long, owner: ContactConversation) {
        PlaceFactory.getChatPlace(
            accountId,
            accountId,
            Peer(owner.id).setTitle(owner.title).setAvaUrl(owner.photo)
        ).tryOpenWith(requireActivity())
    }

    companion object {
        fun newInstance(args: Bundle?): FriendsByPhonesFragment {
            val fragment = FriendsByPhonesFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(accountId: Long): FriendsByPhonesFragment {
            return newInstance(buildArgs(accountId))
        }

        fun buildArgs(accountId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            return args
        }
    }
}
