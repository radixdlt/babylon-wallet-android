package rdx.works.profile.data.model.extensions

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.apppreferences.Display
import rdx.works.profile.data.model.apppreferences.Transaction

fun Profile.changeDefaultDepositGuarantee(
    defaultDepositGuarantee: Double
): Profile {
    return copy(
        appPreferences = AppPreferences(
            transaction = Transaction(defaultDepositGuarantee = defaultDepositGuarantee),
            display = appPreferences.display,
            security = appPreferences.security,
            gateways = appPreferences.gateways,
            p2pLinks = appPreferences.p2pLinks
        )
    )
}

fun Profile.changeBalanceVisibility(
    isVisible: Boolean
): Profile {
    return copy(
        appPreferences = AppPreferences(
            transaction = appPreferences.transaction,
            display = Display(
                fiatCurrencyPriceTarget = appPreferences.display.fiatCurrencyPriceTarget,
                isCurrencyAmountVisible = isVisible
            ),
            security = appPreferences.security,
            gateways = appPreferences.gateways,
            p2pLinks = appPreferences.p2pLinks
        )
    )
}
