package com.ryanshelby.spw.wallet.data.local

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("spw_notification_prefs", Context.MODE_PRIVATE)

    var incomingTransactionsEnabled: Boolean
        get() = prefs.getBoolean("incoming_enabled", true)
        set(value) = prefs.edit().putBoolean("incoming_enabled", value).apply()

    var outgoingTransactionsEnabled: Boolean
        get() = prefs.getBoolean("outgoing_enabled", true)
        set(value) = prefs.edit().putBoolean("outgoing_enabled", value).apply()

    var miningRewardsEnabled: Boolean
        get() = prefs.getBoolean("mining_enabled", true)
        set(value) = prefs.edit().putBoolean("mining_enabled", value).apply()
}
