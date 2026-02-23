@file:Suppress("UnusedParameter")

package rdx.works.core

fun logNonFatalException(throwable: Throwable) {
    // No-op: Firebase Crashlytics not available in light flavor
}

fun deleteCrashlyticsUnsentReports() {
    // No-op
}

fun enableCrashlytics(enabled: Boolean) {
    // No-op
}
