package com.babylon.wallet.android.presentation.model

data class TokenUi(
    val id: String,
    val name: String?,
    val symbol: String?, // short capitalized name
    val tokenQuantity: String, // the amount of the token held
    val tokenValue: String?, // the current value in currency the user has selected for the wallet
    val iconUrl: String?
) {
    /**
     * The title to show in the token list item of the Account screen.
     *
     * It is based of the token display rule:
     *
     * https://radixdlt.atlassian.net/wiki/spaces/AT/pages/2844753933/Rules+for+Display+of+Assets+in+Account+Detail+View
     *
     */
    val tokenItemTitle: String get() = symbol ?: name.orEmpty()
}
