package dev.ragnarok.fenrir.activity.selectprofiles

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.activity.ProfileSelectable
import dev.ragnarok.fenrir.fragment.fave.FaveTabsFragment
import dev.ragnarok.fenrir.fragment.friends.friendstabs.FriendsTabsFragment
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SelectProfileCriteria
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.MainActivityTransforms
import dev.ragnarok.fenrir.util.Utils

class SelectProfilesActivity : MainActivity(), SelectedProfilesAdapter.ActionListener,
    ProfileSelectable {
    override var acceptableCriteria: SelectProfileCriteria? = null
        private set
    private var mSelectedOwners: ArrayList<Owner>? = null
    private var mRecyclerView: RecyclerView? = null
    private var mProfilesAdapter: SelectedProfilesAdapter? = null

    @MainActivityTransforms
    override fun getMainActivityTransform(): Int {
        return MainActivityTransforms.PROFILES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mLayoutRes = if (Settings.get()
                .other().is_side_navigation()
        ) R.layout.activity_main_with_profiles_selection_side else R.layout.activity_main_with_profiles_selection
        super.onCreate(savedInstanceState)
        mLastBackPressedTime = Long.MAX_VALUE - DOUBLE_BACK_PRESSED_TIMEOUT
        acceptableCriteria = intent.getParcelableExtraCompat(Extra.CRITERIA)
        if (savedInstanceState != null) {
            mSelectedOwners = savedInstanceState.getParcelableArrayListCompat(SAVE_SELECTED_OWNERS)
        }
        if (mSelectedOwners == null) {
            mSelectedOwners = ArrayList()
        }
        val manager: RecyclerView.LayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mProfilesAdapter = SelectedProfilesAdapter(this, mSelectedOwners ?: return)
        mProfilesAdapter?.setActionListener(this)
        mRecyclerView = findViewById(R.id.recycleView)
        checkNotNull(mRecyclerView) { "Invalid view" }
        mRecyclerView?.layoutManager = manager
        mRecyclerView?.adapter = mProfilesAdapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(SAVE_SELECTED_OWNERS, mSelectedOwners)
    }

    override fun onClick(adapterPosition: Int, owner: Owner) {
        mProfilesAdapter?.toDataPosition(adapterPosition)?.let { mSelectedOwners?.removeAt(it) }
        mProfilesAdapter?.notifyItemRemoved(adapterPosition)
        mProfilesAdapter?.notifyHeaderChange()
    }

    override fun onCheckClick() {
        val intent = Intent()
        intent.putParcelableArrayListExtra(Extra.OWNERS, mSelectedOwners)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun select(owner: Owner) {
        Logger.d(TAG, "Select, owner: $owner")
        val index = Utils.indexOfOwner(mSelectedOwners, owner)
        if (index != -1) {
            mSelectedOwners?.removeAt(index)
            mProfilesAdapter?.toAdapterPosition(index)
                ?.let { mProfilesAdapter?.notifyItemRemoved(it) }
        }
        mSelectedOwners?.add(0, owner)
        mProfilesAdapter?.toAdapterPosition(0)?.let { mProfilesAdapter?.notifyItemInserted(it) }
        mProfilesAdapter?.notifyHeaderChange()
        mRecyclerView?.smoothScrollToPosition(0)
    }

    companion object {
        private val TAG = SelectProfilesActivity::class.java.simpleName
        private const val SAVE_SELECTED_OWNERS = "save_selected_owners"


        fun createIntent(
            context: Context,
            initialPlace: Place,
            criteria: SelectProfileCriteria
        ): Intent {
            return Intent(context, SelectProfilesActivity::class.java)
                .setAction(ACTION_OPEN_PLACE)
                .putExtra(Extra.PLACE, initialPlace)
                .putExtra(Extra.CRITERIA, criteria)
        }


        fun startFriendsSelection(context: Context): Intent {
            val aid = Settings.get()
                .accounts()
                .current
            val place = PlaceFactory.getFriendsFollowersPlace(
                aid,
                aid,
                FriendsTabsFragment.TAB_ALL_FRIENDS,
                null
            )
            val criteria =
                SelectProfileCriteria().setOwnerType(SelectProfileCriteria.OwnerType.ONLY_FRIENDS)
            val intent = Intent(context, SelectProfilesActivity::class.java)
            intent.action = ACTION_OPEN_PLACE
            intent.putExtra(Extra.PLACE, place)
            intent.putExtra(Extra.CRITERIA, criteria)
            return intent
        }


        fun startFaveSelection(context: Context): Intent {
            val aid = Settings.get()
                .accounts()
                .current
            val place = PlaceFactory.getBookmarksPlace(aid, FaveTabsFragment.TAB_PAGES)
            val criteria =
                SelectProfileCriteria().setOwnerType(SelectProfileCriteria.OwnerType.OWNERS)
            val intent = Intent(context, SelectProfilesActivity::class.java)
            intent.action = ACTION_OPEN_PLACE
            intent.putExtra(Extra.PLACE, place)
            intent.putExtra(Extra.CRITERIA, criteria)
            return intent
        }
    }
}