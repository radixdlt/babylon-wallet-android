package rdx.works.core.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DepositAddressExceptionRule
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.EntityFlags
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LegacyOlympiaAccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.OnLedgerSettings
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.extensions.contains
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBabylonAddress

fun Collection<Account>.notHiddenAccounts(): List<Account> = filter { !it.isHidden }
fun Collection<Account>.hiddenAccounts(): List<Account> = filter { it.isHidden }

val Account.factorSourceId: FactorSourceId
    get() = securityState.factorSourceId

val Account.derivationPathScheme: DerivationPathScheme
    get() = securityState.derivationPathScheme

val Account.hasAuthSigning: Boolean
    get() = securityState.hasAuthSigning

val Account.derivationPathEntityIndex: UInt
    get() = securityState.transactionSigningFactorInstance.publicKey.derivationPath.entityIndex ?: 0u

val Account.usesEd25519: Boolean
    get() = securityState.usesEd25519

val Account.usesSECP256k1: Boolean
    get() = securityState.usesSECP256k1

val Account.isHidden: Boolean
    get() = EntityFlag.DELETED_BY_USER in flags

val Account.isOlympia: Boolean
    get() = usesSECP256k1

val Account.hasAcceptKnownDepositRule: Boolean
    get() = onLedgerSettings.thirdPartyDeposits.depositRule == DepositRule.ACCEPT_KNOWN

val Account.isLedgerAccount: Boolean
    get() = securityState.transactionSigningFactorInstance.factorSourceId.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET

fun Account.Companion.initBabylon(
    networkId: NetworkId,
    displayName: DisplayName,
    publicKey: PublicKey,
    derivationPath: DerivationPath,
    factorSourceId: FactorSourceId.Hash,
    onLedgerSettings: OnLedgerSettings = OnLedgerSettings(
        thirdPartyDeposits = ThirdPartyDeposits(
            depositRule = DepositRule.ACCEPT_ALL,
            assetsExceptionList = emptyList(),
            depositorsAllowList = emptyList()
        )
    ),
    flags: EntityFlags = EntityFlags.init(),
    customAppearanceId: AppearanceId? = null
): Account {
    val accountAddress = AccountAddress.init(publicKey, networkId)
    val unsecuredSecurityState = EntitySecurityState.unsecured(
        publicKey = publicKey,
        derivationPath = derivationPath,
        factorSourceId = factorSourceId
    )

    val appearanceId = customAppearanceId ?: AppearanceId.from(derivationPath)

    return Account(
        networkId = networkId,
        address = accountAddress,
        displayName = displayName,
        securityState = unsecuredSecurityState,
        appearanceId = appearanceId,
        flags = flags,
        onLedgerSettings = onLedgerSettings
    )
}

fun Account.Companion.initOlympia(
    networkId: NetworkId,
    displayName: DisplayName,
    publicKey: PublicKey.Secp256k1,
    derivationPath: DerivationPath,
    factorSourceId: FactorSourceId.Hash,
    onLedgerSettings: OnLedgerSettings = OnLedgerSettings(
        thirdPartyDeposits = ThirdPartyDeposits(
            depositRule = DepositRule.ACCEPT_ALL,
            assetsExceptionList = emptyList(),
            depositorsAllowList = emptyList()
        )
    ),
    flags: EntityFlags = EntityFlags.init(),
    customAppearanceId: AppearanceId? = null
): Account {
    val accountAddress = LegacyOlympiaAccountAddress.init(publicKey).toBabylonAddress()
    val unsecuredSecurityState = EntitySecurityState.unsecured(
        publicKey = publicKey,
        derivationPath = derivationPath,
        factorSourceId = factorSourceId
    )

    val appearanceId = customAppearanceId ?: AppearanceId.from(derivationPath)

    return Account(
        networkId = networkId,
        address = accountAddress,
        displayName = displayName,
        securityState = unsecuredSecurityState,
        appearanceId = appearanceId,
        flags = flags,
        onLedgerSettings = onLedgerSettings
    )
}

fun Account.isSignatureRequiredBasedOnDepositRules(
    forSpecificAssetAddress: ResourceAddress,
    addressesOfAssetsOfTargetAccount: List<ResourceAddress> = emptyList()
): Boolean {
    val thirdPartyDeposits = onLedgerSettings.thirdPartyDeposits

    val hasDenyAll = thirdPartyDeposits.depositRule == DepositRule.DENY_ALL
    val hasAcceptKnown = thirdPartyDeposits.depositRule == DepositRule.ACCEPT_KNOWN

    val hasDenyExceptionRuleForAsset = thirdPartyDeposits.assetsExceptionList.any {
        it.exceptionRule == DepositAddressExceptionRule.DENY && it.address == forSpecificAssetAddress
    }

    val hasAllowExceptionRuleForAsset = thirdPartyDeposits.assetsExceptionList.any {
        it.exceptionRule == DepositAddressExceptionRule.ALLOW && it.address == forSpecificAssetAddress
    }

    return when {
        hasAllowExceptionRuleForAsset -> false
        hasDenyAll || hasDenyExceptionRuleForAsset -> true
        // and if the receiving account knows the resource then do not require signature
        hasAcceptKnown -> !addressesOfAssetsOfTargetAccount.contains(forSpecificAssetAddress)
        else -> false
    }
}
