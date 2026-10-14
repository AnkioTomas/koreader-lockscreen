package net.ankio.koreader

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import java.io.File
import java.lang.ref.WeakReference

/** 盖在锁屏上方的全屏图片；点击即解锁，解锁后关闭。 */
class LockScreenActivity : Activity() {

    companion object {
        private const val TAG = "LockScreenActivity"
        private var current: WeakReference<LockScreenActivity>? = null

        fun finishIfShowing() {
            current?.get()?.finish()
        }
    }

    private lateinit var imageView: ImageView
    private var bitmap: Bitmap? = null
    private var loadedKey: String? = null
    private var unlocking = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        current = WeakReference(this)
        KeepAlive.ensure(this)
        imageView = ImageView(this).apply {
            setBackgroundColor(Color.BLACK)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setOnClickListener { unlock() }
        }
        setContentView(imageView)
        hideSystemBars()
    }

    override fun onStart() {
        super.onStart()
        loadImage() // 仅在图片变化时重新解码，避免亮屏时重绘造成闪烁
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "MissingSuperCall")
    override fun onBackPressed() {
        // 屏蔽返回键：只能点击图片解锁
    }

    /**
     * 上滑（Home 手势）/ 最近任务会把锁屏页切走，露出系统锁屏。
     * 只要还在锁定、亮屏且不是我们主动解锁，就立刻把自己拉回前台。
     */
    override fun onStop() {
        super.onStop()
        if (isFinishing || unlocking) return
        val km = getSystemService(KeyguardManager::class.java) ?: return
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (!km.isKeyguardLocked || !pm.isInteractive) return
        handler.postDelayed({ bringToFront() }, 150)
    }

    private fun bringToFront() {
        if (isFinishing || isDestroyed || unlocking) return
        val km = getSystemService(KeyguardManager::class.java)
        if (km?.isKeyguardLocked != true) return
        try {
            startActivity(Intent(this, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        } catch (e: Throwable) {
            Log.e(TAG, "bringToFront failed", e)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (current?.get() === this) current = null
        imageView.setImageDrawable(null)
        bitmap?.recycle()
        bitmap = null
        super.onDestroy()
    }

    /** 点击图片：直接弹出系统解锁（无密码时立即解锁），解锁后由服务打开 KOReader。 */
    private fun unlock() {
        val km = getSystemService(KeyguardManager::class.java)
        if (km == null || !km.isKeyguardLocked) { finish(); return }
        unlocking = true
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() { finish() }
            override fun onDismissCancelled() { unlocking = false }
            override fun onDismissError() { unlocking = false }
        })
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    private fun loadImage() {
        val path = Prefs.imagePath(this)
        val f = File(path)
        val key = "$path|${f.lastModified()}|${f.length()}"
        if (key == loadedKey && bitmap != null) return
        val bmp = try { decode(path) } catch (e: Throwable) {
            Log.e(TAG, "decode failed: $path", e); null
        } ?: return
        val old = bitmap
        imageView.setImageBitmap(bmp)
        bitmap = bmp
        loadedKey = key
        old?.recycle()
    }

    private fun decode(path: String): Bitmap? {
        if (!File(path).canRead()) {
            Log.e(TAG, "cannot read: $path"); return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val dm = resources.displayMetrics
        val req = maxOf(dm.widthPixels, dm.heightPixels)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= req) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
