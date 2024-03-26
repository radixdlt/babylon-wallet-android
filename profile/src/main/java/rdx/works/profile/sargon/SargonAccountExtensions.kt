package rdx.works.profile.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.AssetException
import com.radixdlt.sargon.DepositAddressExceptionRule
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorInstanceBadge
import com.radixdlt.sargon.FactorInstanceBadgeVirtualSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromAddress
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.OnLedgerSettings
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ThirdPartyDeposits
import com.radixdlt.sargon.UnsecuredEntityControl
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.EntityFlag
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState

// These extensions should be temporary, they exist to ease the transition from our own profile types to Sargon's
fun Network.Account.toSargon(): Account = Account(
    networkId = NetworkId.init(discriminant = networkID.toUByte()),
    address = AccountAddress.init(validatingAddress = address),
    displayName = DisplayName(value = displayName),
    securityState = securityState.toSargon(),
    appearanceId = AppearanceId(value = appearanceID.toUByte()),
    flags = flags.map { it.toSargon() },
    onLedgerSettings = onLedgerSettings.toSargon()
)

private fun SecurityState.toSargon(): EntitySecurityState = when (this) {
    is SecurityState.Unsecured -> EntitySecurityState.Unsecured(
        value = UnsecuredEntityControl(
            transactionSigning = unsecuredEntityControl.transactionSigning.toSargonHD(),
            authenticationSigning = unsecuredEntityControl.authenticationSigning?.toSargonHD()
        )
    )
}

private fun FactorInstance.toSargonHD(): HierarchicalDeterministicFactorInstance = HierarchicalDeterministicFactorInstance(
    factorSourceId = (factorSourceId.toSargon() as FactorSourceId.Hash).value,
    publicKey = with((badge.toSargon() as FactorInstanceBadge.Virtual).value as FactorInstanceBadgeVirtualSource.HierarchicalDeterministic) {
        HierarchicalDeterministicPublicKey(
            publicKey = this.value.publicKey,
            derivationPath = value.derivationPath
        )
    }
)

private fun FactorSource.FactorSourceID.toSargon(): FactorSourceId = when (this) {
    is FactorSource.FactorSourceID.FromAddress -> FactorSourceId.Address(
        value = FactorSourceIdFromAddress(
            kind = kind.toSargon(),
            body = AccountAddress.init(validatingAddress = body.value)
        )
    )
    is FactorSource.FactorSourceID.FromHash -> FactorSourceId.Hash(
        value = FactorSourceIdFromHash(
            kind = kind.toSargon(),
            body = Exactly32Bytes.init(bytes = body.value.hexToBagOfBytes())
        )
    )
}

fun FactorSourceKind.toSargon(): com.radixdlt.sargon.FactorSourceKind = when (this) {
    FactorSourceKind.DEVICE -> com.radixdlt.sargon.FactorSourceKind.DEVICE
    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> com.radixdlt.sargon.FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
    FactorSourceKind.OFF_DEVICE_MNEMONIC -> com.radixdlt.sargon.FactorSourceKind.OFF_DEVICE_MNEMONIC
    FactorSourceKind.TRUSTED_CONTACT -> com.radixdlt.sargon.FactorSourceKind.TRUSTED_CONTACT
}

private fun FactorInstance.Badge.toSargon(): FactorInstanceBadge = when (this) {
    is FactorInstance.Badge.VirtualSource.HierarchicalDeterministic -> FactorInstanceBadge.Virtual(
        value = FactorInstanceBadgeVirtualSource.HierarchicalDeterministic(
            value = HierarchicalDeterministicPublicKey(
                publicKey = publicKey.toSargon(),
                derivationPath = derivationPath.toSargon()
            )
        )
    )
}

fun FactorInstance.PublicKey.toSargon(): PublicKey = when (curve) {
    Slip10Curve.CURVE_25519 -> PublicKey.Ed25519.init(hex = compressedData)
    Slip10Curve.SECP_256K1 -> PublicKey.Secp256k1.init(hex = compressedData)
}

fun DerivationPath.toSargon(): com.radixdlt.sargon.DerivationPath = when (scheme) {
    DerivationPathScheme.CAP_26 -> com.radixdlt.sargon.DerivationPath.Cap26.init(path)
    DerivationPathScheme.BIP_44_OLYMPIA -> com.radixdlt.sargon.DerivationPath.Bip44Like.init(path)
}

fun EntityFlag.toSargon() = when (this) {
    EntityFlag.DeletedByUser -> com.radixdlt.sargon.EntityFlag.DELETED_BY_USER
}

fun Network.Account.OnLedgerSettings.toSargon(): OnLedgerSettings = OnLedgerSettings(
    thirdPartyDeposits = thirdPartyDeposits.toSargon()
)

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.toSargon(): ThirdPartyDeposits = ThirdPartyDeposits(
    depositRule = depositRule.toSargon(),
    assetsExceptionList = assetsExceptionList?.map { it.toSargon() }.orEmpty(),
    depositorsAllowList = depositorsAllowList?.map { it.toSargon() }.orEmpty()
)

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.toSargon() = when (this) {
    Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll -> DepositRule.ACCEPT_ALL
    Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptKnown -> DepositRule.ACCEPT_KNOWN
    Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.DenyAll -> DepositRule.DENY_ALL
}

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.AssetException.toSargon() = AssetException(
    address = ResourceAddress.init(validatingAddress = address),
    exceptionRule = when (exceptionRule) {
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Allow -> DepositAddressExceptionRule.ALLOW
        Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositAddressExceptionRule.Deny -> DepositAddressExceptionRule.DENY
    }
)

fun Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.toSargon() = when (this) {
    is Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.NonFungibleGlobalID ->
        ResourceOrNonFungible.NonFungible(value = NonFungibleGlobalId.init(value))
    is Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositorAddress.ResourceAddress -> ResourceOrNonFungible.Resource(
        value = ResourceAddress.init(validatingAddress = address)
    )
}