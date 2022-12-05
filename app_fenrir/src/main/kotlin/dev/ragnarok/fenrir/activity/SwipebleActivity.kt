package dev.ragnarok.fenrir.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dev.ragnarok.fenrir.activity.slidr.Slidr.attach
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.MainActivityTransforms
import dev.ragnarok.fenrir.util.ViewUtils

/**
 * Тот же MainActivity, предназначенный для шаринга контента
 * Отличие только в том, что этот активити может существовать в нескольких экземплярах
 */
class SwipebleActivity : MainActivity() {
    @MainActivityTransforms
    override fun getMainActivityTransform(): Int {
        return MainActivityTransforms.SWIPEBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attach(
            this,
            SlidrConfig.Builder().scrimColor(CurrentTheme.getColorBackground(this)).build()
        )
        // потому, что в onBackPressed к этому числу будут прибавлять 2000 !!!! и выход за границы
        mLastBackPressedTime = Long.MAX_VALUE - DOUBLE_BACK_PRESSED_TIMEOUT
    }

    public override fun onDestroy() {
        ViewUtils.keyboardHide(this)
        super.onDestroy()
    }

    companion object {

        fun applyIntent(intent: Intent) {
            intent.putExtra(EXTRA_NO_REQUIRE_PIN, true)
        }


        fun start(context: Context, intent: Intent) {
            intent.putExtra(EXTRA_NO_REQUIRE_PIN, true)
            context.startActivity(intent)
        }
    }
}