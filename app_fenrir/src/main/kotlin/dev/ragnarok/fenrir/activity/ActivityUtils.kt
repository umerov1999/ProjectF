package dev.ragnarok.fenrir.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.getParcelableExtraCompat

object ActivityUtils {
    fun isMimeVideo(mime: String?): Boolean {
        return if (mime.isNullOrEmpty()) false else mime.contains("video/")
    }

    fun isMimeAudio(mime: String?): Boolean {
        return if (mime.isNullOrEmpty()) false else mime.contains("audio/")
    }

    fun checkLocalStreams(activity: Activity): StreamData? {
        val intent = activity.intent ?: return null
        val extras = intent.extras
        val action = intent.action
        val mime = intent.type
        if (extras == null || action == null || mime == null) {
            return null
        }
        if (Intent.ACTION_SEND_MULTIPLE == action) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                return StreamData(
                    intent.getParcelableArrayListExtraCompat(Intent.EXTRA_STREAM),
                    mime
                )
            }
        }
        if (Intent.ACTION_SEND == action) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                val uri = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) {
                    val streams = ArrayList<Uri>(1)
                    streams.add(uri)
                    return StreamData(streams, mime)
                }
            }
        }
        return null
    }

    fun checkLinks(activity: Activity): String? {
        val intent = activity.intent
        var link: String? = null
        if (intent == null) {
            return null
        }
        val extras = intent.extras
        val action = intent.action
        if (extras == null || action == null) {
            return null
        }
        if (Intent.ACTION_SEND == action) {
            if (extras.containsKey(Intent.EXTRA_TEXT)) {
                link = intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        }
        return link
    }

    fun checkInputExist(activity: Activity): Boolean {
        val intent = activity.intent ?: return false
        val extras = intent.extras
        val action = intent.action
        if (extras == null || action == null) {
            return false
        }
        if (Intent.ACTION_SEND_MULTIPLE == action) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                return true
            }
        }
        return if (Intent.ACTION_SEND == action) {
            extras.containsKey(Intent.EXTRA_STREAM) || extras.containsKey(Intent.EXTRA_TEXT)
        } else false
    }

    fun resetInputPhotos(activity: Activity) {
        activity.intent?.removeExtra(Intent.EXTRA_STREAM)
    }

    fun resetInputText(activity: Activity) {
        activity.intent?.removeExtra(Intent.EXTRA_TEXT)
    }

    fun safeHasInputAttachments(activity: Activity): Boolean {
        return activity.intent?.extras?.containsKey(
            MainActivity.EXTRA_INPUT_ATTACHMENTS
        ) == true
    }


    fun supportToolbarFor(fragment: Fragment): ActionBar? {
        return if (fragment.activity == null) {
            null
        } else (fragment.requireActivity() as AppCompatActivity).supportActionBar
    }

    fun setToolbarTitle(fragment: Fragment, @StringRes res: Int) {
        supportToolbarFor(fragment)?.setTitle(res)
    }

    fun setToolbarTitle(fragment: Fragment, title: String?) {
        supportToolbarFor(fragment)?.title = title
    }

    fun setToolbarSubtitle(fragment: Fragment, @StringRes res: Int) {
        supportToolbarFor(fragment)?.setSubtitle(res)
    }

    fun setToolbarSubtitle(fragment: Fragment, title: String?) {
        supportToolbarFor(fragment)?.subtitle = title
    }

    fun hideSoftKeyboard(activity: Activity) {
        val focusedView = activity.currentFocus
        if (focusedView != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(focusedView.windowToken, 0)
        }
    }

    fun hideSoftKeyboard(view: View?) {
        if (view != null) {
            val imm =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    class StreamData(val uris: ArrayList<Uri>?, val mime: String)
}
