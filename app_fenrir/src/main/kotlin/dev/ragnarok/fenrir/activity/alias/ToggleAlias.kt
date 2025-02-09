package dev.ragnarok.fenrir.activity.alias

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun reset33(context: Context) {
        val settings = ArrayList<PackageManager.ComponentEnabledSetting>()
        for (i in aliases) {
            if (context.packageManager.getComponentEnabledSetting(
                    ComponentName(
                        context,
                        i
                    )
                ) != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
            ) {
                settings.add(
                    PackageManager.ComponentEnabledSetting(
                        ComponentName(context, i),
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        0
                    )
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
            settings.add(
                PackageManager.ComponentEnabledSetting(
                    ComponentName(
                        context,
                        DefaultFenrirAlias::class.java
                    ), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0
                )
            )
        }
        if (settings.isNotEmpty()) {
            context.packageManager.setComponentEnabledSettings(settings)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun toggleTo33(context: Context, v: Class<out Any>) {
        val settings = ArrayList<PackageManager.ComponentEnabledSetting>()
        if (context.packageManager.getComponentEnabledSetting(
                ComponentName(
                    context,
                    DefaultFenrirAlias::class.java
                )
            ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED && !Constants.IS_DEBUG
        ) {
            settings.add(
                PackageManager.ComponentEnabledSetting(
                    ComponentName(context, DefaultFenrirAlias::class.java),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    0
                )
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
                settings.add(
                    PackageManager.ComponentEnabledSetting(
                        ComponentName(context, i),
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        0
                    )
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
            settings.add(
                PackageManager.ComponentEnabledSetting(
                    ComponentName(context, v),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    0
                )
            )
        }
        if (settings.isNotEmpty()) {
            context.packageManager.setComponentEnabledSettings(settings)
        }
    }

    fun reset(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reset33(context)
            return
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            toggleTo33(context, v)
            return
        }
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