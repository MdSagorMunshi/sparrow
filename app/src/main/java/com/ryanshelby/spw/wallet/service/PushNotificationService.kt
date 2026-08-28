package com.ryanshelby.spw.wallet.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ryanshelby.spw.wallet.MainActivity

class PushNotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID_TRANSFERS = "spw_wallet_transfers"
        const val CHANNEL_NAME_TRANSFERS = "SPW Network Transfers"
        const val CHANNEL_ID_SECURITY = "spw_wallet_security"
        const val CHANNEL_NAME_SECURITY = "SPW Security & Cold Vault"
        private var notificationIdCounter = 1001
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val transferChannel = NotificationChannel(
                CHANNEL_ID_TRANSFERS,
                CHANNEL_NAME_TRANSFERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live notifications for incoming and outgoing SPW Network cryptocurrency transfers"
                enableVibration(true)
            }

            val securityChannel = NotificationChannel(
                CHANNEL_ID_SECURITY,
                CHANNEL_NAME_SECURITY,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Security alerts, cold vault signature requests, and PIN notifications"
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(transferChannel)
            manager.createNotificationChannel(securityChannel)
        }
    }

    fun showIncomingTransferNotification(
        amount: Double,
        symbol: String,
        fromAddress: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shortAddr = if (fromAddress.length > 12) {
            "${fromAddress.take(6)}...${fromAddress.takeLast(4)}"
        } else fromAddress

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_TRANSFERS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("⚡ SPW Received")
            .setContentText("+$amount $symbol from $shortAddr")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You received +$amount $symbol on SPW Network from $fromAddress. Balance updated.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationIdCounter++, builder.build())
        } catch (_: SecurityException) {
            // Permission not yet granted on Android 13+
        }
    }

    fun showOutgoingTransferNotification(
        amount: Double,
        symbol: String,
        toAddress: String,
        txHash: String
    ) {
        val shortAddr = if (toAddress.length > 12) {
            "${toAddress.take(6)}...${toAddress.takeLast(4)}"
        } else toAddress

        val shortHash = if (txHash.length > 12) {
            "${txHash.take(6)}...${txHash.takeLast(4)}"
        } else txHash

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_TRANSFERS)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("🚀 Transfer Broadcasted")
            .setContentText("Sent $amount $symbol to $shortAddr (TX: $shortHash)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Successfully sent $amount $symbol to $toAddress.\nTXID: $txHash")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationIdCounter++, builder.build())
        } catch (_: SecurityException) {
            // Permission not yet granted on Android 13+
        }
    }

}
