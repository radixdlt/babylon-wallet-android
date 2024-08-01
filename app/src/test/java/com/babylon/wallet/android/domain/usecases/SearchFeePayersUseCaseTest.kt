package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.fakes.StateRepositoryFake
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AccountOrAddressOf
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.PerAssetFungibleResource
import com.radixdlt.sargon.PerAssetFungibleTransfer
import com.radixdlt.sargon.PerAssetTransfers
import com.radixdlt.sargon.PerAssetTransfersOfFungibleResource
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.perAssetTransfers
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase

class SearchFeePayersUseCaseTest {

    private val profile = Profile.sample().changeGateway(Gateway.forNetwork(NetworkId.MAINNET)).unHideAllEntities()
    private val account1 = profile.activeAccountsOnCurrentNetwork[0]
    private val profileUseCase = GetProfileUseCase(profileRepository = FakeProfileRepository(profile))
    private val useCase = SearchFeePayersUseCase(
        profileUseCase = profileUseCase,
        stateRepository = StateRepositoryFake()
    )

    @Test
    fun `when account with enough xrd exists, returns the selected fee payer`() =
        runTest {
            val manifestData = manifestDataWithAddress(account1)

            val result = useCase(manifestData, TransactionConfig.DEFAULT_LOCK_FEE.toDecimal192()).getOrThrow()

            assertEquals(
                TransactionFeePayers(
                    selectedAccountAddress = account1.address,
                    candidates = listOf(
                        TransactionFeePayers.FeePayerCandidate(account1, 100.toDecimal192())
                    )
                ),
                result
            )
        }

    @Test
    fun `when account with xrd does not exist, returns the null fee payer`() =
        runTest {
            val manifestData = manifestDataWithAddress(account1)

            val result = useCase(manifestData, 200.toDecimal192()).getOrThrow()

            assertEquals(
                TransactionFeePayers(
                    selectedAccountAddress = null,
                    candidates = listOf(
                        TransactionFeePayers.FeePayerCandidate(account1, 100.toDecimal192())
                    )
                ),
                result
            )
        }

    companion object {
        private fun manifestDataWithAddress(
            account: Account
        ) = TransactionManifestData.from(
            manifest = TransactionManifest.perAssetTransfers(
                transfers = PerAssetTransfers(
                    fromAccount = account.address,
                    fungibleResources = listOf(
                        PerAssetTransfersOfFungibleResource(
                            resource = PerAssetFungibleResource(
                                resourceAddress = XrdResource.address(networkId = account.networkId),
                                divisibility = 18.toUByte()
                            ),
                            transfers = listOf(
                                PerAssetFungibleTransfer(
                                    useTryDepositOrAbort = true,
                                    amount = 10.toDecimal192(),
                                    recipient = AccountOrAddressOf.AddressOfExternalAccount(
                                        value = AccountAddress.sampleMainnet.random()
                                    )
                                )
                            )
                        )
                    ),
                    nonFungibleResources = emptyList()
                )
            )
        )

    }

}
