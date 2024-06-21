@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.status.address

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.VerifyAddressOnLedgerUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetPoolsUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetValidatorsUseCase
import com.babylon.wallet.android.fakes.FakeProfileRepository
import com.babylon.wallet.android.presentation.StateViewModelTest
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleRandom
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator
import rdx.works.core.domain.resources.sampleMainnet
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.addAccounts
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.initBabylon
import rdx.works.core.sargon.isLedgerAccount
import rdx.works.core.sargon.sample
import rdx.works.profile.domain.GetProfileUseCase

class AddressDetailsDialogViewModelTest : StateViewModelTest<AddressDetailsDialogViewModel>() {

    private val savedStateHandle = mockk<SavedStateHandle>()
    private val profileRepository = FakeProfileRepository()
    private val getProfileUseCase = GetProfileUseCase(profileRepository = profileRepository)
    private val verifyAddressOnLedgerUseCase = mockk<VerifyAddressOnLedgerUseCase>()
    private val getResourcesUseCase = mockk<GetResourcesUseCase>()
    private val getPoolsUseCase = mockk<GetPoolsUseCase>()
    private val getValidatorsUseCase = mockk<GetValidatorsUseCase>()

    override fun initVM(): AddressDetailsDialogViewModel = AddressDetailsDialogViewModel(
        savedStateHandle = savedStateHandle,
        getProfileUseCase = getProfileUseCase,
        verifyAddressOnLedgerUseCase = verifyAddressOnLedgerUseCase,
        getResourcesUseCase = getResourcesUseCase,
        getPoolsUseCase = getPoolsUseCase,
        getValidatorsUseCase = getValidatorsUseCase
    )

    @Test
    fun `given account address, verify that basic sections are added`() = runTest {
        val address = AccountAddress.sampleMainnet()
        provideInput(Address.Account(address))
        val vm = vm.value
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(
            listOf(
                AddressDetailsDialogViewModel.State.Section.AccountAddressQRCode(accountAddress = address),
                AddressDetailsDialogViewModel.State.Section.FullAddress(
                    rawAddress = address.formatted(AddressFormat.RAW),
                    truncatedPart = address.formatted(AddressFormat.MIDDLE)
                ),
                AddressDetailsDialogViewModel.State.Section.VisitDashboard(
                    url = "https://dashboard.radixdlt.com/account/${address.formatted(AddressFormat.RAW)}"
                )
            ),
            state.sections
        )
    }

    @Test
    fun `given account address created with Ledger, verify that verify on ledger section is shown`() = runTest {
        val profile = Profile.sampleWithLedgerAccount().also {
            profileRepository.saveProfile(it)
        }

        val account = profile.activeAccountsOnCurrentNetwork.find { it.isLedgerAccount } ?: error("Expected ledger account but none found")
        provideInput(Address.Account(account.address))
        val vm = vm.value
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(
            AddressDetailsDialogViewModel.State.Section.VerifyAddressOnLedger(accountAddress = account.address),
            state.sections.last()
        )
    }

    @Test
    fun `given owned account, resolve title with account's name`() = runTest {
        val profile = Profile.sample().changeGatewayToNetworkId(NetworkId.MAINNET)
        profileRepository.saveProfile(profile = profile)
        val account = profile.activeAccountsOnCurrentNetwork.first()
        provideInput(Address.Account(account.address))

        val vm = vm.value
        advanceUntilIdle()
        assertEquals(
            account.displayName.value,
            vm.state.value.title
        )
    }

    @Test
    fun `given not owned account address, resolve no title`() = runTest {
        provideInput(Address.Account(AccountAddress.sampleRandom(NetworkId.MAINNET)))

        val vm = vm.value
        advanceUntilIdle()
        assertNull(vm.state.value.title)
    }

    @Test
    fun `given owned identity address, resolve title with persona's name`() = runTest {
        val profile = Profile.sample().changeGatewayToNetworkId(NetworkId.MAINNET)
        profileRepository.saveProfile(profile = profile)
        val persona = profile.activePersonasOnCurrentNetwork.first()
        provideInput(Address.Identity(persona.address))

        val vm = vm.value
        advanceUntilIdle()
        assertEquals(
            persona.displayName.value,
            vm.state.value.title
        )
    }

    @Test
    fun `given not owned identity address, resolve no title`() = runTest {
        provideInput(Address.Identity(IdentityAddress.sampleRandom(NetworkId.MAINNET)))

        val vm = vm.value
        advanceUntilIdle()
        assertNull(vm.state.value.title)
    }

    @Test
    fun `given fungible resource address, resolve title with name and symbol`() = runTest {
        val resource = Resource.FungibleResource.sampleMainnet()
        coEvery { getResourcesUseCase(setOf(resource.address)) } returns Result.success(
            listOf(
                resource
            )
        )
        provideInput(Address.Resource(resource.address))

        val vm = vm.value
        advanceUntilIdle()
        assertEquals(
            "${resource.name} (${resource.symbol})",
            vm.state.value.title
        )
    }

    @Test
    fun `given non fungible resource address, resolve title with name`() = runTest {
        val resource = Resource.NonFungibleResource.sampleMainnet()
        coEvery { getResourcesUseCase(setOf(resource.address)) } returns Result.success(
            listOf(
                resource
            )
        )
        provideInput(Address.Resource(resource.address))

        val vm = vm.value
        advanceUntilIdle()
        assertEquals(
            resource.name,
            vm.state.value.title
        )
    }

    @Test
    fun `given validator address, resolve title with validator's name`() = runTest {
        val validator = Validator.sampleMainnet()
        coEvery { getValidatorsUseCase(setOf(validator.address)) } returns Result.success(
            listOf(
                validator
            )
        )
        provideInput(Address.Validator(validator.address))

        val vm = vm.value
        advanceUntilIdle()
        assertEquals(
            validator.name,
            vm.state.value.title
        )
    }

    @Test
    fun `given address, when copy clicked, verify that copy event is sent`() = runTest {
        val address = AccountAddress.sampleRandom(NetworkId.MAINNET)
        provideInput(Address.Account(address))

        val vm = vm.value
        advanceUntilIdle()

        vm.onCopyClick()
        vm.oneOffEvent.test {
            val event = awaitItem()

            assertEquals(
                AddressDetailsDialogViewModel.Event.PerformCopy(
                    valueToCopy = address.formatted(AddressFormat.RAW)
                ),
                event
            )
        }
    }

    @Test
    fun `given address, when enlarge clicked, verify that enlarge event is sent`() = runTest {
        val addressString = "account_tdx_2_12xn3lgz7xv4d0d4cx25nvfekyxx0fsawhmtht0dd550vcu5wwl0g70"
        val address = AccountAddress.init(addressString)
        provideInput(Address.Account(address))

        val vm = vm.value
        advanceUntilIdle()

        vm.onEnlargeClick()
        vm.oneOffEvent.test {
            val event = awaitItem()
            assertEquals(
                AddressDetailsDialogViewModel.Event.PerformEnlarge(
                    value = addressString,
                    numberRanges = listOf(
                        12 until 13,
                        14 until 16,
                        18 until 19,
                        22 until 23,
                        25 until 26,
                        27 until 28,
                        29 until 30,
                        32 until 34,
                        42 until 43,
                        52 until 53,
                        55 until 58,
                        61 until 62,
                        65 until 66,
                        67 until 69
                    )
                ),
                event
            )
        }
    }

    @Test
    fun `when hide enlarge clicked, verify that close enlarge event is sent`() = runTest {
        provideInput(Address.Account(AccountAddress.sampleRandom(NetworkId.MAINNET)))

        val vm = vm.value
        advanceUntilIdle()

        vm.onHideEnlargeClick()
        vm.oneOffEvent.test {
            val event = awaitItem()
            assertEquals(
                AddressDetailsDialogViewModel.Event.CloseEnlarged,
                event
            )
        }
    }

    @Test
    fun `given account address, when share click, verify the share event is sent with correct title and value`() = runTest {
        val profile = Profile.sample().changeGatewayToNetworkId(NetworkId.MAINNET)
        profileRepository.saveProfile(profile = profile)
        val account = profile.activeAccountsOnCurrentNetwork.first()
        provideInput(Address.Account(account.address))

        val vm = vm.value
        advanceUntilIdle()

        vm.onShareClick()
        vm.oneOffEvent.test {
            val event = awaitItem()
            assertEquals(
                AddressDetailsDialogViewModel.Event.PerformShare(
                    shareTitle = account.displayName.value,
                    shareValue = account.address.formatted(AddressFormat.RAW)
                ),
                event
            )
        }
    }

    @Test
    fun `given account address, when visit on dashboard click, verify the dashboard event is sent with correct url`() = runTest {
        val addressString = "account_tdx_2_12xn3lgz7xv4d0d4cx25nvfekyxx0fsawhmtht0dd550vcu5wwl0g70"
        val address = AccountAddress.init(addressString)
        provideInput(Address.Account(address))

        val vm = vm.value
        advanceUntilIdle()

        vm.onVisitDashboardClick()
        vm.oneOffEvent.test {
            val event = awaitItem()
            assertEquals(
                AddressDetailsDialogViewModel.Event.PerformVisitDashBoard(
                    url = "https://stokenet-dashboard.radixdlt.com/account/$addressString",
                ),
                event
            )
        }
    }

    @Test
    fun `given account address created with ledger and address verifiable, when verify on ledger click, verify the success event is sent`() =
        runTest {
            val profile = Profile.sampleWithLedgerAccount().also {
                profileRepository.saveProfile(it)
            }
            val account =
                profile.activeAccountsOnCurrentNetwork.find { it.isLedgerAccount } ?: error("Expected Ledger account but none found")
            provideInput(Address.Account(account.address))

            coEvery { verifyAddressOnLedgerUseCase(address = account.address) } returns Result.success(Unit)

            val vm = vm.value
            advanceUntilIdle()

            vm.onVerifyOnLedgerDeviceClick()
            vm.oneOffEvent.test {
                val event = awaitItem()
                assertEquals(
                    AddressDetailsDialogViewModel.Event.ShowLedgerVerificationResult(isVerified = true),
                    event
                )
            }
        }

    @Test
    fun `given account address created with ledger and address not verifiable, when verify on ledger click, verify the error event is sent`() =
        runTest {
            val profile = Profile.sampleWithLedgerAccount().also {
                profileRepository.saveProfile(it)
            }
            val account =
                profile.activeAccountsOnCurrentNetwork.find { it.isLedgerAccount } ?: error("Expected Ledger account but none found")
            provideInput(Address.Account(account.address))

            coEvery { verifyAddressOnLedgerUseCase(address = account.address) } returns Result.failure(RuntimeException("An error"))

            val vm = vm.value
            advanceUntilIdle()

            vm.onVerifyOnLedgerDeviceClick()
            vm.oneOffEvent.test {
                val event = awaitItem()
                assertEquals(
                    AddressDetailsDialogViewModel.Event.ShowLedgerVerificationResult(isVerified = false),
                    event
                )
            }
        }

    private fun provideInput(address: Address) {
        val actionableAddress = ActionableAddress.Address(
            address = address,
            isVisitableInDashboard = true
        )
        every { savedStateHandle.get<String>(ARG_ACTIONABLE_ADDRESS) } returns Json.encodeToString<ActionableAddress>(actionableAddress)
    }

    private fun Profile.Companion.sampleWithLedgerAccount(): Profile = with(FactorSource.Ledger.sample()) {
        Profile.sample().let {
            it
                .changeGatewayToNetworkId(NetworkId.MAINNET)
                .copy(factorSources = it.factorSources.asIdentifiable().append(this).asList())
        }.let {
            val newAccount = Account.initBabylon(
                networkId = NetworkId.MAINNET,
                displayName = DisplayName("Ledger"),
                hdPublicKey = HierarchicalDeterministicPublicKey.sample(),
                factorSourceId = value.id.asGeneral(),
                customAppearanceId = AppearanceId(0u)
            )
            it.addAccounts(listOf(newAccount), NetworkId.MAINNET)
        }
    }

}
