package com.ryanshelby.spw.wallet.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ryanshelby.spw.wallet.MainActivity
import com.ryanshelby.spw.wallet.R
import com.ryanshelby.spw.wallet.SPWApplication
import java.util.Locale

class SpwWalletAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SpwWalletAppWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.ryanshelby.spw.wallet.REFRESH_WIDGET"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, SpwWalletAppWidget::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_spw_wallet)

            try {
                val securityManager = SPWApplication.instance.securityManager
                val miningManager = SPWApplication.instance.miningManager
                val address = securityManager.getWalletAddress()
                val walletName = securityManager.getWalletName()
                val cached = securityManager.getCachedBalance(address)
                val balanceSpw = cached?.first ?: 0.0
                val isMining = miningManager.state.value.isActive

                views.setTextViewText(R.id.widget_wallet_name, walletName.ifEmpty { "Main Account" })
                views.setTextViewText(
                    R.id.widget_balance_text,
                    String.format(Locale.US, "%.4f SPW", balanceSpw)
                )

                if (isMining) {
                    views.setTextViewText(R.id.widget_status_badge, "MINING")
                } else {
                    views.setTextViewText(R.id.widget_status_badge, "LIVE")
                }

                // Send Button Action (Opens App on Send Screen)
                val sendIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("initial_route", "send")
                }
                val sendPendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    sendIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_btn_send, sendPendingIntent)

                // Mine Button Action (Opens App on Mining Screen)
                val mineIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("initial_route", "mining")
                }
                val minePendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    mineIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_btn_mine, minePendingIntent)

                // Root Click (Opens Dashboard)
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    100,
                    mainIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

            } catch (_: Exception) {
                views.setTextViewText(R.id.widget_wallet_name, "Sparrow Wallet")
                views.setTextViewText(R.id.widget_balance_text, "0.0000 SPW")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
