package top.sakari.wallsetter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * 常驻前台服务：
 *  - 熄屏 -> 拉起 LockScreenActivity 盖在锁屏上
 *  - 解锁 -> 打开 KOReader
 */
class LockService : Service() {

    companion object {
        private const val TAG = "LockService"
        private const val CHANNEL_ID = "lock_service"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val i = Intent(context, LockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockService::class.java))
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> showLockScreen()
                Intent.ACTION_USER_PRESENT -> openReader()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showLockScreen() {
        try {
            startActivity(Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            })
        } catch (e: Exception) {
            Log.e(TAG, "show lock screen failed", e)
        }
    }

    private fun openReader() {
        LockScreenActivity.finishIfShowing()
        val pkg = Prefs.launchPackage(this)
        if (pkg.isBlank()) return
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch == null) {
            Log.w(TAG, "app not installed: $pkg"); return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { startActivity(launch) }.onFailure { Log.e(TAG, "open $pkg failed", it) }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "锁屏服务", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID) else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("锁屏显示运行中")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
