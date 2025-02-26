package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.AccountPath
import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.DerivePublicKeysSource
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.KeySpace
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.asHardened
import com.radixdlt.sargon.extensions.indexInGlobalKeySpace
import com.radixdlt.sargon.extensions.indexInLocalKeySpace
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.lastPathComponent
import com.radixdlt.sargon.extensions.scheme
import com.radixdlt.sargon.extensions.unsecuredControllingFactorInstance
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.toFactorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ResolveDerivationPathsForRecoveryScanUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(
        source: DerivePublicKeysSource,
        isOlympia: Boolean,
        currentPathIndex: HdPathComponent,
        maxIndicesToResolve: Int
    ): Paths {
        val profile = getProfileUseCase.finishedOnboardingProfile()

        val networkId = profile?.currentGateway?.network?.id ?: NetworkId.MAINNET
        val usedIndices = if (profile != null) {
            val factorSourceId = source.toFactorSourceId()
            val network = profile.networks.asIdentifiable().getBy(networkId)
            val derivationPathScheme = if (isOlympia) {
                DerivationPathScheme.BIP44_OLYMPIA
            } else {
                DerivationPathScheme.CAP26
            }

            network
                ?.accounts
                ?.filter { account ->
                    val transactionSigning = account.unsecuredControllingFactorInstance ?: return@filter false
                    transactionSigning.factorSourceId == factorSourceId.value &&
                        transactionSigning.publicKey.derivationPath.scheme == derivationPathScheme
                }
                ?.mapNotNull { account ->
                    account.unsecuredControllingFactorInstance?.publicKey?.derivationPath?.lastPathComponent?.indexInLocalKeySpace
                }
                ?.toSet().orEmpty()
        } else {
            emptySet()
        }

        val indicesToScan = LinkedHashSet<HdPathComponent>()
        var currentIndex = currentPathIndex.indexInLocalKeySpace
        do {
            if (currentIndex !in usedIndices) {
                indicesToScan.add(
                    HdPathComponent.init(
                        localKeySpace = currentIndex,
                        keySpace = KeySpace.Unsecurified(isHardened = true)
                    )
                )
            }
            currentIndex++
        } while (indicesToScan.size < maxIndicesToResolve)

        val paths = indicesToScan.map { index ->
            if (isOlympia) {
                Bip44LikePath.init(index = index).asGeneral()
            } else {
                AccountPath.init(
                    networkId = networkId,
                    keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
                    index = index.asHardened()
                ).asGeneral()
            }
        }

        return Paths(
            derivationPaths = paths,
            networkId = networkId
        )
    }

    data class Paths(
        val derivationPaths: List<DerivationPath>,
        val networkId: NetworkId
    ) {

        val nextIndex: HdPathComponent
            get() {
                val nextIndexInGlobalKeySpace = if (derivationPaths.isNotEmpty()) {
                    derivationPaths.maxOf { it.lastPathComponent.indexInGlobalKeySpace } + 1u
                } else {
                    0u
                }

                return HdPathComponent.init(globalKeySpace = nextIndexInGlobalKeySpace)
            }
    }
}
