package rdx.works.core

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

fun logNonFatalException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
}

fun deleteCrashlyticsUnsentReports() {
    Firebase.crashlytics.deleteUnsentReports()
}

fun enableCrashlytics(enabled: Boolean) {
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
}
