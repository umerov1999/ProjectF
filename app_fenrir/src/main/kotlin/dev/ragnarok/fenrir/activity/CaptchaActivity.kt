package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.ICaptchaProvider
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.settings.theme.ThemeOverlay
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.toast.CustomToast
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CaptchaActivity : AppCompatActivity() {
    private val mCompositeDisposable = CompositeDisposable()
    private var mTextField: TextInputEditText? = null
    private var captchaProvider: ICaptchaProvider? = null
    private var requestSid: String? = null
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        @StyleRes val theme: Int = when (Settings.get().main().themeOverlay) {
            ThemeOverlay.AMOLED -> R.style.QuickReply_Amoled
            ThemeOverlay.MD1 -> R.style.QuickReply_MD1
            ThemeOverlay.OFF -> R.style.QuickReply
            else -> R.style.QuickReply
        }
        setTheme(theme)
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        setContentView(R.layout.activity_captcha)
        val imageView =
            findViewById<ImageView>(R.id.captcha_view)
        mTextField = findViewById(R.id.captcha_text)
        val image = intent.getStringExtra(Extra.CAPTCHA_URL)

        //onSuccess, w: 130, h: 50
        with()
            .load(image)
            .into(imageView)
        findViewById<View>(R.id.button_cancel).setOnClickListener { cancel() }
        findViewById<View>(R.id.button_ok).setOnClickListener { onOkButtonClick() }
        requestSid = intent.getStringExtra(Extra.CAPTCHA_SID)
        captchaProvider = Includes.captchaProvider

        captchaProvider?.let {
            mCompositeDisposable.add(
                it.observeWaiting()
                    .filter { ob -> ob == requestSid }
                    .observeOn(provideMainThreadScheduler())
                    .subscribe({ onWaitingRequestReceived() }, RxUtils.ignore())
            )
            mCompositeDisposable.add(
                it.observeCanceling()
                    .filter { ob -> ob == requestSid }
                    .observeOn(provideMainThreadScheduler())
                    .subscribe({ onRequestCancelled() }, RxUtils.ignore())
            )
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
    }

    private fun cancel() {
        requestSid?.let { captchaProvider?.cancel(it) }
        finish()
    }

    private fun onRequestCancelled() {
        finish()
    }

    private fun onWaitingRequestReceived() {
        requestSid?.let { captchaProvider?.notifyThatCaptchaEntryActive(it) }
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    private fun onOkButtonClick() {
        val text: CharSequence? = mTextField?.text
        if (text.isNullOrEmpty()) {
            CustomToast.createCustomToast(this).showToastError(R.string.enter_captcha_text)
            return
        }
        requestSid?.let { captchaProvider?.enterCode(it, text.toString()) }
        finish()
    }

    companion object {

        fun createIntent(context: Context, captchaSid: String?, captchaImg: String?): Intent {
            return Intent(context, CaptchaActivity::class.java)
                .putExtra(Extra.CAPTCHA_SID, captchaSid)
                .putExtra(Extra.CAPTCHA_URL, captchaImg)
        }
    }
}