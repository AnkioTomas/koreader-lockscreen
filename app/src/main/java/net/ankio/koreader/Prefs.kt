package net.ankio.koreader

import android.content.Context

object Prefs {
    private const val NAME = "settings"
    const val DEFAULT_IMAGE = "/storage/emulated/0/koreader/.moon/screensaver/compose.png"
    const val DEFAULT_LAUNCH_PACKAGE = "org.koreader.launcher"

    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun imagePath(c: Context): String = sp(c).getString("image", DEFAULT_IMAGE) ?: DEFAULT_IMAGE
    fun launchPackage(c: Context): String = sp(c).getString("launch", DEFAULT_LAUNCH_PACKAGE) ?: DEFAULT_LAUNCH_PACKAGE
    fun enabled(c: Context): Boolean = sp(c).getBoolean("enabled", false)

    fun save(c: Context, image: String, launch: String, enabled: Boolean) {
        sp(c).edit().putString("image", image).putString("launch", launch)
            .putBoolean("enabled", enabled).apply()
    }
}
