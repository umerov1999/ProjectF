package dev.ragnarok.fenrir.fragment.requestexecute

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.hideSoftKeyboard
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs

class RequestExecuteFragment : BaseMvpFragment<RequestExecutePresenter, IRequestExecuteView>(),
    IRequestExecuteView {
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        lazyPresenter {
            fireWritePermissionResolved()
        }
    }
    private var mResposeBody: TextInputEditText? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_request_executor, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mResposeBody = root.findViewById(R.id.response_body)
        val methodEditText: TextInputEditText = root.findViewById(R.id.method)
        methodEditText.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireMethodEdit(s)
            }
        })
        val bodyEditText: TextInputEditText = root.findViewById(R.id.body)
        bodyEditText.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireBodyEdit(s)
            }
        })
        root.findViewById<View>(R.id.button_copy).setOnClickListener {
            presenter?.fireCopyClick()
        }
        root.findViewById<View>(R.id.button_save).setOnClickListener {
            presenter?.fireSaveClick()
        }
        root.findViewById<View>(R.id.button_execute).setOnClickListener {
            presenter?.fireExecuteClick()
        }
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<RequestExecutePresenter> {
        return object : IPresenterFactory<RequestExecutePresenter> {
            override fun create(): RequestExecutePresenter {
                return RequestExecutePresenter(
                    requireArguments().getLong(
                        Extra.ACCOUNT_ID
                    ), saveInstanceState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.request_executor_title)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayBody(body: String?) {
        safelySetText(mResposeBody, body)
    }

    override fun hideKeyboard() {
        hideSoftKeyboard(requireActivity())
    }

    override fun requestWriteExternalStoragePermission() {
        requestWritePermission.launch()
    }

    companion object {
        fun newInstance(accountId: Long): RequestExecuteFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = RequestExecuteFragment()
            fragment.arguments = args
            return fragment
        }
    }
}