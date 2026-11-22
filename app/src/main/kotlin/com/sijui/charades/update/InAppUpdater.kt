package com.sijui.charades.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sijui.charades.MainActivity
import com.sijui.charades.data.AppUpdateInfo
import com.sijui.charades.data.SijuiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface UpdateProgressState {
    data object Idle : UpdateProgressState
    data class Downloading(
        val percent: Int,
        val downloadedMb: Float,
        val totalMb: Float,
    ) : UpdateProgressState

    data class NeedsInstallPermission(val apkFile: File) : UpdateProgressState
    data object Installing : UpdateProgressState
    data class Failed(val message: String) : UpdateProgressState
}

object InAppUpdateManager {
    private const val PREFS_NAME = "sijui_update_prefs"
    private const val KEY_PENDING_RELAUNCH = "pending_relaunch"
    private const val UPDATES_DIR = "updates"
    private const val UPDATE_APK_NAME = "sijui-update.apk"

    private val _state = MutableStateFlow<UpdateProgressState>(UpdateProgressState.Idle)
    val state: StateFlow<UpdateProgressState> = _state.asStateFlow()

    fun reset() {
        _state.value = UpdateProgressState.Idle
    }

    fun cleanupOldApks(context: Context) {
        runCatching {
            val dir = File(context.cacheDir, UPDATES_DIR)
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun unknownSourcesSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )
    }

    suspend fun downloadAndInstall(
        context: Context,
        api: SijuiApiClient,
        update: AppUpdateInfo,
    ) {
        val appCtx = context.applicationContext
        _state.value = UpdateProgressState.Downloading(0, 0f, (update.fileSizeBytes / (1024f * 1024f)))

        val apkFile = try {
            val dir = File(appCtx.cacheDir, UPDATES_DIR).apply { mkdirs() }
            val target = File(dir, UPDATE_APK_NAME)
            if (target.exists()) target.delete()

            api.downloadApkToFile(
                url = update.downloadUrl,
                destFile = target,
                expectedSizeBytes = update.fileSizeBytes,
            ) { downloaded, total ->
                val effectiveTotal = if (total > 0L) total else update.fileSizeBytes
                val pct = if (effectiveTotal > 0L) {
                    ((downloaded * 100L) / effectiveTotal).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                _state.value = UpdateProgressState.Downloading(
                    percent = pct,
                    downloadedMb = downloaded / (1024f * 1024f),
                    totalMb = if (effectiveTotal > 0L) effectiveTotal / (1024f * 1024f) else 0f,
                )
            }
            target
        } catch (e: Exception) {
            _state.value = UpdateProgressState.Failed(e.message ?: "Download failed. Please try again.")
            return
        }

        if (!canInstallPackages(appCtx)) {
            _state.value = UpdateProgressState.NeedsInstallPermission(apkFile)
            return
        }

        installApk(appCtx, apkFile)
    }

    suspend fun resumeInstallIfPermitted(context: Context) {
        val current = _state.value
        if (current is UpdateProgressState.NeedsInstallPermission) {
            if (canInstallPackages(context)) {
                installApk(context.applicationContext, current.apkFile)
            }
        }
    }

    suspend fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            _state.value = UpdateProgressState.Failed("Downloaded APK is missing. Tap Retry.")
            return
        }

        _state.value = UpdateProgressState.Installing
        markPendingRelaunch(context, true)

        val sessionStarted = withContext(Dispatchers.IO) {
            runCatching {
                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL,
                ).apply {
                    setAppPackageName(context.packageName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }
                val sessionId = packageInstaller.createSession(params)
                packageInstaller.openSession(sessionId).use { session ->
                    session.openWrite("sijui_update.apk", 0, apkFile.length()).use { out ->
                        apkFile.inputStream().use { input ->
                            input.copyTo(out, bufferSize = 64 * 1024)
                        }
                        session.fsync(out)
                    }

                    val intent = Intent(context, UpdateInstallReceiver::class.java).apply {
                        action = UpdateInstallReceiver.ACTION_INSTALL_RESULT
                    }
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        intent,
                        flags,
                    )
                    session.commit(pendingIntent.intentSender)
                }
                true
            }.getOrElse { false }
        }

        if (!sessionStarted) {
            // Fallback to FileProvider ACTION_VIEW installer if PackageInstaller fails on a specific ROM
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile,
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(installIntent)
            }.onFailure { err ->
                markPendingRelaunch(context, false)
                _state.value = UpdateProgressState.Failed(err.message ?: "Could not start installer.")
            }
        }
    }

    internal fun onInstallStatus(status: Int, message: String?) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                _state.value = UpdateProgressState.Idle
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                _state.value = UpdateProgressState.Idle
            }
            else -> {
                _state.value = UpdateProgressState.Failed(
                    message?.takeIf { it.isNotBlank() } ?: "Installation did not complete. Tap Retry.",
                )
            }
        }
    }

    internal fun markPendingRelaunch(context: Context, pending: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PENDING_RELAUNCH, pending)
            .apply()
    }

    internal fun consumePendingRelaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(KEY_PENDING_RELAUNCH, false)
        if (pending) {
            prefs.edit().putBoolean(KEY_PENDING_RELAUNCH, false).apply()
        }
        return pending
    }

    internal fun launchMainActivity(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        runCatching { context.startActivity(launchIntent) }
    }
}

class UpdateInstallReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_INSTALL_RESULT = "com.sijui.charades.action.INSTALL_RESULT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(confirmIntent) }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                InAppUpdateManager.onInstallStatus(status, null)
                InAppUpdateManager.cleanupOldApks(context)
                InAppUpdateManager.launchMainActivity(context)
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                InAppUpdateManager.markPendingRelaunch(context, false)
                InAppUpdateManager.onInstallStatus(status, msg)
            }
        }
    }
}

class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        InAppUpdateManager.cleanupOldApks(context)
        if (InAppUpdateManager.consumePendingRelaunch(context)) {
            InAppUpdateManager.launchMainActivity(context)
        }
    }
}
