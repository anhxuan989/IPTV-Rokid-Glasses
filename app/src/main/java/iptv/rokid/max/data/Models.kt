package iptv.rokid.max.data

data class Channel(
    val name: String,
    val logo: String,
    val group: String,
    val url: String
)

data class Category(
    val name: String,
    val channels: List<Channel>
)
