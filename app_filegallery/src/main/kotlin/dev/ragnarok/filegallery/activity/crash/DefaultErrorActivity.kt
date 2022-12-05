package dev.ragnarok.filegallery.activity.crash

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.slidr.Slidr
import dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig
import dev.ragnarok.filegallery.activity.slidr.model.SlidrListener
import dev.ragnarok.filegallery.settings.CurrentTheme

class DefaultErrorActivity : AppCompatActivity() {
    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.App_CrashError)
        super.onCreate(savedInstanceState)
        Slidr.attach(
            this,
            SlidrConfig.Builder().scrimColor(CurrentTheme.getColorBackground(this))
                .listener(object : SlidrListener {
                    override fun onSlideStateChanged(state: Int) {
                    }

                    override fun onSlideChange(percent: Float) {
                    }

                    override fun onSlideOpened() {
                    }

                    override fun onSlideClosed(): Boolean {
                        CrashUtils.closeApplication(this@DefaultErrorActivity)
                        return true
                    }
                }).build()
        )
        setContentView(R.layout.crash_error_activity)
        findViewById<MaterialButton>(R.id.crash_error_activity_restart_button).setOnClickListener {
            CrashUtils.restartApplication(
                this
            )
        }

        if (intent.getBooleanExtra(Extra.IS_OUT_OF_MEMORY, false)) {
            findViewById<MaterialButton>(R.id.crash_error_activity_more_info_button).visibility =
                View.GONE
            findViewById<ImageView>(R.id.crash_error_activity_bag).visibility = View.GONE
            findViewById<TextView>(R.id.crash_error_activity_throwable).setText(R.string.crash_error_activity_out_of_memory)
        }

        findViewById<MaterialButton>(R.id.crash_error_activity_more_info_button).setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.crash_error_activity_error_details_title)
                .setMessage(CrashUtils.getAllErrorDetailsFromIntent(this, intent))
                .setPositiveButton(R.string.crash_error_activity_error_details_close, null)
                .setNeutralButton(
                    R.string.crash_error_activity_error_details_copy
                ) { _: DialogInterface?, _: Int -> copyErrorToClipboard() }
                .show()
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            textView?.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.crash_error_activity_error_details_text_size)
            )
        }
    }

    private fun copyErrorToClipboard() {
        val errorInformation = CrashUtils.getAllErrorDetailsFromIntent(this, intent)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard != null) {
            val clip = ClipData.newPlainText(
                getString(R.string.crash_error_activity_error_details_clipboard_label),
                errorInformation
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                this,
                R.string.crash_error_activity_error_details_copied,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
