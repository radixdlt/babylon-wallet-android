package com.babylon.wallet.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.utils.AccountLockersObserver
import com.babylon.wallet.android.utils.AppsFlyerIntegrationManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class RadixApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    @Inject
    lateinit var appsFlyerIntegrationManager: AppsFlyerIntegrationManager

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var appLockStateProvider: AppLockStateProvider

    @Inject
    lateinit var homeCardsRepository: HomeCardsRepository

    @Inject
    lateinit var accountLockersObserver: AccountLockersObserver

    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setWorkerFactory(EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        }

        appsFlyerIntegrationManager.init()
        bootstrapHomeCards()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    inner class AppLifecycleObserver : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            accountLockersObserver.startMonitoring()
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            scope.launch { appLockStateProvider.lockApp() }
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            accountLockersObserver.stopMonitoring()
        }
    }

    private fun bootstrapHomeCards() {
        scope.launch { homeCardsRepository.bootstrap() }
    }
}
