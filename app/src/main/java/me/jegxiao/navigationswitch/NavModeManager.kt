package me.jegxiao.navigationswitch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Locale

enum class NavMode { THREE_BUTTON, TWO_BUTTON, GESTURE }

/**
 * Reads and switches the system navigation mode.
 *
 * AOSP stores it in [Settings.Secure] "navigation_mode":
 *  0 = 3-button, 1 = 2-button, 2 = gesture.
 *
 * Honor / Huawei use their own values (101 = 3-button, 105 = gesture) and, more importantly,
 * their SystemUI does not react to the setting at runtime. Their Settings app toggles the
 * navigation bar overlay through OverlayManager, which needs a signature permission. We use
 * Shizuku to run the same commands with the shell identity.
 */
object NavModeManager {

    private const val KEY_NAVIGATION_MODE = "navigation_mode"
    private const val KEY_HONOR_GESTURE = "secure_gesture_navigation"

    private const val AOSP_THREE_BUTTON = 0
    private const val AOSP_TWO_BUTTON = 1
    private const val AOSP_GESTURE = 2

    private const val HONOR_THREE_BUTTON = 101
    private const val HONOR_GESTURE = 105

    private const val OVERLAY_GESTURAL = "com.android.internal.systemui.navbar.gestural"
    private const val OVERLAY_THREE_BUTTON = "com.android.internal.systemui.navbar.threebutton"

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isHonorFamily(): Boolean {
        val manufacturer = Build.MANUFACTURER.uppercase(Locale.ROOT)
        val brand = Build.BRAND.uppercase(Locale.ROOT)
        return manufacturer.contains("HONOR") || brand.contains("HONOR") ||
            manufacturer.contains("HUAWEI") || brand.contains("HUAWEI")
    }

    fun settingUri(): Uri = Settings.Secure.getUriFor(KEY_NAVIGATION_MODE)

    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** True when this app can actually change the navigation mode on this device. */
    fun canToggle(context: Context): Boolean = when {
        !isSupported() -> false
        isHonorFamily() -> ShizukuHelper.isPermissionGranted()
        else -> canWriteSecureSettings(context)
    }

    fun currentMode(context: Context): NavMode {
        if (!isSupported()) return NavMode.THREE_BUTTON

        if (isHonorFamily()) {
            // secure_gesture_navigation is the value Honor's settings UI keeps in sync.
            val gestureFlag = Settings.Secure.getInt(
                context.contentResolver, KEY_HONOR_GESTURE, -1
            )
            if (gestureFlag == 1) return NavMode.GESTURE
            if (gestureFlag == 0) return NavMode.THREE_BUTTON

            val raw = Settings.Secure.getInt(
                context.contentResolver, KEY_NAVIGATION_MODE, HONOR_THREE_BUTTON
            )
            return if (raw == HONOR_GESTURE) NavMode.GESTURE else NavMode.THREE_BUTTON
        }

        return when (Settings.Secure.getInt(
            context.contentResolver, KEY_NAVIGATION_MODE, AOSP_THREE_BUTTON
        )) {
            AOSP_GESTURE -> NavMode.GESTURE
            AOSP_TWO_BUTTON -> NavMode.TWO_BUTTON
            else -> NavMode.THREE_BUTTON
        }
    }

    fun isGesture(context: Context): Boolean = currentMode(context) == NavMode.GESTURE

    fun targetMode(context: Context): NavMode =
        if (isGesture(context)) NavMode.THREE_BUTTON else NavMode.GESTURE

    fun switchMode(context: Context): SwitchResult {
        if (!isSupported()) return SwitchResult.Unsupported

        val target = targetMode(context)
        if (isHonorFamily()) {
            if (!ShizukuHelper.isPermissionGranted()) return SwitchResult.NeedsShizuku
            return switchHonor(context, target)
        }

        if (!canWriteSecureSettings(context)) return SwitchResult.NoPermission
        return writeAosp(context, target)
    }

    private fun writeAosp(context: Context, target: NavMode): SwitchResult {
        val value = when (target) {
            NavMode.GESTURE -> AOSP_GESTURE
            NavMode.TWO_BUTTON -> AOSP_TWO_BUTTON
            NavMode.THREE_BUTTON -> AOSP_THREE_BUTTON
        }
        return try {
            val written = Settings.Secure.putInt(context.contentResolver, KEY_NAVIGATION_MODE, value)
            when {
                !written -> SwitchResult.Failed(context.getString(R.string.error_write_rejected))
                currentMode(context) != target ->
                    SwitchResult.Failed(context.getString(R.string.error_not_applied))
                else -> SwitchResult.Success(target)
            }
        } catch (_: SecurityException) {
            SwitchResult.NoPermission
        } catch (e: Exception) {
            SwitchResult.Failed(e.message ?: context.getString(R.string.error_unknown))
        }
    }

    /**
     * Honor / Huawei: write both settings for consistency, then toggle the navigation bar
     * overlay with the shell identity provided by Shizuku (or root).
     */
    private fun switchHonor(context: Context, target: NavMode): SwitchResult {
        val gesture = target == NavMode.GESTURE
        val command = buildString {
            append("settings put secure ").append(KEY_NAVIGATION_MODE).append(' ')
                .append(if (gesture) HONOR_GESTURE else HONOR_THREE_BUTTON).append("; ")
            append("settings put secure ").append(KEY_HONOR_GESTURE).append(' ')
                .append(if (gesture) 1 else 0).append("; ")
            if (gesture) {
                append("cmd overlay enable-exclusive --category ")
                    .append(OVERLAY_GESTURAL).append("; ")
            } else {
                append("cmd overlay disable ").append(OVERLAY_GESTURAL).append("; ")
                append("cmd overlay disable ").append(OVERLAY_THREE_BUTTON).append("; ")
            }
            append("sleep 0.2; cmd overlay list")
        }

        return when (val result = ShizukuHelper.runShell(command)) {
            is ShellResult.Finished -> {
                val line = result.output.lineSequence()
                    .firstOrNull { it.contains("navbar.gestural") }
                    ?.trimStart()
                val overlayGesture = line?.startsWith("[x]", ignoreCase = true) == true
                if (overlayGesture == gesture && currentMode(context) == target) {
                    SwitchResult.Success(target)
                } else {
                    SwitchResult.Failed(context.getString(R.string.error_not_applied))
                }
            }

            ShellResult.NoPermission -> SwitchResult.NeedsShizuku
            is ShellResult.Error -> SwitchResult.Failed(result.message)
        }
    }

    fun modeNameRes(mode: NavMode): Int = when (mode) {
        NavMode.GESTURE -> R.string.mode_gesture
        NavMode.TWO_BUTTON -> R.string.mode_two
        NavMode.THREE_BUTTON -> R.string.mode_three
    }

    fun modeShortNameRes(mode: NavMode): Int = when (mode) {
        NavMode.GESTURE -> R.string.mode_gesture_short
        NavMode.TWO_BUTTON -> R.string.mode_two_short
        NavMode.THREE_BUTTON -> R.string.mode_three_short
    }
}

sealed class SwitchResult {
    data class Success(val mode: NavMode) : SwitchResult()
    object NoPermission : SwitchResult()
    object NeedsShizuku : SwitchResult()
    object Unsupported : SwitchResult()
    data class Failed(val reason: String) : SwitchResult()
}