package com.babylon.wallet.android.utils

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

fun logNonFatalException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
}
