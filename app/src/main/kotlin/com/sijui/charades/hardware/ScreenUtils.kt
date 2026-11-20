package com.sijui.charades.hardware

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.ByteArrayOutputStream
import kotlin.math.max

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Locks orientation (optional landscape), hides system bars, and keeps the screen awake
 * while the phone is held on a player's forehead.
 */
@Composable
fun ForeheadModeEffect(
    lockLandscape: Boolean = true,
    keepScreenOn: Boolean = true,
    immersive: Boolean = true,
) {
    val context = LocalContext.current
    DisposableEffect(lockLandscape, keepScreenOn, immersive) {
        val activity = context.findActivity()
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation

        if (activity != null && lockLandscape) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        if (window != null && keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (window != null && immersive) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            if (activity != null && lockLandscape) {
                activity.requestedOrientation =
                    previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            if (window != null && keepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            if (window != null && immersive) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

object ImageCodec {
    /**
     * Reads a picked gallery image, scales it to max 800px, JPEG-compresses it,
     * and returns a data URI accepted by Sijui's /api/admin/nani/characters endpoint.
     */
    fun uriToDataUrl(context: Context, uri: Uri, maxDim: Int = 800): String? = runCatching {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
        val largest = max(original.width, original.height)
        val scaled = if (largest > maxDim) {
            val ratio = maxDim.toFloat() / largest.toFloat()
            Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt().coerceAtLeast(1),
                (original.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            original
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$b64"
    }.getOrNull()

    /**
     * Decodes a base64 data:image/... URI into a Bitmap for instant display.
     */
    fun decodeDataUrl(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank() || !dataUrl.startsWith("data:")) return null
        val commaIdx = dataUrl.indexOf(',')
        if (commaIdx < 0) return null
        return runCatching {
            val bytes = Base64.decode(dataUrl.substring(commaIdx + 1), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
