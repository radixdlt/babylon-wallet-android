package rdx.works.profile.data.model.extensions

import com.radixdlt.ret.AccountDefaultDepositRule
import com.radixdlt.ret.ResourcePreference
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState

fun Network.Account.isOlympiaAccount(): Boolean {
    val unsecuredEntityControl = (securityState as? SecurityState.Unsecured)?.unsecuredEntityControl
    return when (val virtualBadge = unsecuredEntityControl?.transactionSigning?.badge) {
        is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
            virtualBadge.publicKey.curve == Slip10Curve.SECP_256K1
        }

        null -> false
    }
}

fun Profile.renameAccountDisplayName(
    accountToRename: Network.Account,
    newDisplayName: String
): Profile {
    val networkId = currentGateway.network.networkId()
    val renamedAccount = accountToRename.copy(
        displayName = newDisplayName
    )

    return copy(
        networks = networks.mapWhen(
            predicate = { it.networkID == networkId.value },
            mutation = { network ->
                network.copy(
                    accounts = network.accounts.mapWhen(
                        predicate = { it == accountToRename },
                        mutation = { renamedAccount }
                    )
                )
            }
        )
    )
}

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.toRETDepositRule(): AccountDefaultDepositRule {
    return when (this) {
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll -> AccountDefaultDepositRule.ACCEPT
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown -> AccountDefaultDepositRule.ALLOW_EXISTING
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll -> AccountDefaultDepositRule.REJECT
    }
}

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.toRETResourcePreference(): ResourcePreference {
    return when (this) {
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow -> ResourcePreference.ALLOWED
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny -> ResourcePreference.DISALLOWED
    }
}

fun Network.Account.hasAnyDenyDepositRule(forSpecificAssetAddress: String): Boolean {
    val thirdPartyDeposits = this.onLedgerSettings.thirdPartyDeposits

    val hasDenyAll = thirdPartyDeposits.depositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll
    val hasDenyExceptionRuleForAsset = thirdPartyDeposits.assetsExceptionList.any {
        it.exceptionRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny &&
            it.address == forSpecificAssetAddress
    }

    if (hasDenyAll || hasDenyExceptionRuleForAsset) {
        return true
    }

    return false
}
