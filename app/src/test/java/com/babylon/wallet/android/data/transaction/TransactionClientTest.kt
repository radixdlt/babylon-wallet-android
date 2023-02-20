package com.babylon.wallet.android.data.transaction

import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
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
        coEvery { getAccountResourceUseCase("account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py7ab") } returns Result.Success(
            SampleDataProvider().sampleAccountResource("account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py7ab")
        )
        coEvery { getAccountResourceUseCase("account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej") } returns Result.Success(
            SampleDataProvider().sampleAccountResource("account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej")
        )
        coEvery { profileDataSource.getCurrentNetworkId() } returns Network.nebunet.networkId()

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `finds address involved & signing for set metadata manifest`() = runTest {
        var manifest = ManifestBuilder().addInstruction(
            Instruction.SetMetadata(
                entityAddress = Address.ComponentAddress("account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej"),
                Value.String("name"),
                Value.String("RadixDashboard")
            )
        ).build()
        val addressesInvolved = transactionClient.getAddressesInvolvedInATransaction(manifest)
        val addressToLockFee = transactionClient.selectAccountAddressToLockFee(addressesInvolved)
        assert(addressToLockFee != null)
        manifest = manifest.addLockFeeInstructionToManifest(addressToLockFee!!)
        val addressesNeededToSign = transactionClient.getAddressesNeededToSign(manifest)
        Assert.assertEquals(1, addressesNeededToSign.size)
        Assert.assertEquals(
            "account_tdx_22_1pp59nka549kq56lrh4evyewk00thgnw0cntfwgyjqn7q2py8ej",
            addressesNeededToSign.first()
        )
    }
}