package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityoptions

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.MenuProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Day
import dev.ragnarok.fenrir.model.GroupSettings
import dev.ragnarok.fenrir.model.IdOption
import dev.ragnarok.fenrir.util.Month.getMonthTitle
import dev.ragnarok.fenrir.view.MySpinnerView

class CommunityOptionsFragment :
    BaseMvpFragment<CommunityOptionsPresenter, ICommunityOptionsView>(), ICommunityOptionsView,
    MenuProvider {
    private var mName: TextInputEditText? = null
    private var mDescription: TextInputEditText? = null
    private var mCommunityTypeRoot: ViewGroup? = null
    private var mAddress: TextInputEditText? = null
    private var mCategoryRoot: ViewGroup? = null
    private var mCategory: MySpinnerView? = null
    private var mWebsite: TextInputEditText? = null
    private var mPublicDateRoot: ViewGroup? = null
    private var mDay: TextView? = null
    private var mMonth: TextView? = null
    private var mYear: TextView? = null
    private var mType: MaterialButton? = null
    private var mFeedbackCommentsRoot: ViewGroup? = null
    private var mFeedbackComments: MaterialCheckBox? = null
    private var mObsceneFilter: MaterialCheckBox? = null
    private var mObsceneStopWords: MaterialCheckBox? = null
    private var mObsceneStopWordsEditText: TextInputEditText? = null
    private var rAge: RadioGroup? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.community_option_edit, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_save) {
            presenter?.fireButtonSaveClick(
                mName?.editableText.toString().trim(),
                mDescription?.editableText.toString().trim(),
                mAddress?.editableText.toString().trim(),
                mWebsite?.editableText.toString().trim(),
                if (mObsceneFilter?.isChecked == true) 1 else 0,
                if (mObsceneStopWords?.isChecked == true) 1 else 0,
                mObsceneStopWordsEditText?.editableText.toString().trim()
            )
            return true
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_options, container, false)
        mName = root.findViewById(R.id.name)
        mDescription = root.findViewById(R.id.description)
        mCommunityTypeRoot = root.findViewById(R.id.community_type_root)
        mAddress = root.findViewById(R.id.link)
        mCategoryRoot = root.findViewById(R.id.category_root)
        mCategory = root.findViewById(R.id.spinner_category)
        mCategory?.setOnClickListener {
            presenter?.onCategoryClick()
        }
        mWebsite = root.findViewById(R.id.website)
        mPublicDateRoot = root.findViewById(R.id.public_date_root)
        mDay = root.findViewById(R.id.day)
        mDay?.setOnClickListener {
            presenter?.fireDayClick()
        }
        mMonth = root.findViewById(R.id.month)
        mMonth?.setOnClickListener {
            presenter?.fireMonthClick()
        }
        mYear = root.findViewById(R.id.year)
        mYear?.setOnClickListener {
            presenter?.fireYearClick()
        }
        mType = root.findViewById(R.id.type_group)
        mType?.setOnClickListener {
            presenter?.fireAccessClick()
        }
        rAge = root.findViewById(R.id.category_age)
        rAge?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.age_under16 -> presenter?.fireAge(1)
                R.id.age_16_to_18 -> presenter?.fireAge(2)
                R.id.age_after_18 -> presenter?.fireAge(3)
            }
        }
        mFeedbackCommentsRoot = root.findViewById(R.id.feedback_comments_root)
        mFeedbackComments = root.findViewById(R.id.feedback_comments)
        mObsceneFilter = root.findViewById(R.id.obscene_filter)
        mObsceneStopWords = root.findViewById(R.id.obscene_stopwords)
        mObsceneStopWordsEditText = root.findViewById(R.id.obscene_stopwords_values)
        return root
    }

    override fun resolveEdge(age: Int) {
        when (age) {
            1 -> {
                view?.findViewById<MaterialRadioButton>(R.id.age_after_18)?.isChecked = false
                view?.findViewById<MaterialRadioButton>(R.id.age_16_to_18)?.isChecked = false
                view?.findViewById<MaterialRadioButton>(R.id.age_under16)?.isChecked = true
            }

            2 -> {
                view?.findViewById<MaterialRadioButton>(R.id.age_after_18)?.isChecked = false
                view?.findViewById<MaterialRadioButton>(R.id.age_16_to_18)?.isChecked = true
                view?.findViewById<MaterialRadioButton>(R.id.age_under16)?.isChecked = false
            }

            3 -> {
                view?.findViewById<MaterialRadioButton>(R.id.age_after_18)?.isChecked = true
                view?.findViewById<MaterialRadioButton>(R.id.age_16_to_18)?.isChecked = false
                view?.findViewById<MaterialRadioButton>(R.id.age_under16)?.isChecked = false
            }
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityOptionsPresenter> {
        return object : IPresenterFactory<CommunityOptionsPresenter> {
            override fun create(): CommunityOptionsPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val community: Community = requireArguments().getParcelableCompat(Extra.GROUP)!!
                val settings: GroupSettings =
                    requireArguments().getParcelableCompat(Extra.SETTINGS)!!
                return CommunityOptionsPresenter(accountId, community, settings, saveInstanceState)
            }
        }
    }

    override fun displayName(name: String?) {
        safelySetText(mName, name)
    }

    override fun displayDescription(description: String?) {
        safelySetText(mDescription, description)
    }

    override fun setCommunityTypeVisible(visible: Boolean) {
        safelySetVisibleOrGone(mCommunityTypeRoot, visible)
    }

    override fun displayAddress(address: String?) {
        safelySetText(mAddress, address)
    }

    override fun setCategoryVisible(visible: Boolean) {
        safelySetVisibleOrGone(mCategoryRoot, visible)
    }

    override fun displayCategory(categoryText: String?) {
        mCategory?.setValue(categoryText)
    }

    override fun showSelectOptionDialog(requestCode: Int, data: List<IdOption>) {
        val strings = arrayOfNulls<String>(data.size)
        for (i in data.indices) {
            strings[i] = data[i].title
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.select_from_list_title)
            .setItems(strings) { _: DialogInterface?, which: Int ->
                presenter?.fireOptionSelected(
                    requestCode,
                    data[which]
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun displayWebsite(website: String?) {
        safelySetText(mWebsite, website)
    }

    override fun setPublicDateVisible(visible: Boolean) {
        safelySetVisibleOrGone(mPublicDateRoot, visible)
    }

    override fun dislayPublicDate(day: Day) {
        if (day.day > 0) {
            safelySetText(mDay, day.day.toString())
        } else {
            safelySetText(mDay, R.string.day)
        }
        if (day.year > 0) {
            safelySetText(mYear, day.year.toString())
        } else {
            safelySetText(mYear, R.string.year)
        }
        if (day.month > 0) {
            safelySetText(
                mMonth, getMonthTitle(
                    day.month
                )
            )
        } else {
            safelySetText(mMonth, R.string.month)
        }
    }

    override fun setFeedbackCommentsRootVisible(visible: Boolean) {
        safelySetVisibleOrGone(mFeedbackCommentsRoot, visible)
    }

    override fun setFeedbackCommentsChecked(checked: Boolean) {
        safelySetChecked(mFeedbackComments, checked)
    }

    override fun setObsceneFilterChecked(checked: Boolean) {
        safelySetChecked(mObsceneFilter, checked)
    }

    override fun setObsceneStopWordsChecked(checked: Boolean) {
        safelySetChecked(mObsceneStopWords, checked)
    }

    override fun setObsceneStopWordsVisible(visible: Boolean) {
        safelySetVisibleOrGoneView(mObsceneStopWordsEditText, visible)
    }

    override fun displayObsceneStopWords(words: String?) {
        safelySetText(mObsceneStopWordsEditText, words)
    }

    override fun setGroupType(type: Int) {
        when (type) {
            0 -> mType?.setText(R.string.opened)
            1 -> mType?.setText(R.string.closed)
            2 -> mType?.setText(R.string.privated)
        }
    }

    companion object {
        fun newInstance(
            accountId: Long,
            community: Community,
            settings: GroupSettings
        ): CommunityOptionsFragment {
            val args = Bundle()
            args.putParcelable(Extra.GROUP, community)
            args.putParcelable(Extra.SETTINGS, settings)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = CommunityOptionsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}