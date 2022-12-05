package dev.ragnarok.fenrir.activity.alias

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dev.ragnarok.fenrir.Constants

object ToggleAlias {
    private val aliases: Array<Class<out Any>> =
        arrayOf(
            BlueFenrirAlias::class.java,
            GreenFenrirAlias::class.java,
            VioletFenrirAlias::class.java,
            RedFenrirAlias::class.java,
            YellowFenrirAlias::class.java,
            BlackFenrirAlias::class.java,
            VKFenrirAlias::class.java,
            WhiteFenrirAlias::class.java,
            LineageFenrirAlias::class.java
        )

    fun reset(context: Context) {
        for (i in aliases) {
            if (context.packageManager.getComponentEnabledSetting(
                    ComponentName(
                        context,
                        i
                    )
                ) != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            ) {
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, i),
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
        if (context.packageManager.getComponentEnabledSetting(
                ComponentName(
                    context,
                    DefaultFenrirAlias::class.java
                )
            ) != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        ) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, DefaultFenrirAlias::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun toggleTo(context: Context, v: Class<out Any>) {
        if (context.packageManager.getComponentEnabledSetting(
                ComponentName(
                    context,
                    DefaultFenrirAlias::class.java
                )
            ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED && !Constants.IS_DEBUG
        ) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, DefaultFenrirAlias::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        for (i in aliases) {
            if (i == v) {
                continue
            }
            if (context.packageManager.getComponentEnabledSetting(
                    ComponentName(
                        context,
                        i
                    )
                ) != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            ) {
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, i),
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
        if (context.packageManager.getComponentEnabledSetting(
                ComponentName(
                    context,
                    v
                )
            ) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        ) {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, v),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}