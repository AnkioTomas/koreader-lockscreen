package net.ankio.koreader

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** 设置页：图片路径、解锁后打开的应用、启停服务。 */
class MainActivity : Activity() {

    private lateinit var imageInput: EditText
    private lateinit var packageInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun label(t: String) = TextView(this).apply { text = t; setPadding(0, pad, 0, pad / 4) }
        fun input(v: String) = EditText(this).apply {
            setText(v); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true
        }
        fun button(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t; setOnClickListener { onClick() }
        }

        imageInput = input(Prefs.imagePath(this))
        packageInput = input(Prefs.launchPackage(this))
        status = TextView(this).apply { setPadding(0, pad, 0, 0) }

        root.addView(label("锁屏图片路径"))
        root.addView(imageInput)
        root.addView(label("解锁后打开的应用包名（留空则不打开）"))
        root.addView(packageInput)
        root.addView(button("保存并启动服务") { save(true) })
        root.addView(button("停止服务") { save(false) })
        root.addView(button("预览锁屏图片") {
            save(Prefs.enabled(this))
            startActivity(Intent(this, LockScreenActivity::class.java))
        })
        root.addView(status)
        setContentView(root, ViewGroup.LayoutParams(-1, -1))

        requestStorage()
        if (Prefs.enabled(this)) LockService.start(this)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun save(enable: Boolean) {
        Prefs.save(this, imageInput.text.toString().trim(), packageInput.text.toString().trim(), enable)
        if (enable) LockService.start(this) else LockService.stop(this)
        Toast.makeText(this, if (enable) "服务已启动" else "服务已停止", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun refreshStatus() {
        status.text = buildString {
            append("服务：").append(if (Prefs.enabled(this@MainActivity)) "开启" else "关闭").append('\n')
            append("存储权限：").append(if (hasStorage()) "已授予" else "未授予")
        }
    }

    private fun hasStorage(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestStorage() {
        if (hasStorage()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")))
            }.onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }
}
