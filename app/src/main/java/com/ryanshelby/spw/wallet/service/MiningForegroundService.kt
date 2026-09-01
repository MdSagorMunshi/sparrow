package com.ryanshelby.spw.wallet.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.ryanshelby.spw.wallet.MainActivity
import com.ryanshelby.spw.wallet.R
import com.ryanshelby.spw.wallet.SPWApplication
import com.ryanshelby.spw.wallet.ui.widget.SpwWalletAppWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MiningForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopMiningService()
            return START_NOT_STICKY
        }

        val payoutAddress = intent?.getStringExtra(EXTRA_PAYOUT_ADDRESS) ?: ""
        val cpuAllocation = intent?.getIntExtra(EXTRA_CPU_ALLOCATION, 50) ?: 50

        // Acquire partial wake lock so CPU continues hashing when screen is off
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SPW:MiningEngineWakeLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max safeguard
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Start Foreground Notification
        val initialNotification = buildNotification("Initializing RandomX node miner...", "Connecting to node")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        // Start real mining engine
        val miningManager = SPWApplication.instance.miningManager
        miningManager.startMiningInternal(payoutAddress, cpuAllocation)

        // Observe mining telemetry and update notification & home screen widget
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            miningManager.state.collectLatest { state ->
                if (!state.isActive) {
                    stopMiningService()
                } else {
                    val contentText = String.format(
                        Locale.US,
                        "%.1f H/s • Block #%d • Session: %.4f SPW",
                        state.hashRate,
                        state.currentBlockHeight,
                        state.sessionMinedSpw
                    )
                    val updatedNotification = buildNotification(contentText, "RandomX Mining Active (${state.cpuAllocation}% CPU)")
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                    SpwWalletAppWidget.updateAllWidgets(applicationContext)
                }
            }
        }

        return START_STICKY
    }

    private fun buildNotification(contentText: String, title: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("initial_route", "mining")
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MiningForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            202,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mining)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Mining", stopPendingIntent)
            .build()
    }

    private fun stopMiningService() {
        observerJob?.cancel()
        SPWApplication.instance.miningManager.stopMiningInternal()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        SpwWalletAppWidget.updateAllWidgets(applicationContext)
    }

    override fun onDestroy() {
        stopMiningService()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SPW Mining Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live telemetry while node mining in background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "spw_mining_channel"
        const val NOTIFICATION_ID = 8888
        const val ACTION_START = "com.ryanshelby.spw.wallet.START_MINING"
        const val ACTION_STOP = "com.ryanshelby.spw.wallet.STOP_MINING"
        const val EXTRA_PAYOUT_ADDRESS = "extra_payout_address"
        const val EXTRA_CPU_ALLOCATION = "extra_cpu_allocation"

        fun startService(context: Context, payoutAddress: String, cpuAllocation: Int) {
            val intent = Intent(context, MiningForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PAYOUT_ADDRESS, payoutAddress)
                putExtra(EXTRA_CPU_ALLOCATION, cpuAllocation)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MiningForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
