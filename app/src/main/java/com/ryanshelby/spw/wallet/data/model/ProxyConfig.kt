package com.ryanshelby.spw.wallet.data.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.InetSocketAddress
import java.net.Proxy

enum class ProxyType(val displayName: String) {
    NONE("Direct Connection (No Proxy)"),
    TOR_ORBOT("Tor / Orbot (SOCKS 127.0.0.1:9050)"),
    SOCKS5("Custom SOCKS5 Proxy"),
    HTTP("Custom HTTP Proxy")
}

data class ProxyConfig(
    val enabled: Boolean = false,
    val type: ProxyType = ProxyType.NONE,
    val host: String = "127.0.0.1",
    val port: Int = 9050,
    val username: String = "",
    val password: String = ""
) {
    fun toJavaProxy(): Proxy {
        if (!enabled || type == ProxyType.NONE) {
            return Proxy.NO_PROXY
        }
        val proxyType = when (type) {
            ProxyType.TOR_ORBOT, ProxyType.SOCKS5 -> Proxy.Type.SOCKS
            ProxyType.HTTP -> Proxy.Type.HTTP
            ProxyType.NONE -> Proxy.Type.DIRECT
        }
        val targetHost = if (type == ProxyType.TOR_ORBOT) "127.0.0.1" else host
        val targetPort = if (type == ProxyType.TOR_ORBOT) 9050 else port
        return try {
            Proxy(proxyType, InetSocketAddress(targetHost, targetPort))
        } catch (e: Exception) {
            Proxy.NO_PROXY
        }
    }
}

private val Context.proxyDataStore by preferencesDataStore(name = "spw_proxy_settings")

class ProxyPreferences(private val context: Context) {
    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("proxy_enabled")
        private val KEY_TYPE = stringPreferencesKey("proxy_type")
        private val KEY_HOST = stringPreferencesKey("proxy_host")
        private val KEY_PORT = intPreferencesKey("proxy_port")
        private val KEY_USER = stringPreferencesKey("proxy_user")
        private val KEY_PASS = stringPreferencesKey("proxy_pass")
    }

    val proxyConfigFlow: Flow<ProxyConfig> = context.proxyDataStore.data.map { prefs ->
        val typeStr = prefs[KEY_TYPE] ?: ProxyType.NONE.name
        val type = try {
            ProxyType.valueOf(typeStr)
        } catch (e: Exception) {
            ProxyType.NONE
        }
        ProxyConfig(
            enabled = prefs[KEY_ENABLED] ?: false,
            type = type,
            host = prefs[KEY_HOST] ?: "127.0.0.1",
            port = prefs[KEY_PORT] ?: 9050,
            username = prefs[KEY_USER] ?: "",
            password = prefs[KEY_PASS] ?: ""
        )
    }

    suspend fun saveProxyConfig(config: ProxyConfig) {
        context.proxyDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = config.enabled
            prefs[KEY_TYPE] = config.type.name
            prefs[KEY_HOST] = config.host
            prefs[KEY_PORT] = config.port
            prefs[KEY_USER] = config.username
            prefs[KEY_PASS] = config.password
        }
    }
}
