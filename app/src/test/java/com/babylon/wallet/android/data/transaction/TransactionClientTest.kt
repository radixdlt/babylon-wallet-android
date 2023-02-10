package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.TestDispatcherRule
import com.radixdlt.toolkit.builders.ManifestBuilder
import com.radixdlt.toolkit.models.Instruction
import com.radixdlt.toolkit.models.Value
import com.radixdlt.toolkit.models.address.Address
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.ProfileDataSource

internal class TransactionClientTest {

    @get:Rule
    val coroutineRule = TestDispatcherRule()

    private val transactionRepository = mockk<TransactionRepository>()
    private val profileDataSource = mockk<ProfileDataSource>()
    private val accountRepository = mockk<AccountRepository>()
    private val getAccountResourceUseCase = mockk<GetAccountResourcesUseCase>()

    private lateinit var transactionClient: TransactionClient

    @Before
    fun setUp() {
        transactionClient = TransactionClient(
            transactionRepository, profileDataSource, accountRepository, getAccountResourceUseCase
        )
        coEvery { profileDataSource.getCurrentNetworkId() } returns Network.nebunet.networkId()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `correctly finds address needed for signing set metadata manifest`() = runTest {
        val manifest = ManifestBuilder().addInstruction(
            Instruction.SetMetadata(
                entityAddress = Address.ComponentAddress("component_sim1qgehpqdhhr62xh76wh6gppnyn88a0uau68epljprvj3sxknsqr"),
                Value.String("name"),
                Value.String("RadixDashboard")
            )
        ).addInstruction(
            Instruction.SetMetadata(
                entityAddress = Address.ComponentAddress("account_sim1q0egd2wpyslhkd28yuwpzq0qdg4aq73kl4urcnc3qsxsk6kug3"),
                Value.String("name"),
                Value.String("RadixDashboard")
            )
        ).build()
        val addresses = transactionClient.getAddressesNeededToSignTransaction(
            manifest
        )
        Assert.assertEquals("account_sim1q0egd2wpyslhkd28yuwpzq0qdg4aq73kl4urcnc3qsxsk6kug3", addresses.first())
    }
}