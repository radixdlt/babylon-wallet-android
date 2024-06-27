package com.babylon.wallet.android.domain.model

enum class Browser(val browserName: String, val packageName: String) {
    CHROME("Chrome", "com.android.chrome"),
    FIREFOX("Firefox", "org.mozilla.firefox"),
    BRAVE("Brave", "com.brave.browser"),
    OPERA("Opera", "com.opera.browser"),
    DUCKDUCKGO("DuckDuckGo", "com.duckduckgo.mobile.android"),
    EDGE("Microsoft Edge", "com.microsoft.emmx"),
    SAMSUNG("Samsung Internet for Android", "com.sec.android.app.sbrowser"),
    YANDEX("Yandex Browser", "com.yandex.browser"),
    DEFAULT("Default", "");

    companion object {
        fun fromBrowserName(browserName: String?): Browser {
            return entries.firstOrNull { it.browserName == browserName } ?: DEFAULT
        }
    }
}
