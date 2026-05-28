package dev.ahmedmohamed.hayaitts.app.crash

import android.app.Application
import android.content.Intent
import dev.ahmedmohamed.hayaitts.BuildConfig
import dev.ahmedmohamed.hayaitts.ui.crash.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Catches every uncaught exception on every thread, formats it into a
 * report (stack trace, app version, device info, timestamp) and hands
 * control to [CrashActivity] so the user can copy/paste the report into a
 * GitHub issue.
 *
 * Without this hook a crash drops the user into the OS "app keeps stopping"
 * dialog with zero diagnostic value — the report is gone the moment they
 * dismiss it, and asking them to "send the logcat" is unreasonable.
 *
 * Install once from [Application.onCreate]. The previous default handler is
 * preserved and invoked after [CrashActivity] is launched so the OS still
 * records the crash for `adb bugreport` etc.
 */
object CrashHandler {

    fun install(application: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val report = buildReport(thread, throwable)
            try {
                val intent = Intent(application, CrashActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    )
                    putExtra(CrashActivity.EXTRA_REPORT, report)
                }
                application.startActivity(intent)
            } catch (_: Throwable) {
                // If startActivity itself fails we have no way back; the OS
                // crash dialog (via the previous handler below) is the only
                // remaining hook.
            }
            // Defer to the platform handler so the process actually dies —
            // the new CrashActivity launches in a separate task and survives
            // this process's death.
            if (previous != null) previous.uncaughtException(thread, throwable)
            exitProcess(2)
        }
    }

    private fun buildReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return buildString {
            append("HayaiTTS ").append(BuildConfig.VERSION_NAME)
                .append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
            append("Device: ").append(android.os.Build.MANUFACTURER)
                .append(' ').append(android.os.Build.MODEL).append('\n')
            append("Android: ").append(android.os.Build.VERSION.RELEASE)
                .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n")
            append("Thread: ").append(thread.name).append('\n')
            append("Time: ").append(java.util.Date()).append("\n\n")
            append(sw.toString())
        }
    }
}
