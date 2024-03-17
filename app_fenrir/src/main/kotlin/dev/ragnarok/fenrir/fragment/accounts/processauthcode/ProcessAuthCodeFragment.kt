package dev.ragnarok.fenrir.fragment.accounts.processauthcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.RecyclerMenuAdapter
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast

class ProcessAuthCodeFragment : BaseMvpFragment<ProcessAuthCodePresenter, IProcessAuthCodeView>(),
    IProcessAuthCodeView, RecyclerMenuAdapter.ActionListener {
    private var mAdapter: RecyclerMenuAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root =
            inflater.inflate(R.layout.fragment_process_auth_code, container, false) as ViewGroup
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        PicassoPauseOnScrollListener.addListener(recyclerView)
        mAdapter = RecyclerMenuAdapter(emptyList())
        mAdapter?.setActionListener(this)
        recyclerView.adapter = mAdapter
        root.findViewById<MaterialButton>(R.id.permit_account).setOnClickListener {
            presenter?.permit()
        }
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.auth_by_code)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayData(shortcuts: List<AdvancedItem>) {
        mAdapter?.setItems(shortcuts)
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun success() {
        CustomToast.createCustomToast(requireActivity())
            .setDuration(Toast.LENGTH_LONG)
            .showToastSuccessBottom(R.string.success)

        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ProcessAuthCodePresenter> {
        return object : IPresenterFactory<ProcessAuthCodePresenter> {
            override fun create(): ProcessAuthCodePresenter {
                return ProcessAuthCodePresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getString(Extra.CODE)!!,
                    saveInstanceState
                )
            }
        }
    }

    companion object {
        fun newInstance(args: Bundle?): Fragment {
            val fragment = ProcessAuthCodeFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(accountId: Long, code: String): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putString(Extra.CODE, code)
            return args
        }
    }

    override fun onClick(item: AdvancedItem) {
    }

    override fun onLongClick(item: AdvancedItem) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard != null) {
            val title = item.title?.getText(requireContext())
            val subtitle = item.subtitle?.getText(requireContext())
            val details = Utils.joinNonEmptyStrings("\n", title, subtitle)
            val clip: ClipData =
                if (item.type == AdvancedItem.TYPE_COPY_DETAILS_ONLY || item.type == AdvancedItem.TYPE_OPEN_URL) {
                    ClipData.newPlainText("Details", subtitle)
                } else {
                    ClipData.newPlainText("Details", details)
                }
            clipboard.setPrimaryClip(clip)
            CustomToast.createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
        }
    }
}
