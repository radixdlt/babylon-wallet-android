package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.usecases.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CollectSignersSignaturesUseCase
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.ret.TransactionManifest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.core.identifiedArrayListOf
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.ret.ManifestPoet
import rdx.works.profile.ret.addLockFeeInstructionToManifest
import rdx.works.profile.ret.crypto.PrivateKey
import rdx.works.profile.ret.sampleXRDWithdraw
import java.math.BigDecimal

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val getProfileUseCase = GetProfileUseCase(ProfileRepositoryFake)
    private val collectSignersSignaturesUseCase = mockk<CollectSignersSignaturesUseCase>()
    private val resolveNotaryAndSignersUseCase = ResolveNotaryAndSignersUseCase(getProfileUseCase)

    private lateinit var transactionClient: TransactionClient

    @Before
    fun setUp() {
        coEvery { collectSignersSignaturesUseCase.interactionState } returns emptyFlow()
        transactionClient = TransactionClient(
            transactionRepository,
            resolveNotaryAndSignersUseCase,
            collectSignersSignaturesUseCase
        )
    }

    @Test
    fun `when address exists, finds address involved & signing for set metadata manifest`() =
        runTest {
            val manifest = manifestWithAddress(account1)
                .addLockFeeInstructionToManifest(account1.address, TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal()).summary(
                    networkId = Radix.Gateway.default.network.id.toUByte()
                )

            val notaryKey = PrivateKey.EddsaEd25519.newRandom()
            val notaryAndSigners = resolveNotaryAndSignersUseCase(manifest, notaryKey)

            Assert.assertEquals(listOf(account1), notaryAndSigners.getOrNull()?.signers)
        }

    companion object {
        private val account1 = account(name = "account1", address = "account_rdx12x20vgu94d96g3demdumxl6yjpvm0jy8dhrr03g75299ghxrwq76uh")
        private val account2 = account(name = "account2", address = "account_rdx12x20vgu94d96g3demdumxl6yjpvm0jy8dhrr03g75299ghxrwq73uh")

        private fun manifestWithAddress(
            account: Network.Account
        ): TransactionManifest = sampleXRDWithdraw(
            fromAddress = account.address,
            value = BigDecimal.TEN
        )

        private object ProfileRepositoryFake : ProfileRepository {
            private val profile = profile(accounts = identifiedArrayListOf(account1, account2))

            override val profileState: Flow<ProfileState> = flowOf(ProfileState.Restored(profile = profile))

            override val inMemoryProfileOrNull: Profile?
                get() = profile

            override suspend fun saveProfile(profile: Profile) {
                error("Not needed")
            }

            override suspend fun clearProfileDataOnly() {
                error("Not needed")
            }

            override suspend fun clearAllWalletData() {
                error("Not needed")
            }

            override fun deriveProfileState(content: String): ProfileState {
                error("Not needed")
            }
        }
    }
}
