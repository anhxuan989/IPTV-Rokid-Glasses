package iptv.rokid.max.data

import android.content.Context
import android.content.SharedPreferences

object RecentChannelManager {
    private const val PREFS_NAME = "RecentChannelPrefs"
    private const val KEY_NAME = "recent_name"
    private const val KEY_LOGO = "recent_logo"
    private const val KEY_GROUP = "recent_group"
    private const val KEY_URL = "recent_url"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveRecentChannel(context: Context, channel: Channel) {
        getPrefs(context).edit().apply {
            putString(KEY_NAME, channel.name)
            putString(KEY_LOGO, channel.logo)
            putString(KEY_GROUP, channel.group)
            putString(KEY_URL, channel.url)
            apply()
        }
    }

    fun getRecentChannel(context: Context): Channel? {
        val prefs = getPrefs(context)
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val url = prefs.getString(KEY_URL, null) ?: return null
        
        return Channel(
            name = name,
            logo = prefs.getString(KEY_LOGO, "") ?: "",
            group = prefs.getString(KEY_GROUP, "Gần đây") ?: "Gần đây",
            url = url
        )
    }
}
