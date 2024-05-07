package com.babylon.wallet.android.utils

object Constants {
    const val SNACKBAR_SHOW_DURATION_MS = 2000L
    const val VM_STOP_TIMEOUT_MS = 5000L
    const val DELAY_300_MS = 300L
    const val ACCOUNT_NAME_MAX_LENGTH = 30
    const val RADIX_START_PAGE_URL = "https://wallet.radixdlt.com/?wallet=downloaded"
    const val DEFAULT_ACCOUNT_NAME = "Unnamed"
    const val MAX_ITEMS_PER_ENTITY_DETAILS_REQUEST = 20
    const val DEFAULT_RADIX_CONNECT_LINKING_DELAY_SECONDS = 1

    object RadixMobileConnect {
        const val CONNECT_URL_PARAM_PUBLIC_KEY = "publicKey"
        const val CONNECT_URL_PARAM_SECRET = "secret"
        const val CONNECT_URL_PARAM_SESSION_ID = "sessionId"
        const val CONNECT_URL_PARAM_ORIGIN = "origin"
        const val CONNECT_URL_PARAM_INTERACTION_ID = "interactionId"
    }
}
