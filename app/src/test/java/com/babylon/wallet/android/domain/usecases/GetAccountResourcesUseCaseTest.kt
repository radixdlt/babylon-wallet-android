package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleIdType
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.RawJson
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.ScryptoSborValue
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDetailsResponseItem
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleMetadataContainer
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.NonFungibleTokenItemContainer
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class GetAccountResourcesUseCaseTest {

    private val entityRepositoryMock = mockk<EntityRepository>()
    private val getProfileUseCaseMock = mockk<GetProfileUseCase>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val mnemonicRepository = mockk<MnemonicRepository>()

    private val testedClass = GetAccountResourcesUseCase(
        entityRepository = entityRepositoryMock,
        getProfileUseCase = getProfileUseCaseMock,
        preferencesManager = preferencesManager,
        mnemonicRepository = mnemonicRepository
    )

    @Before
    fun setUp() {
        coEvery { preferencesManager.getBackedUpFactorSourceIds() } returns flow { emit(emptySet()) }
        coEvery { mnemonicRepository.readMnemonic(any()) } returns null
    }

    @Test
    fun `account in profile with no resources`() = runTest {
        val expectedProfileAccountAddress = "1234"
        val expectedProfileAccountName = "account1"
        val expectedProfile = profile(accounts = listOf(
            account(
                address = expectedProfileAccountAddress,
                name = expectedProfileAccountName
            )
        ))
        every { getProfileUseCaseMock.invoke() } returns flowOf(expectedProfile)

        coEvery {
            entityRepositoryMock.stateEntityDetails(addresses = listOf(expectedProfileAccountAddress), isRefreshing = true)
        } returns Result.Success(
            expectedAccount(accountAddress = expectedProfileAccountAddress)
        )

        val accountResourcesResult = testedClass.getAccountsFromProfile(isRefreshing = true)

        Assert.assertEquals(
            listOf(
                AccountResources(
                    address = expectedProfileAccountAddress,
                    displayName = expectedProfileAccountName,
                    appearanceID = 1,
                    fungibleTokens = persistentListOf(),
                    nonFungibleTokens = persistentListOf(),
                )
            ),
            accountResourcesResult.value()
        )
    }

    @Test
    fun `account in profile with a fungible resource`() = runTest {
        val expectedProfileAccountAddress = "account_rdx_1234"
        val expectedProfileAccountName = "account1"
        val expectedResource = FungibleResourcesCollectionItemGloballyAggregated(
            aggregationLevel = ResourceAggregationLevel.global,
            resourceAddress = "resource_rdx_5678",
            amount = "1000",
            lastUpdatedAtStateVersion = 0L
        )
        val expectedProfile = profile(
            accounts = listOf(account(
                address = expectedProfileAccountAddress,
                name = expectedProfileAccountName
            ))
        )
        every { getProfileUseCaseMock() } returns flowOf(expectedProfile)
        coEvery {
            entityRepositoryMock.stateEntityDetails(addresses = listOf(expectedProfileAccountAddress), isRefreshing = true)
        } returns Result.Success(
            expectedAccount(expectedProfileAccountAddress, expectedResource)
        )
        coEvery {
            entityRepositoryMock.stateEntityDetails(addresses = listOf(expectedResource.resourceAddress), isRefreshing = true)
        } returns Result.Success(
            expectedFungibleResource(expectedResource.resourceAddress)
        )

        val accountResourcesResult = testedClass.getAccountsFromProfile(isRefreshing = true)

        Assert.assertEquals(
            listOf(
                AccountResources(
                    address = expectedProfileAccountAddress,
                    displayName = expectedProfileAccountName,
                    appearanceID = 1,
                    fungibleTokens = persistentListOf(
                        OwnedFungibleToken(
                            owner = AccountAddress(expectedProfileAccountAddress),
                            amount = BigDecimal("1000"),
                            address = expectedResource.resourceAddress,
                            token = FungibleToken(address = expectedResource.resourceAddress)
                        )
                    ),
                    nonFungibleTokens = persistentListOf(),
                )
            ),
            accountResourcesResult.value()
        )
    }

    @Test
    fun `account in profile with a non fungible resource`() = runTest {
        val expectedProfileAccountAddress = "account_rdx_1234"
        val expectedProfileAccountName = "account1"
        val expectedResource = NonFungibleResourcesCollectionItemGloballyAggregated(
            aggregationLevel = ResourceAggregationLevel.global,
            resourceAddress = "resource_rdx_5678",
            amount = 10L,
            lastUpdatedAtStateVersion = 0L
        )
        val expectedNonFungibleIdContainer = NonFungibleTokenIdContainer(
            ids = listOf("id_container")
        )
        val expectedProfile = profile(accounts = listOf(
            account(address = expectedProfileAccountAddress, name = expectedProfileAccountName))
        )
        every { getProfileUseCaseMock() } returns flowOf(expectedProfile)
        coEvery {
            entityRepositoryMock.stateEntityDetails(addresses = listOf(expectedProfileAccountAddress), isRefreshing = true)
        } returns Result.Success(
            expectedAccount(expectedProfileAccountAddress, nonFungibleResource = expectedResource)
        )
        coEvery {
            entityRepositoryMock.stateEntityDetails(addresses = listOf(expectedResource.resourceAddress), isRefreshing = true)
        } returns Result.Success(
            expectedNonFungibleResource(expectedResource.resourceAddress)
        )
        coEvery {
            entityRepositoryMock.getNonFungibleIds(address = expectedResource.resourceAddress, isRefreshing = true)
        } returns Result.Success(
            expectedNonFungibleIdContainer
        )

        coEvery {
            entityRepositoryMock.nonFungibleData(address = expectedResource.resourceAddress, any(), isRefreshing = true)
        } returns Result.Success(
            expectedNonFungibleData()
        )

        val accountResourcesResult = testedClass.getAccountsFromProfile(isRefreshing = true)

        Assert.assertEquals(
            listOf(
                AccountResources(
                    address = expectedProfileAccountAddress,
                    displayName = expectedProfileAccountName,
                    appearanceID = 1,
                    fungibleTokens = persistentListOf(),
                    nonFungibleTokens = persistentListOf(
                        OwnedNonFungibleToken(
                            owner = AccountAddress(expectedProfileAccountAddress),
                            amount = 10,
                            tokenResourceAddress = expectedResource.resourceAddress,
                            token = NonFungibleToken(
                                address = expectedResource.resourceAddress,
                                nfts = listOf(
                                    NonFungibleTokenItemContainer(id = "1", nftImage = "")
                                ),
                                metadataContainer = NonFungibleMetadataContainer()
                            )
                        )
                    ),
                )
            ),
            accountResourcesResult.value()
        )
    }

    private fun expectedAccount(
        accountAddress: String,
        fungibleResource: FungibleResourcesCollectionItemGloballyAggregated? = null,
        nonFungibleResource: NonFungibleResourcesCollectionItemGloballyAggregated? = null
    ) = StateEntityDetailsResponse(
            ledgerState = LedgerState(
                network = Radix.Network.hammunet.name,
                stateVersion = 0,
                proposerRoundTimestamp = "0",
                epoch = 0L,
                round = 0L
            ),
            items = listOf(
                StateEntityDetailsResponseItem(
                    address = accountAddress,
                    metadata = EntityMetadataCollection(items = listOf()),
                    fungibleResources = fungibleResource?.let {
                        FungibleResourcesCollection(items = listOf(it), totalCount = 1)
                    },
                    nonFungibleResources = nonFungibleResource?.let {
                        NonFungibleResourcesCollection(items = listOf(it), totalCount = 1)
                    }
                )
            )
        )

    private fun expectedFungibleResource(
        resourceAddress: String
    ) = StateEntityDetailsResponse(
        ledgerState = LedgerState(
            network = Radix.Network.hammunet.name,
            stateVersion = 0,
            proposerRoundTimestamp = "0",
            epoch = 0L,
            round = 0L
        ),
        items = listOf(
            StateEntityDetailsResponseItem(
                address = resourceAddress,
                metadata = EntityMetadataCollection(items = listOf())
            )
        )
    )

    private fun expectedNonFungibleResource(
        resourceAddress: String
    ) = StateEntityDetailsResponse(
        ledgerState = LedgerState(
            network = Radix.Network.hammunet.name,
            stateVersion = 0,
            proposerRoundTimestamp = "0",
            epoch = 0L,
            round = 0L
        ),
        items = listOf(
            StateEntityDetailsResponseItem(
                address = resourceAddress,
                metadata = EntityMetadataCollection(items = listOf())
            )
        )
    )

    private fun expectedNonFungibleData() = StateNonFungibleDataResponse(
        ledgerState = LedgerState(
            network = Radix.Network.hammunet.name,
            stateVersion = 0,
            proposerRoundTimestamp = "0",
            epoch = 0L,
            round = 0L
        ),
        resourceAddress = "",
        nonFungibleIdType = NonFungibleIdType.string,
        nonFungibleIds = listOf(
            StateNonFungibleDetailsResponseItem(
                nonFungibleId = "1",
                mutableData = ScryptoSborValue(
                    "",
                    RawJson(
                        elements = listOf(),
                        type = ""
                    )
                ),
                lastUpdatedAtStateVersion = 0L
            )
        )
    )
}
