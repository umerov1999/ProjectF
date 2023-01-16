package dev.ragnarok.fenrir.dialog.privacyview

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.selectprofiles.SelectProfilesActivity.Companion.startFriendsSelection
import dev.ragnarok.fenrir.dialog.base.AccountDependencyDialogFragment
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.FriendList
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Privacy
import dev.ragnarok.fenrir.model.User

class PrivacyViewDialog : AccountDependencyDialogFragment(), PrivacyAdapter.ActionListener {
    private var mPrivacy: Privacy? = null
    private var mAdapter: PrivacyAdapter? = null
    private val requestSelectUsersAllowed = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val users: ArrayList<Owner>? =
                result.data?.getParcelableArrayListExtraCompat(Extra.OWNERS)
            users ?: return@registerForActivityResult
            for (user in users) {
                if (user is User) {
                    mPrivacy?.allowFor(user)
                }
            }
            safeNotifyDatasetChanged()
        }
    }
    private val requestSelectUsersDisAllowed = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val users: ArrayList<Owner>? =
                result.data?.getParcelableArrayListExtraCompat(Extra.OWNERS)
            users ?: return@registerForActivityResult
            for (user in users) {
                if (user is User) {
                    mPrivacy?.disallowFor(user)
                }
            }
            safeNotifyDatasetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mPrivacy = savedInstanceState.getParcelableCompat(SAVE_PRIVACY)
        }
        if (mPrivacy == null) {
            mPrivacy = clonePrivacyFromArgs()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val root = View.inflate(requireActivity(), R.layout.fragment_privacy_view, null)
        val columns = resources.getInteger(R.integer.privacy_entry_column_count)
        mAdapter = mPrivacy?.let { PrivacyAdapter(requireActivity(), it) }
        mAdapter?.setActionListener(this)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycleView)
        recyclerView.layoutManager =
            StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = mAdapter
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.privacy_settings)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int -> returnResult() }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun returnResult() {
        val intent = Bundle()
        intent.putParcelable(Extra.PRIVACY, mPrivacy)
        parentFragmentManager.setFragmentResult(REQUEST_PRIVACY_VIEW, intent)
    }

    override fun onTypeClick() {
        val items = arrayOf(
            getString(R.string.privacy_to_all_users),
            getString(R.string.privacy_to_friends_only),
            getString(R.string.privacy_to_friends_and_friends_of_friends),
            getString(R.string.privacy_to_only_me)
        )
        MaterialAlertDialogBuilder(requireActivity())
            .setItems(items) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> mPrivacy?.setType(Privacy.Type.ALL)
                    1 -> mPrivacy?.setType(Privacy.Type.FRIENDS)
                    2 -> mPrivacy?.setType(Privacy.Type.FRIENDS_OF_FRIENDS)
                    3 -> mPrivacy?.setType(Privacy.Type.ONLY_ME)
                }
                safeNotifyDatasetChanged()
            }.setNegativeButton(R.string.button_cancel, null).show()
    }

    private fun safeNotifyDatasetChanged() {
        if (isAdded) mAdapter?.notifyDataSetChanged()
    }

    override fun onAllowedUserRemove(user: User) {
        mPrivacy?.removeFromAllowed(user)
        safeNotifyDatasetChanged()
    }

    override fun onAllowedFriendsListRemove(friendList: FriendList) {
        mPrivacy?.removeFromAllowed(friendList)
        safeNotifyDatasetChanged()
    }

    override fun onDisallowedUserRemove(user: User) {
        (mPrivacy ?: return).removeFromDisallowed(user)
        safeNotifyDatasetChanged()
    }

    override fun onDisallowedFriendsListRemove(friendList: FriendList) {
        (mPrivacy ?: return).removeFromDisallowed(friendList)
        safeNotifyDatasetChanged()
    }

    override fun onAddToAllowedClick() {
        requestSelectUsersAllowed.launch(startFriendsSelection(requireActivity()))
    }

    override fun onAddToDisallowedClick() {
        requestSelectUsersDisAllowed.launch(startFriendsSelection(requireActivity()))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SAVE_PRIVACY, mPrivacy)
    }

    private fun clonePrivacyFromArgs(): Privacy {
        val privacy: Privacy = requireArguments().getParcelableCompat(Extra.PRIVACY)
            ?: throw IllegalArgumentException("Args do not contain Privacy extra")
        return try {
            privacy.clone()
        } catch (e: CloneNotSupportedException) {
            privacy
        }
    }

    companion object {
        const val REQUEST_PRIVACY_VIEW = "request_privacy_view"
        private const val SAVE_PRIVACY = "save_privacy"
        fun buildArgs(aid: Long, privacy: Privacy?): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(Extra.PRIVACY, privacy)
            bundle.putLong(Extra.ACCOUNT_ID, aid)
            return bundle
        }

        fun newInstance(args: Bundle?): PrivacyViewDialog {
            val fragment = PrivacyViewDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
