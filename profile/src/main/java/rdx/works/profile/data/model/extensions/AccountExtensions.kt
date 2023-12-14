package rdx.works.profile.data.model.extensions

import com.radixdlt.ret.AccountDefaultDepositRule
import com.radixdlt.ret.ResourcePreference
import rdx.works.core.mapWhen
import rdx.works.core.toIdentifiedArrayList
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.isBip44LikePath

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
                    ).toIdentifiedArrayList()
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

fun Network.Account.hasAcceptKnownDepositRule(): Boolean {
    val thirdPartyDeposits = this.onLedgerSettings.thirdPartyDeposits
    return thirdPartyDeposits.depositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown
}

@Suppress("ReturnCount")
fun Network.Account.isSignatureRequiredBasedOnDepositRules(
    forSpecificAssetAddress: String,
    addressesOfAssetsOfTargetAccount: List<String> = emptyList()
): Boolean {
    val thirdPartyDeposits = this.onLedgerSettings.thirdPartyDeposits

    val hasDenyAll = thirdPartyDeposits.depositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll

    val hasAcceptKnown = thirdPartyDeposits.depositRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown

    val hasDenyExceptionRuleForAsset = thirdPartyDeposits.assetsExceptionList?.any {
        it.exceptionRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny &&
            it.address == forSpecificAssetAddress
    } == true

    val hasAllowExceptionRuleForAsset = thirdPartyDeposits.assetsExceptionList?.any {
        it.exceptionRule == Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow &&
            it.address == forSpecificAssetAddress
    } == true

    if (hasAllowExceptionRuleForAsset) {
        return false
    } else if (hasDenyAll || hasDenyExceptionRuleForAsset) {
        return true
    } else if (hasAcceptKnown) {
        // and if the receiving account knows the resource then do not require signature
        if (addressesOfAssetsOfTargetAccount.contains(forSpecificAssetAddress)) {
            return false
        }
        return true
    }

    return false
}

fun Network.Account.updateDerivationPathScheme(derivationPathScheme: DerivationPathScheme): Network.Account {
    val transactionSigning = (this.securityState as SecurityState.Unsecured).unsecuredEntityControl.transactionSigning
    return when (transactionSigning.badge) {
        is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
            val updatedBadge =
                transactionSigning.badge.copy(derivationPath = transactionSigning.badge.derivationPath.copy(scheme = derivationPathScheme))
            val updatedTransactionSigning = transactionSigning.copy(badge = updatedBadge)
            val updatedUnsecuredEntityControl =
                this.securityState.unsecuredEntityControl.copy(transactionSigning = updatedTransactionSigning)
            val updatedSecurityState = SecurityState.Unsecured(unsecuredEntityControl = updatedUnsecuredEntityControl)
            copy(securityState = updatedSecurityState)
        }
    }
}

val Network.Account.hasWrongDerivationPathScheme: Boolean
    get() {
        val transactionSigning = (this.securityState as SecurityState.Unsecured).unsecuredEntityControl.transactionSigning
        return when (transactionSigning.badge) {
            is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> {
                transactionSigning.badge.derivationPath.isBip44LikePath() &&
                    transactionSigning.badge.derivationPath.scheme == DerivationPathScheme.CAP_26
            }
        }
    }
