package com.babylon.wallet.android.presentation.settings.securitycenter.mfafactorinstance

import app.cash.turbine.test
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceInput
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceOutput
import com.babylon.wallet.android.presentation.selectfactorsource.SelectFactorSourceProxy
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressBookEntry
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorInstance
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.Gateway
import com.radixdlt.sargon.MfaFactorInstance
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.SargonOs
import com.radixdlt.sargon.UsedMfaSignatureResourceWithAccounts
import com.radixdlt.sargon.extensions.forNetwork
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.nonFungibleGlobalId
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleRandom
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.changeGateway
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.addressbook.GetAddressBookEntriesOnCurrentNetworkUseCase

@OptIn(ExperimentalCoroutinesApi::class)
internal class MfaFactorInstanceViewModelTest : StateViewModelTest<MfaFactorInstanceViewModel>() {

    private val sargonOsManager = mockk<SargonOsManager>()
    private val sargonOs = mockk<SargonOs>()
    private val selectFactorSourceProxy = mockk<SelectFactorSourceProxy>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()
    private val getAddressBookEntriesOnCurrentNetworkUseCase = mockk<GetAddressBookEntriesOnCurrentNetworkUseCase>()

    private val profile = Profile.sample()
        .changeGateway(Gateway.forNetwork(NetworkId.MAINNET))
        .unHideAllEntities()

    override fun initVM(): MfaFactorInstanceViewModel = MfaFactorInstanceViewModel(
        sargonOsManager = sargonOsManager,
        selectFactorSourceProxy = selectFactorSourceProxy,
        getProfileUseCase = getProfileUseCase,
        getAddressBookEntriesOnCurrentNetworkUseCase = getAddressBookEntriesOnCurrentNetworkUseCase,
        defaultDispatcher = coroutineRule.dispatcher
    )

    @Before
    override fun setUp() {
        super.setUp()

        every { sargonOsManager.sargonOs } returns sargonOs
        coEvery { sargonOs.usedMfaSignatureResourcesWithAccountsCurrentNetwork() } returns emptyList()
        coEvery { getProfileUseCase() } returns profile
        coEvery { getAddressBookEntriesOnCurrentNetworkUseCase() } returns emptyList()
        coEvery { selectFactorSourceProxy.selectFactorSource(any()) } returns null
    }

    @Test
    fun `load current usage maps profile account, address book name and factor source`() = runTest {
        val knownAccount = profile.activeAccountsOnCurrentNetwork.first()
        val knownAddress = knownAccount.address
        val unknownAddress = AccountAddress.sampleRandom(NetworkId.MAINNET)
        val factorSource = profile.factorSources.first()
        val signatureResource = NonFungibleGlobalId.sample()

        val factorInstance = mockk<FactorInstance>()
        every { factorInstance.factorSourceId } returns factorSource.id
        val mfaFactorInstance = mockk<MfaFactorInstance>()
        every { mfaFactorInstance.factorInstance } returns factorInstance

        val usedResource = mockk<UsedMfaSignatureResourceWithAccounts>()
        every { usedResource.nonFungibleGlobalId } returns signatureResource
        every { usedResource.accountAddresses } returns listOf(knownAddress, unknownAddress)
        every { usedResource.mfaFactorInstance } returns mfaFactorInstance

        val addressBookEntry = mockk<AddressBookEntry>()
        every { addressBookEntry.address } returns unknownAddress
        every { addressBookEntry.name } returns DisplayName("Address Book")

        coEvery { sargonOs.usedMfaSignatureResourcesWithAccountsCurrentNetwork() } returns listOf(usedResource)
        coEvery { getAddressBookEntriesOnCurrentNetworkUseCase() } returns listOf(addressBookEntry)

        val vm = vm.value
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(false, state.isLoadingCurrentUsage)
        assertEquals(1, state.activeUsages.size)

        val usage = state.activeUsages.first()
        assertEquals(signatureResource, usage.signatureResource)
        assertEquals(factorSource, usage.factorSource)
        assertEquals(2, usage.accounts.size)
        assertEquals(knownAccount, usage.accounts.first().profileAccount)
        assertNull(usage.accounts.first().addressBookName)
        assertNull(usage.accounts.last().profileAccount)
        assertEquals("Address Book", usage.accounts.last().addressBookName)
    }

    @Test
    fun `load current usage handles missing account address book and factor source`() = runTest {
        val unknownAddress = AccountAddress.sampleRandom(NetworkId.MAINNET)
        val signatureResource = NonFungibleGlobalId.sample()
        val unknownFactorSourceId = mockk<FactorSourceId>()

        val factorInstance = mockk<FactorInstance>()
        every { factorInstance.factorSourceId } returns unknownFactorSourceId
        val mfaFactorInstance = mockk<MfaFactorInstance>()
        every { mfaFactorInstance.factorInstance } returns factorInstance

        val usedResource = mockk<UsedMfaSignatureResourceWithAccounts>()
        every { usedResource.nonFungibleGlobalId } returns signatureResource
        every { usedResource.accountAddresses } returns listOf(unknownAddress)
        every { usedResource.mfaFactorInstance } returns mfaFactorInstance

        coEvery { sargonOs.usedMfaSignatureResourcesWithAccountsCurrentNetwork() } returns listOf(usedResource)

        val vm = vm.value
        advanceUntilIdle()

        val usage = vm.state.value.activeUsages.first()
        assertNull(usage.factorSource)
        assertNull(usage.accounts.first().profileAccount)
        assertNull(usage.accounts.first().addressBookName)
    }

    @Test
    fun `on get new instance click emits address details event on success`() = runTest {
        val factorSource = profile.factorSources.first()
        val selectedFactorSourceId = SelectFactorSourceOutput.Id(factorSource.id)
        val mfaFactorInstance = mockk<MfaFactorInstance>()
        val signatureResource = NonFungibleGlobalId.sample()

        coEvery {
            selectFactorSourceProxy.selectFactorSource(SelectFactorSourceInput.Context.MfaFactorInstance)
        } returns selectedFactorSourceId
        coEvery { sargonOs.getNewMfaFactorInstance(factorSource) } returns mfaFactorInstance

        mockkStatic("com.radixdlt.sargon.extensions.MfaFactorInstanceKt")
        try {
            every { mfaFactorInstance.nonFungibleGlobalId() } returns signatureResource

            val vm = vm.value
            advanceUntilIdle()

            vm.onGetNewInstanceClick()
            advanceUntilIdle()

            vm.oneOffEvent.test {
                val event = awaitItem()
                assertEquals(
                    MfaFactorInstanceViewModel.Event.ShowAddressDetails(
                        ActionableAddress.GlobalId(
                            address = signatureResource,
                            isVisitableInDashboard = true,
                            isOnlyLocalIdVisible = false
                        )
                    ),
                    event
                )
            }
        } finally {
            unmockkStatic("com.radixdlt.sargon.extensions.MfaFactorInstanceKt")
        }
    }

    @Test
    fun `on factor source click emits factor source details event`() = runTest {
        val factorSourceId = profile.factorSources.first().id
        val vm = vm.value
        advanceUntilIdle()

        vm.onFactorSourceClick(factorSourceId)
        advanceUntilIdle()

        vm.oneOffEvent.test {
            val event = awaitItem()
            assertEquals(
                MfaFactorInstanceViewModel.Event.ShowFactorSourceDetails(factorSourceId),
                event
            )
        }
    }

    @Test
    fun `on get new instance failure sets error message`() = runTest {
        val factorSource = profile.factorSources.first()

        coEvery {
            selectFactorSourceProxy.selectFactorSource(SelectFactorSourceInput.Context.MfaFactorInstance)
        } returns SelectFactorSourceOutput.Id(factorSource.id)
        coEvery { sargonOs.getNewMfaFactorInstance(factorSource) } throws IllegalStateException("boom")

        val vm = vm.value
        advanceUntilIdle()

        vm.onGetNewInstanceClick()
        advanceUntilIdle()

        assertTrue(vm.state.value.uiMessage is UiMessage.ErrorMessage)
    }

    @Test
    fun `load current usage failure sets error message and stops loading`() = runTest {
        coEvery { sargonOs.usedMfaSignatureResourcesWithAccountsCurrentNetwork() } throws IllegalStateException("boom")

        val vm = vm.value
        advanceUntilIdle()

        assertEquals(false, vm.state.value.isLoadingCurrentUsage)
        assertTrue(vm.state.value.uiMessage is UiMessage.ErrorMessage)
        assertTrue(vm.state.value.activeUsages.isEmpty())
    }
}
