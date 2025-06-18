package com.babylon.wallet.android.presentation.discover.common.models

import com.babylon.wallet.android.R

enum class SocialLinkType(
    val url: String
) {

    Telegram("https://t.me/radix_dlt"),
    X("https://x.com/radixdlt"),
    Discord("https://go.radixdlt.com/Discord")
}

fun SocialLinkType.icon(isDarkTheme: Boolean): Int {
    return when (this) {
        SocialLinkType.Telegram -> R.drawable.ic_telegram_logo
        SocialLinkType.X -> if (isDarkTheme) {
            R.drawable.ic_x_logo_light
        } else {
            R.drawable.ic_x_logo
        }

        SocialLinkType.Discord -> R.drawable.ic_discord_logo
    }
}

val SocialLinkType.titleRes: Int
    get() = when (this) {
        SocialLinkType.Telegram -> R.string.discover_socialLinks_telegram_title
        SocialLinkType.X -> R.string.discover_socialLinks_twitter_title
        SocialLinkType.Discord -> R.string.discover_socialLinks_discord_title
    }

val SocialLinkType.descriptionRes: Int
    get() = when (this) {
        SocialLinkType.Telegram -> R.string.discover_socialLinks_telegram_subtitle
        SocialLinkType.X -> R.string.discover_socialLinks_twitter_subtitle
        SocialLinkType.Discord -> R.string.discover_socialLinks_discord_subtitle
    }
