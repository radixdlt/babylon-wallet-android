package rdx.works.profile.data.model.extensions

import com.babylon.wallet.android.designsystem.theme.AccountGradientList
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
import rdx.works.profile.ret.crypto.PublicKey

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
        PublicKey.Ed25519(compressedPublicKey).deriveAccountAddress(onNetworkId.value)
    } else {
        PublicKey.Secp256k1(compressedPublicKey).deriveOlympiaAccountAddress(networkId = onNetworkId.value)
    }

    val unsecuredSecurityState = SecurityState.unsecured(
        publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
        derivationPath = derivationPath,
        factorSourceId = (factorSource as FactorSource).id as FactorSource.FactorSourceID.FromHash
    )

    return Network.Account(
        address = address,
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
    val address = PublicKey.Ed25519(compressedPublicKey).deriveAccountAddress(networkId = onNetworkId.value)

    val unsecuredSecurityState = SecurityState.unsecured(
        publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
        derivationPath = derivationPath,
        factorSourceId = (factorSource as FactorSource).id as FactorSource.FactorSourceID.FromHash
    )

    return Network.Account(
        address = address,
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
