package com.babylon.wallet.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.utils.AppsFlyerIntegrationManager
import com.radixdlt.sargon.HomeCardsManager
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
    lateinit var homeCardsManager: HomeCardsManager

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
        bootstrapHomeCardsManager()
    }

    private fun bootstrapHomeCardsManager() {
        scope.launch {
            runCatching { homeCardsManager.bootstrap() }
                .onFailure { Timber.d("HomeCardsManager init error: ${it.message}") }
                .onSuccess { Timber.d("Successfully initialized HomeCardsManager") }
        }
    }
}
