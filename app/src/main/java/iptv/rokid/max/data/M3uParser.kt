package iptv.rokid.max.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object M3uParser {
    private val client = OkHttpClient()

    suspend fun fetchAndParse(context: Context, url: String = "https://xem.hoiquan.click"): List<Category> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "m3u_cache.txt")
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val bodyText = response.body?.string() ?: return@withContext emptyList()
            cacheFile.writeText(bodyText)
            addRecentCategory(context, parse(bodyText))
        } catch (e: Exception) {
            e.printStackTrace()
            if (cacheFile.exists()) {
                try {
                    val cachedText = cacheFile.readText()
                    return@withContext addRecentCategory(context, parse(cachedText))
                } catch (cacheEx: Exception) {
                    cacheEx.printStackTrace()
                }
            }
            emptyList()
        }
    }

    private fun addRecentCategory(context: Context, categories: List<Category>): List<Category> {
        val recentChannel = RecentChannelManager.getRecentChannel(context) ?: return categories
        val recentCategory = Category("Gần đây", listOf(recentChannel))
        
        // Remove the recent channel from other categories to avoid duplicates (optional, but good UX)
        // For now, simply prepending it.
        return listOf(recentCategory) + categories
    }

    private fun parse(m3uContent: String): List<Category> {
        val channels = mutableListOf<Channel>()
        val lines = m3uContent.lines()
        
        var currentName = ""
        var currentLogo = ""
        var currentGroup = "Khác"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                // Parse attributes
                val logoMatch = Regex("""tvg-logo="([^"]*)"""").find(trimmed)
                currentLogo = logoMatch?.groups?.get(1)?.value ?: ""
                
                val groupMatch = Regex("""group-title="([^"]*)"""").find(trimmed)
                currentGroup = groupMatch?.groups?.get(1)?.value ?: "Khác"
                
                // Parse name (usually after the last comma)
                val commaIndex = trimmed.lastIndexOf(',')
                currentName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                    trimmed.substring(commaIndex + 1).trim()
                } else {
                    "Kênh chưa biết"
                }
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                // This is the URL line
                if (currentName.isNotEmpty()) {
                    channels.add(Channel(currentName, currentLogo, currentGroup, trimmed))
                    currentName = ""
                    currentLogo = ""
                    currentGroup = "Khác"
                }
            }
        }

        // Group channels by category and preserve order
        val categoriesMap = LinkedHashMap<String, MutableList<Channel>>()
        for (channel in channels) {
            val list = categoriesMap.getOrPut(channel.group) { mutableListOf() }
            list.add(channel)
        }

        return categoriesMap.map { Category(it.key, it.value) }
    }
}
