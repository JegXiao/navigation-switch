package me.jegxiao.navigationswitch

import android.content.Context

/**
 * Optional last resort for users without a PC: let `su` grant WRITE_SECURE_SETTINGS once,
 * so every later switch behaves like the adb-authorized path.
 *
 * Must be called off the main thread.
 */
object RootShell {

    fun grantSecureSettings(context: Context): Boolean {
        // WRITE_SECURE_SETTINGS is a development permission, therefore `pm grant` works.
        val command = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
        return runSu(command) && NavModeManager.canWriteSecureSettings(context)
    }

    private fun runSu(command: String): Boolean {
        var process: Process? = null
        return try {
            val p = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            process = p
            val deadline = System.currentTimeMillis() + 8_000
            while (System.currentTimeMillis() < deadline) {
                try {
                    return p.exitValue() == 0
                } catch (_: IllegalThreadStateException) {
                    Thread.sleep(100)
                }
            }
            false
        } catch (_: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }
}