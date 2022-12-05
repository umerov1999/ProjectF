package dev.ragnarok.filegallery.activity

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

object ActivityUtils {
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
}