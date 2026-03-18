package iptv.rokid.max

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import iptv.rokid.max.data.Channel
import iptv.rokid.max.ui.MainScreen
import iptv.rokid.max.ui.PlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (!isConnected) {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            var selectedChannel by remember { mutableStateOf<Channel?>(null) }

            if (selectedChannel == null) {
                MainScreen(onChannelSelected = { channel ->
                    selectedChannel = channel
                })
            } else {
                PlayerScreen(channel = selectedChannel!!) {
                    selectedChannel = null
                }
            }
        }
    }
}
