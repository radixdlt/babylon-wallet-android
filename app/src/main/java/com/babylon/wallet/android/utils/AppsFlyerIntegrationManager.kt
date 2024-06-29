package com.babylon.wallet.android.utils

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkResult
import com.babylon.wallet.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsFlyerIntegrationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun init() {
        AppsFlyerLib.getInstance()
            .init(BuildConfig.APPS_FLYER_DEV_KEY, null, context)
            .apply { setDebugLog(BuildConfig.DEBUG_MODE) }

        subscribeForDeepLink()

        AppsFlyerLib.getInstance().start(context)
    }

    /**
     * Subscribes for a deep link
     * Tracks an event with the link details if it was deferred - i.e. clicked before the app was installed
     * This has to be called before calling start on [AppsFlyerLib.getInstance]
     */
    private fun subscribeForDeepLink() {
        AppsFlyerLib.getInstance().subscribeForDeepLink { result ->
            when (result.status) {
                DeepLinkResult.Status.FOUND -> {
                    val deepLink = result.deepLink
                    Timber.d("Resolved deep link. Is deferred: ${deepLink.isDeferred}. Click event: ${deepLink.clickEvent}")

                    if (deepLink.isDeferred == true) {
                        val message = if (deepLink.deepLinkValue != null) {
                            "Resolved deferred DL with value: ${deepLink.deepLinkValue}"
                        } else {
                            "Resolved deferred DL without value"
                        }
                        val clickEventMap = runCatching {
                            deepLink.clickEvent.keys()
                                .asSequence()
                                .map { it to deepLink.clickEvent.get(it) }
                                .toMap()
                        }.getOrNull() ?: emptyMap()

                        AppsFlyerLib.getInstance().logEvent(context, message, clickEventMap)
                    }
                }
                DeepLinkResult.Status.ERROR -> {
                    Timber.d("Failed to resolve deep link. Error: ${result.error}")
                }
                DeepLinkResult.Status.NOT_FOUND -> {
                    Timber.d("AF deep link not found")
                }
            }
        }
    }
}
