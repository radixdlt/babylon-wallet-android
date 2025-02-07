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
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.LegacyOlympiaAccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.OnLedgerSettings
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.extensions.EntityFlags
import com.radixdlt.sargon.extensions.default
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.path
import com.radixdlt.sargon.extensions.toBabylonAddress

fun Collection<Account>.active(): List<Account> = filterNot { it.isHidden || it.isDeleted }

val Account.factorSourceId: FactorSourceId
    get() = securityState.factorSourceId

val Account.derivationPathScheme: DerivationPathScheme
    get() = securityState.derivationPathScheme

val Account.derivationPathEntityIndex: HdPathComponent
    get() = securityState.transactionSigningFactorInstance.publicKey.derivationPath.path.components.last()

val Account.isHidden: Boolean
    get() = EntityFlag.HIDDEN_BY_USER in flags

val Account.isDeleted: Boolean
    get() = EntityFlag.TOMBSTONED_BY_USER in flags

val Account.hasAcceptKnownDepositRule: Boolean
    get() = onLedgerSettings.thirdPartyDeposits.depositRule == DepositRule.ACCEPT_KNOWN

@Suppress("LongParameterList")
fun Account.Companion.initBabylon(
    networkId: NetworkId,
    displayName: DisplayName,
    hdPublicKey: HierarchicalDeterministicPublicKey,
    factorSourceId: FactorSourceId.Hash,
    onLedgerSettings: OnLedgerSettings = OnLedgerSettings.default(),
    flags: EntityFlags = EntityFlags(),
    customAppearanceId: AppearanceId? = null
): Account {
    val accountAddress = AccountAddress.init(hdPublicKey.publicKey, networkId)
    val unsecuredSecurityState = EntitySecurityState.unsecured(
        hdPublicKey = hdPublicKey,
        factorSourceId = factorSourceId
    )

    val appearanceId = customAppearanceId ?: AppearanceId.from(hdPublicKey.derivationPath)

    return Account(
        networkId = networkId,
        address = accountAddress,
        displayName = displayName,
        securityState = unsecuredSecurityState,
        appearanceId = appearanceId,
        flags = flags.asList(),
        onLedgerSettings = onLedgerSettings
    )
}

@Suppress("LongParameterList")
fun Account.Companion.initOlympia(
    networkId: NetworkId,
    displayName: DisplayName,
    publicKey: PublicKey.Secp256k1,
    derivationPath: DerivationPath.Bip44Like,
    factorSourceId: FactorSourceId.Hash,
    onLedgerSettings: OnLedgerSettings = OnLedgerSettings(
        thirdPartyDeposits = ThirdPartyDeposits(
            depositRule = DepositRule.ACCEPT_ALL,
            assetsExceptionList = null,
            depositorsAllowList = null
        )
    ),
    flags: EntityFlags = EntityFlags(),
    customAppearanceId: AppearanceId? = null
): Account {
    val accountAddress = LegacyOlympiaAccountAddress.init(publicKey).toBabylonAddress()
    val unsecuredSecurityState = EntitySecurityState.unsecured(
        factorSourceId = factorSourceId,
        hdPublicKey = HierarchicalDeterministicPublicKey(
            publicKey = publicKey,
            derivationPath = derivationPath
        )
    )

    val appearanceId = customAppearanceId ?: AppearanceId.from(derivationPath)

    return Account(
        networkId = networkId,
        address = accountAddress,
        displayName = displayName,
        securityState = unsecuredSecurityState,
        appearanceId = appearanceId,
        flags = flags.asList(),
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

    val rule = thirdPartyDeposits.assetsExceptionList?.asIdentifiable()?.getBy(forSpecificAssetAddress)?.exceptionRule
    return when {
        rule == DepositAddressExceptionRule.ALLOW -> false
        hasDenyAll || rule == DepositAddressExceptionRule.DENY -> true
        // and if the receiving account knows the resource then do not require signature
        hasAcceptKnown -> forSpecificAssetAddress !in addressesOfAssetsOfTargetAccount
        else -> false
    }
}
