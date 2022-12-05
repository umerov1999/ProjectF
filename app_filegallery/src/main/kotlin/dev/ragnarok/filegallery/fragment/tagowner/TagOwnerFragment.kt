package dev.ragnarok.filegallery.fragment.tagowner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.ActivityFeatures
import dev.ragnarok.filegallery.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.filegallery.fragment.base.BaseMvpFragment
import dev.ragnarok.filegallery.fragment.base.core.IPresenterFactory
import dev.ragnarok.filegallery.listener.OnSectionResumeCallback
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.SectionItem
import dev.ragnarok.filegallery.model.tags.TagOwner
import dev.ragnarok.filegallery.place.PlaceFactory
import dev.ragnarok.filegallery.util.toast.CustomToast


class TagOwnerFragment : BaseMvpFragment<TagOwnerPresenter, ITagOwnerView>(), ITagOwnerView,
    TagOwnerAdapter.ClickListener {
    private var mAdapter: TagOwnerAdapter? = null
    private var mAdd: FloatingActionButton? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_tag_owners, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        mAdapter = TagOwnerAdapter(emptyList(), requireActivity())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter

        mAdd = root.findViewById(R.id.add_button)
        mAdd?.setOnClickListener {
            val view = View.inflate(context, R.layout.entry_name, null)
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.title_entry_name)
                .setCancelable(true)
                .setView(view)
                .setPositiveButton(R.string.button_ok) { _, _ ->
                    presenter?.addOwner(
                        (view.findViewById<View>(R.id.edit_name) as TextInputEditText).text.toString()
                            .trim { it <= ' ' })
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
        }
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.tag_owner_title)
        actionBar?.subtitle = null
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(SectionItem.TAGS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<TagOwnerPresenter> =
        object : IPresenterFactory<TagOwnerPresenter> {
            override fun create(): TagOwnerPresenter {
                return TagOwnerPresenter(saveInstanceState)
            }
        }

    override fun displayData(data: List<TagOwner>) {
        mAdapter?.setData(data)
    }

    override fun notifyChanges() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyAdd(index: Int) {
        mAdapter?.notifyItemInserted(index)
    }

    override fun notifyRemove(index: Int) {
        mAdapter?.notifyItemRemoved(index)
    }

    override fun successAdd(owner: TagOwner, item: FileItem) {
        CustomToast.createCustomToast(requireActivity(), view)
            ?.showToastSuccessBottom(getString(R.string.success_add, item.file_name, owner.name))
    }

    override fun onTagOwnerClick(index: Int, owner: TagOwner) {
        PlaceFactory.getTagDirsPlace(owner.id, arguments?.getBoolean(Extra.SELECT) == true)
            .tryOpenWith(requireActivity())
    }

    override fun onTagOwnerDelete(index: Int, owner: TagOwner) {
        presenter?.deleteTagOwner(index, owner)
    }

    override fun onTagOwnerRename(index: Int, owner: TagOwner) {
        val view = View.inflate(context, R.layout.entry_name, null)
        (view.findViewById<View>(R.id.edit_name) as TextInputEditText).setText(owner.name)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.title_entry_name)
            .setCancelable(true)
            .setView(view)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                presenter?.renameTagOwner(
                    (view.findViewById<View>(R.id.edit_name) as TextInputEditText).text.toString()
                        .trim { it <= ' ' }, owner
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    companion object {
        fun buildArgs(isSelect: Boolean): Bundle {
            val args = Bundle()
            args.putBoolean(Extra.SELECT, isSelect)
            return args
        }

        fun newInstance(args: Bundle): TagOwnerFragment {
            val fragment = TagOwnerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}