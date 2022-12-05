package dev.ragnarok.filegallery.fragment.tagowner

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.base.BaseMvpBottomSheetDialogFragment
import dev.ragnarok.filegallery.fragment.base.core.IPresenterFactory
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.tags.TagOwner
import dev.ragnarok.filegallery.util.toast.CustomToast

class TagOwnerBottomSheetSelected :
    BaseMvpBottomSheetDialogFragment<TagOwnerPresenter, ITagOwnerView>(),
    ITagOwnerView,
    TagOwnerAdapter.ClickListener {
    private var mAdapter: TagOwnerAdapter? = null
    private var mAdd: FloatingActionButton? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_tag_owners_bottom_sheet, container, false)
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        //behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        return dialog
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
        CustomToast.createCustomToast(requireActivity(), null)?.showToastSuccessBottom(
            getString(
                R.string.success_add,
                item.file_name,
                owner.name
            )
        )
        dismiss()
    }

    override fun onTagOwnerClick(index: Int, owner: TagOwner) {
        val ret = Bundle()
        ret.putParcelable(Extra.NAME, owner)
        setFragmentResult(SELECTED_OWNER_KEY, ret)
        dismiss()
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
        const val SELECTED_OWNER_KEY = "selected_owner_key"
    }
}
