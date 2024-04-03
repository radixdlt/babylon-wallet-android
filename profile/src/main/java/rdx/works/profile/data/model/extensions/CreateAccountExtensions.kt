package rdx.works.profile.data.model.extensions

import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LegacyOlympiaAccountAddress
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toBabylonAddress
import com.radixdlt.sargon.extensions.toBagOfBytes
import rdx.works.core.toHexString
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.data.model.pernetwork.derivationPathEntityIndex
import rdx.works.profile.derivation.model.NetworkId

@Suppress("LongParameterList")
fun Profile.createAccount(
    displayName: String,
    onNetworkId: NetworkId,
    compressedPublicKey: ByteArray,
    derivationPath: DerivationPath,
    factorSource: FactorSource.CreatingEntity,
    onLedgerSettings: Network.Account.OnLedgerSettings,
    isForLegacyOlympia: Boolean = false, // optional - used for account recovery
    appearanceID: Int? = null // optional - used for account recovery
): Network.Account {
    val address = if (isForLegacyOlympia.not()) {
        val publicKey = PublicKey.Ed25519.init(bytes = compressedPublicKey.toBagOfBytes())
        AccountAddress.init(publicKey, com.radixdlt.sargon.NetworkId.init(onNetworkId.value.toUByte()))
    } else {
        val publicKey = PublicKey.Secp256k1.init(bytes = compressedPublicKey.toBagOfBytes())
        LegacyOlympiaAccountAddress.init(publicKey).toBabylonAddress()
    }

    val unsecuredSecurityState = SecurityState.unsecured(
        publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
        derivationPath = derivationPath,
        factorSourceId = (factorSource as FactorSource).id as FactorSource.FactorSourceID.FromHash
    )

    return Network.Account(
        address = address.string,
        appearanceID = if (appearanceID == null) { // then take the next ID based on the accounts of the existing network
            nextAppearanceId(forNetworkId = onNetworkId)
        } else {
            derivationPath.derivationPathEntityIndex() % AccountGradientList.count()
        },
        displayName = displayName,
        networkID = onNetworkId.value,
        securityState = unsecuredSecurityState,
        onLedgerSettings = onLedgerSettings
    )
}

/**
 * This function does not depend on an existing profile (see above extension function).
 * It creates and returns a single account. Mostly used for account recovery.
 *
 */
@Suppress("LongParameterList")
fun initializeAccount(
    displayName: String,
    onNetworkId: NetworkId,
    compressedPublicKey: ByteArray,
    derivationPath: DerivationPath,
    factorSource: FactorSource.CreatingEntity,
    onLedgerSettings: Network.Account.OnLedgerSettings
): Network.Account {
    val publicKey = PublicKey.Ed25519.init(bytes = compressedPublicKey.toBagOfBytes())
    val accountAddress = AccountAddress.init(
        publicKey = publicKey,
        networkId = com.radixdlt.sargon.NetworkId.init(discriminant = onNetworkId.value.toUByte())
    )
    val unsecuredSecurityState = SecurityState.unsecured(
        publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
        derivationPath = derivationPath,
        factorSourceId = (factorSource as FactorSource).id as FactorSource.FactorSourceID.FromHash
    )

    return Network.Account(
        address = accountAddress.string,
        appearanceID = derivationPath.derivationPathEntityIndex() % AccountGradientList.count(),
        displayName = displayName,
        networkID = onNetworkId.value,
        securityState = unsecuredSecurityState,
        onLedgerSettings = onLedgerSettings
    )
}

fun Profile.nextAccountIndex(
    factorSource: FactorSource,
    derivationPathScheme: DerivationPathScheme,
    forNetworkId: NetworkId
): Int {
    val forNetwork = networks.firstOrNull { it.networkID == forNetworkId.value } ?: return 0
    val accountsControlledByFactorSource = forNetwork.accounts.filter {
        it.factorSourceId == factorSource.id && it.derivationPathScheme == derivationPathScheme
    }

    return if (accountsControlledByFactorSource.isEmpty()) {
        0
    } else {
        accountsControlledByFactorSource.maxOf { it.derivationPathEntityIndex } + 1
    }
}

fun Profile.nextAppearanceId(forNetworkId: NetworkId): Int {
    val forNetwork = networks.firstOrNull { it.networkID == forNetworkId.value } ?: return 0
    return forNetwork.accounts.count() % AccountGradientList.count()
}
