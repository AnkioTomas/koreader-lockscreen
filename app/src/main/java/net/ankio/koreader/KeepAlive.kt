package net.ankio.koreader

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

/**
 * 保活看门狗：用 AlarmManager 定时检查并拉起 LockService。
 * 服务被杀（划掉任务、系统回收、崩溃）后由闹钟重新启动。
 */
object KeepAlive {
    private const val TAG = "KeepAlive"
    private const val ACTION = "net.ankio.koreader.KEEP_ALIVE"
    private const val INTERVAL = 10 * 60 * 1000L

    private fun pi(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 100, Intent(c, KeepAliveReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** 安排下一次检查，delay 毫秒后触发（Doze 下也会触发）。 */
    fun schedule(c: Context, delay: Long = INTERVAL) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val at = SystemClock.elapsedRealtime() + delay
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi(c))
            } else {
                // Android 12+ 未授予精确闹钟权限：退化为非精确闹钟
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi(c))
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "exact alarm denied, fallback", e)
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi(c))
        }
    }

    fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java)?.cancel(pi(c))
    }

    fun ensure(c: Context) {
        if (!Prefs.enabled(c)) return
        runCatching { LockService.start(c) }.onFailure { Log.e(TAG, "start failed", it) }
        schedule(c)
    }
}

class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        KeepAlive.ensure(context)
    }
}
