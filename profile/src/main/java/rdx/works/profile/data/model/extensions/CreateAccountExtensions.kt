package rdx.works.profile.data.model.extensions

import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.radixdlt.ret.PublicKey
import com.radixdlt.ret.deriveVirtualAccountAddressFromPublicKey
import rdx.works.core.toHexString
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.factorsources.DerivationPathScheme
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.NetworkId

@Suppress("LongParameterList")
fun Profile.createAccount(
    displayName: String,
    onNetworkId: NetworkId,
    compressedPublicKey: ByteArray,
    derivationPath: DerivationPath,
    factorSource: FactorSource.CreatingEntity,
    onLedgerSettings: Network.Account.OnLedgerSettings
): Network.Account {
    val address = deriveVirtualAccountAddressFromPublicKey(
        PublicKey.Ed25519(compressedPublicKey),
        onNetworkId.value.toUByte()
    ).addressString()

    val unsecuredSecurityState = SecurityState.unsecured(
        publicKey = FactorInstance.PublicKey(compressedPublicKey.toHexString(), Slip10Curve.CURVE_25519),
        derivationPath = derivationPath,
        factorSourceId = (factorSource as FactorSource).id as FactorSource.FactorSourceID.FromHash
    )

    return Network.Account(
        address = address,
        appearanceID = nextAppearanceId(forNetworkId = onNetworkId),
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
