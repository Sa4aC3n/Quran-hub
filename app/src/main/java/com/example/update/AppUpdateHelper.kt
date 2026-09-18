package com.example.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI State representing the current status of the In-App Update flow.
 */
sealed class AppUpdateState {
    object Idle : AppUpdateState()
    object Checking : AppUpdateState()

    data class UpdateAvailable(
        val appUpdateInfo: AppUpdateInfo,
        val currentVersionName: String,
        val currentVersionCode: Long,
        val availableVersionCode: Int,
        val isImmediateSupported: Boolean,
        val isFlexibleSupported: Boolean
    ) : AppUpdateState()

    data class UpToDate(
        val versionName: String,
        val versionCode: Long
    ) : AppUpdateState()

    data class NoInternet(
        val message: String = "تعذر التحقق من وجود تحديث. يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى."
    ) : AppUpdateState()

    data class Error(
        val title: String = "تعذر التحقق من وجود تحديث",
        val message: String = "يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى.",
        val canOpenStoreDirectly: Boolean = true
    ) : AppUpdateState()

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long,
        val progressPercent: Int
    ) : AppUpdateState()

    object Downloaded : AppUpdateState()
}

/**
 * Helper class managing Google Play In-App Updates and store deep linking.
 */
class AppUpdateHelper(context: Context) {

    private val appContext = context.applicationContext
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    private var installStateUpdatedListener: InstallStateUpdatedListener? = null

    companion object {
        private const val TAG = "AppUpdateHelper"
        const val UPDATE_REQUEST_CODE = 9001
    }

    init {
        setupInstallListener()
    }

    private fun setupInstallListener() {
        installStateUpdatedListener = InstallStateUpdatedListener { state: InstallState ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val bytes = state.bytesDownloaded()
                    val total = state.totalBytesToDownload()
                    val progress = if (total > 0) ((bytes * 100) / total).toInt() else 0
                    _updateState.value = AppUpdateState.Downloading(bytes, total, progress)
                }
                InstallStatus.DOWNLOADED -> {
                    _updateState.value = AppUpdateState.Downloaded
                }
                InstallStatus.FAILED -> {
                    _updateState.value = AppUpdateState.Error(
                        title = "فشل تنزيل التحديث",
                        message = "حدث خطأ أثناء تنزيل التحديث من Google Play. يمكنك التحديث مباشرة من المتجر.",
                        canOpenStoreDirectly = true
                    )
                }
                InstallStatus.CANCELED -> {
                    _updateState.value = AppUpdateState.Idle
                }
                else -> Unit
            }
        }
        installStateUpdatedListener?.let { appUpdateManager.registerListener(it) }
    }

    /**
     * Checks if there is an active internet connection.
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves current installed app version information dynamically from PackageInfo.
     */
    fun getCurrentVersionInfo(): Pair<String, Long> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
            val versionName = packageInfo.versionName ?: "2.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Pair("2.0", 2L)
        }
    }

    /**
     * Initiates checking for app updates using Google Play In-App Updates API.
     */
    fun checkForUpdate() {
        // 1. Check network connection first
        if (!isNetworkAvailable()) {
            _updateState.value = AppUpdateState.NoInternet()
            return
        }

        _updateState.value = AppUpdateState.Checking

        val (currentVersionName, currentVersionCode) = getCurrentVersionInfo()

        try {
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                val availability = appUpdateInfo.updateAvailability()
                Log.d(TAG, "Update availability: $availability, availableVersion: ${appUpdateInfo.availableVersionCode()}")

                when (availability) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        val isImmediate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        val isFlexible = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                        _updateState.value = AppUpdateState.UpdateAvailable(
                            appUpdateInfo = appUpdateInfo,
                            currentVersionName = currentVersionName,
                            currentVersionCode = currentVersionCode,
                            availableVersionCode = appUpdateInfo.availableVersionCode(),
                            isImmediateSupported = isImmediate,
                            isFlexibleSupported = isFlexible
                        )
                    }

                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        // Resume update if previously initiated
                        val isImmediate = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        val isFlexible = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                        _updateState.value = AppUpdateState.UpdateAvailable(
                            appUpdateInfo = appUpdateInfo,
                            currentVersionName = currentVersionName,
                            currentVersionCode = currentVersionCode,
                            availableVersionCode = appUpdateInfo.availableVersionCode(),
                            isImmediateSupported = isImmediate,
                            isFlexibleSupported = isFlexible
                        )
                    }

                    UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                        _updateState.value = AppUpdateState.UpToDate(
                            versionName = currentVersionName,
                            versionCode = currentVersionCode
                        )
                    }

                    else -> {
                        _updateState.value = AppUpdateState.UpToDate(
                            versionName = currentVersionName,
                            versionCode = currentVersionCode
                        )
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to check for update", exception)
                if (!isNetworkAvailable()) {
                    _updateState.value = AppUpdateState.NoInternet()
                } else {
                    _updateState.value = AppUpdateState.Error(
                        title = "تعذر التحقق من وجود تحديث",
                        message = "تعذر الاتصال بـ Google Play للتحقق من التحديث. يمكنك فحص صفحة التطبيق بالمتجر يدوياً.",
                        canOpenStoreDirectly = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initiating update check", e)
            if (!isNetworkAvailable()) {
                _updateState.value = AppUpdateState.NoInternet()
            } else {
                _updateState.value = AppUpdateState.Error(
                    title = "تعذر التحقق من وجود تحديث",
                    message = "يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى.",
                    canOpenStoreDirectly = true
                )
            }
        }
    }

    /**
     * Starts the update flow using the appropriate update mode (Immediate, Flexible, or Play Store fallback).
     */
    fun startUpdate(activity: Activity) {
        val currentState = _updateState.value
        if (currentState !is AppUpdateState.UpdateAvailable) {
            openPlayStore(activity)
            return
        }

        val appUpdateInfo = currentState.appUpdateInfo

        try {
            when {
                currentState.isImmediateSupported -> {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE
                    )
                }
                currentState.isFlexibleSupported -> {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        UPDATE_REQUEST_CODE
                    )
                }
                else -> {
                    // Fallback to Google Play Store page
                    openPlayStore(activity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start update flow, falling back to Play Store", e)
            openPlayStore(activity)
        }
    }

    /**
     * Completes a flexible update when the download is finished.
     */
    fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete update", e)
        }
    }

    /**
     * Safely opens the app's official Google Play Store page.
     */
    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        val marketUri = Uri.parse("market://details?id=$packageName")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    /**
     * Dismisses any active dialog and resets the state to Idle.
     */
    fun dismissDialog() {
        _updateState.value = AppUpdateState.Idle
    }

    /**
     * Cleans up listeners.
     */
    fun onDestroy() {
        installStateUpdatedListener?.let {
            try {
                appUpdateManager.unregisterListener(it)
            } catch (e: Exception) {
                // Ignore unregister errors
            }
        }
    }
}
