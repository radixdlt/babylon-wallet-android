package rdx.works.core

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

fun logNonFatalException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
}

fun deleteCrashlyticsUnsentReports() {
    Firebase.crashlytics.deleteUnsentReports()
}

fun enableCrashlytics(enabled: Boolean) {
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
}
