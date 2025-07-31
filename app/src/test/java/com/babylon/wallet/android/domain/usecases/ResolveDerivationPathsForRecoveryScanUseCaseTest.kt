package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.AccountPath
import com.radixdlt.sargon.Bip44LikePath
import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.DerivePublicKeysSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.Hardened
import com.radixdlt.sargon.HdPathComponent
import com.radixdlt.sargon.KeySpace
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.UnsecurifiedHardened
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.initFromLocal
import com.radixdlt.sargon.samples.sample
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.sample
import rdx.works.profile.domain.GetProfileUseCase

class ResolveDerivationPathsForRecoveryScanUseCaseTest {

    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val sut = ResolveDerivationPathsForRecoveryScanUseCase(
        getProfileUseCase = getProfileUseCase
    )

    @Test
    fun testWhenProfileExists() = runTest {
        val profile = Profile.sample().changeGatewayToNetworkId(NetworkId.MAINNET)

        coEvery { getProfileUseCase.finishedOnboardingProfile() } returns profile

        val bdfs = profile.deviceFactorSources.first()

        val paths = sut(
            source = DerivePublicKeysSource.FactorSource(v1 = bdfs.value.id),
            isOlympia = false,
            currentPathIndex = HdPathComponent.init(
                localKeySpace = 0u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            maxIndicesToResolve = INDICES_TO_RESOLVE
        )

        assertEquals(
            accountPaths(
                count = INDICES_TO_RESOLVE,
                from = 2, // Since two accounts are already stored in profile
                networkId = NetworkId.MAINNET
            ),
            paths.derivationPaths
        )

        assertEquals(
            HdPathComponent.init(
                localKeySpace = 12u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            paths.nextIndex,
        )
    }

    @Test
    fun testWhenProfileWithNoOlympiaAccountsExists() = runTest {
        val profile = Profile.sample().changeGatewayToNetworkId(NetworkId.MAINNET)

        coEvery { getProfileUseCase.finishedOnboardingProfile() } returns profile

        val bdfs = profile.deviceFactorSources.first()

        val paths = sut(
            source = DerivePublicKeysSource.FactorSource(v1 = bdfs.value.id),
            isOlympia = true,
            currentPathIndex = HdPathComponent.init(
                localKeySpace = 0u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            maxIndicesToResolve = INDICES_TO_RESOLVE
        )

        assertEquals(
            olympiaPaths(
                count = INDICES_TO_RESOLVE,
                from = 0
            ),
            paths.derivationPaths
        )

        assertEquals(
            HdPathComponent.init(
                localKeySpace = 10u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            paths.nextIndex,
        )
    }

    @Test
    fun testWhenNoProfileExists() = runTest {
        coEvery { getProfileUseCase.finishedOnboardingProfile() } returns null

        val paths = sut(
            source = DerivePublicKeysSource.FactorSource(
                v1 = FactorSource.Device.sample().value.id
            ),
            isOlympia = false,
            currentPathIndex = HdPathComponent.init(
                localKeySpace = 0u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            maxIndicesToResolve = INDICES_TO_RESOLVE
        )

        assertEquals(
            accountPaths(
                count = INDICES_TO_RESOLVE,
                from = 0,
                networkId = NetworkId.MAINNET
            ),
            paths.derivationPaths
        )

        assertEquals(
            HdPathComponent.init(
                localKeySpace = 10u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            paths.nextIndex,
        )
    }

    @Test
    fun testWhenFactorSourceNotFoundInProfile() = runTest {
        val profile = Profile.sample().changeGatewayToNetworkId(NetworkId.MAINNET)
        coEvery { getProfileUseCase.finishedOnboardingProfile() } returns profile

        val paths = sut(
            source = DerivePublicKeysSource.FactorSource(
                v1 = FactorSource.Ledger.sample().value.id
            ),
            isOlympia = false,
            currentPathIndex = HdPathComponent.init(
                localKeySpace = 0u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            maxIndicesToResolve = INDICES_TO_RESOLVE
        )

        assertEquals(
            accountPaths(
                count = INDICES_TO_RESOLVE,
                from = 0,
                networkId = NetworkId.MAINNET
            ),
            paths.derivationPaths
        )

        assertEquals(
            HdPathComponent.init(
                localKeySpace = 10u,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            ),
            paths.nextIndex,
        )
    }

    private fun accountPaths(
        count: Int,
        from: Int,
        networkId: NetworkId,
    ): List<DerivationPath> = List(count) { index ->
        val pathIndex = (from + index).toUInt()
        AccountPath.init(
            networkId = networkId,
            keyKind = Cap26KeyKind.TRANSACTION_SIGNING,
            index = Hardened.Unsecurified(UnsecurifiedHardened.initFromLocal(pathIndex))
        ).asGeneral()
    }

    private fun olympiaPaths(
        count: Int,
        from: Int,
    ): List<DerivationPath> = List(count) { index ->
        val pathIndex = (from + index).toUInt()
        Bip44LikePath.init(
            index = HdPathComponent.init(
                localKeySpace = pathIndex,
                keySpace = KeySpace.Unsecurified(isHardened = true)
            )
        ).asGeneral()
    }

    companion object {
        private val INDICES_TO_RESOLVE = 10
    }
}