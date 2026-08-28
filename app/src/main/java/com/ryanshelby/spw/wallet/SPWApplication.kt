package com.ryanshelby.spw.wallet

import android.app.Application
import com.ryanshelby.spw.wallet.data.local.AppDatabase
import com.ryanshelby.spw.wallet.data.remote.RpcClient
import com.ryanshelby.spw.wallet.data.repository.WalletRepository
import com.ryanshelby.spw.wallet.security.SecurityManager
import com.ryanshelby.spw.wallet.service.PushNotificationService

class SPWApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var securityManager: SecurityManager
        private set

    lateinit var notificationService: PushNotificationService
        private set

    lateinit var rpcClient: RpcClient
        private set

    lateinit var walletRepository: WalletRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        securityManager = SecurityManager(this)
        notificationService = PushNotificationService(this)
        rpcClient = RpcClient()
        walletRepository = WalletRepository(
            context = this,
            database = database,
            securityManager = securityManager,
            notificationService = notificationService,
            rpcClient = rpcClient
        )
    }

    companion object {
        lateinit var instance: SPWApplication
            private set
    }
}
