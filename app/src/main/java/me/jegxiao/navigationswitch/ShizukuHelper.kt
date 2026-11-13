package me.jegxiao.navigationswitch

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuAccess
import rikka.shizuku.ShizukuRemoteProcess
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the Shizuku API.
 *
 * Shizuku gives this app a shell identity (uid 2000), which is what lets us run
 * `cmd overlay ...` on Honor / Huawei devices without root.
 */
object ShizukuHelper {

    const val PACKAGE_NAME = "moe.shizuku.privileged.api"
    const val PERMISSION_REQUEST_CODE = 2001

    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun isPermissionGranted(): Boolean {
        if (!isBinderAlive()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission() {
        if (!isBinderAlive()) return
        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        } catch (_: Throwable) {
            // Shizuku may disappear between the check and the request; the UI refreshes anyway.
        }
    }

    fun runShell(command: String, timeoutMs: Long = 8_000L): ShellResult {
        if (!isPermissionGranted()) return ShellResult.NoPermission

        var process: Process? = null
        return try {
            val p = ShizukuAccess.newProcess(arrayOf("sh", "-c", command), null, null)
            process = p

            // ShizukuRemoteProcess.exitValue() throws a RuntimeException (not
            // IllegalThreadStateException) while the process is still alive, so use its
            // dedicated timeout-aware wait instead of polling exitValue().
            val exited = if (p is ShizukuRemoteProcess) {
                p.waitForTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                waitForLegacy(p, timeoutMs)
            }

            if (!exited) {
                try {
                    p.destroy()
                } catch (_: Exception) {
                }
                ShellResult.Error("Shizuku 命令执行超时")
            } else {
                val exitCode = try {
                    p.exitValue()
                } catch (_: Exception) {
                    -1
                }
                val output = try {
                    p.inputStream.bufferedReader().use { it.readText() }
                } catch (_: Exception) {
                    ""
                }
                ShellResult.Finished(exitCode, output)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            ShellResult.Error("Shizuku 命令执行被中断")
        } catch (e: Throwable) {
            ShellResult.Error(e.message ?: "Shizuku 调用失败")
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    private fun waitForLegacy(process: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(50)
            }
        }
        return false
    }
}

sealed class ShellResult {
    data class Finished(val exitCode: Int, val output: String) : ShellResult()
    object NoPermission : ShellResult()
    data class Error(val message: String) : ShellResult()
}