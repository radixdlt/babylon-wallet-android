package com.babylon.wallet.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.babylon.wallet.android.data.repository.homecards.HomeCardsRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
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
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(appLockStateProvider, scope))
    }

    class AppLifecycleObserver(
        private val appLockStateProvider: AppLockStateProvider,
        private val scope: CoroutineScope
    ) : DefaultLifecycleObserver {

        override fun onPause(owner: LifecycleOwner) {
            scope.launch {
                appLockStateProvider.lockApp()
            }
        }
    }

    private fun bootstrapHomeCards() {
        scope.launch { homeCardsRepository.bootstrap() }
    }
}
