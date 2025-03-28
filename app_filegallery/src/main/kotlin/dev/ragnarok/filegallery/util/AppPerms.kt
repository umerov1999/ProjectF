package dev.ragnarok.filegallery.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.util.toast.CustomToast

object AppPerms {
    @SuppressLint("BatteryLife")
    fun ignoreBattery(context: Context) {
        if (!HelperSimple.needHelp(HelperSimple.BATTERY_OPTIMIZATION, 1)) {
            return
        }
        try {
            val packageName = context.packageName
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
            pm ?: return
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = "package:$packageName".toUri()
                //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasReadWriteStoragePermission(context: Context): Boolean {
        if (Utils.hasScopedStorage()) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${context.packageName}".toUri()
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Process.killProcess(Process.myPid())
                return false
            }
            return true
        }
        val hasWritePermission = PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val hasReadPermission = PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return hasWritePermission == PackageManager.PERMISSION_GRANTED && hasReadPermission == PackageManager.PERMISSION_GRANTED
    }

    fun hasCameraPermission(context: Context): Boolean {
        val hasCameraPermission =
            PermissionChecker.checkSelfPermission(context, Manifest.permission.CAMERA)
        return hasReadWriteStoragePermission(context) && hasCameraPermission == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermissionSimple(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val hasNPermission = PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return hasNPermission == PackageManager.PERMISSION_GRANTED
    }

    inline fun <reified T : Fragment> T.requestPermissionsAbs(
        permissions: Array<String>,
        crossinline granted: () -> Unit
    ): DoRequestPermissions {
        val request = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result: Map<String, Boolean> ->
            if (Utils.checkValues(result.values)) {
                granted.invoke()
            } else {
                CustomToast.createCustomToast(requireActivity(), view)?.setDuration(
                    Toast.LENGTH_LONG
                )?.showToastError(R.string.not_permitted)
            }
        }
        return object : DoRequestPermissions {
            override fun launch() {
                request.launch(permissions)
            }
        }
    }

    inline fun <reified T : AppCompatActivity> T.requestPermissionsAbs(
        permissions: Array<String>,
        crossinline granted: () -> Unit
    ): DoRequestPermissions {
        val request = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result: Map<String, Boolean> ->
            if (Utils.checkValues(result.values)) {
                granted.invoke()
            } else {
                CustomToast.createCustomToast(this, null)?.setDuration(
                    Toast.LENGTH_LONG
                )?.showToastError(R.string.not_permitted)
            }
        }
        return object : DoRequestPermissions {
            override fun launch() {
                request.launch(permissions)
            }
        }
    }

    inline fun <reified T : AppCompatActivity> T.requestPermissionsResultAbs(
        permissions: Array<String>,
        crossinline granted: () -> Unit,
        crossinline denied: () -> Unit
    ): DoRequestPermissions {
        val request = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result: Map<String, Boolean> ->
            if (Utils.checkValues(result.values)) {
                granted.invoke()
            } else {
                CustomToast.createCustomToast(this, null)?.setDuration(
                    Toast.LENGTH_LONG
                )?.showToastError(R.string.not_permitted)
                denied.invoke()
            }
        }
        return object : DoRequestPermissions {
            override fun launch() {
                request.launch(permissions)
            }
        }
    }

    inline fun <reified T : Fragment> T.requestPermissionsResultAbs(
        permissions: Array<String>,
        crossinline granted: () -> Unit,
        crossinline denied: () -> Unit
    ): DoRequestPermissions {
        val request = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result: Map<String, Boolean> ->
            if (Utils.checkValues(result.values)) {
                granted.invoke()
            } else {
                CustomToast.createCustomToast(requireActivity(), view)?.setDuration(
                    Toast.LENGTH_LONG
                )?.showToastError(R.string.not_permitted)
                denied.invoke()
            }
        }
        return object : DoRequestPermissions {
            override fun launch() {
                request.launch(permissions)
            }
        }
    }

    interface DoRequestPermissions {
        fun launch()
    }
}