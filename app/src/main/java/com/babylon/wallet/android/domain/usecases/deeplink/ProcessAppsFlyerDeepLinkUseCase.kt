package com.babylon.wallet.android.domain.usecases.deeplink

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkResult
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class ProcessAppsFlyerDeepLinkUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke() {
        AppsFlyerLib.getInstance().subscribeForDeepLink { result ->
            when (result.status) {
                DeepLinkResult.Status.FOUND -> {
                    val deepLink = result.deepLink
                    Timber.d("did resolve deep link. Is deferred: ${deepLink.isDeferred}. Click events: ${deepLink.clickEvent}")

                    if (deepLink.isDeferred == true) {
                        val message = if (deepLink.deepLinkValue != null) {
                            "Resolved deferred DL with value: ${deepLink.deepLinkValue}"
                        } else {
                            "Resolved deferred DL without value"
                        }
                        val clickEventMap = deepLink.clickEvent.keys().asSequence().map {
                            it to deepLink.clickEvent.get(it)
                        }.toMap()
                        AppsFlyerLib.getInstance().logEvent(context, message, clickEventMap)
                    }
                }
                DeepLinkResult.Status.ERROR -> {
                    Timber.d("Failed to resolve deep link. Error: ${result.error}")
                }
                DeepLinkResult.Status.NOT_FOUND -> {}
            }
        }
    }
}
