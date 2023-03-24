package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.generated.models.EntityMetadataCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.FungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.LedgerState
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollection
import com.babylon.wallet.android.data.gateway.generated.models.NonFungibleResourcesCollectionItemGloballyAggregated
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.nonfungible.NonFungibleRepository
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleMetadataContainer
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.repository.AccountRepository
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class GetAccountResourcesUseCaseTest {

    private val entityRepositoryMock = mockk<EntityRepository>()
    private val nonFungibleRepositoryMock = mockk<NonFungibleRepository>()
    private val accountRepositoryMock = mockk<AccountRepository>()

    private val testedClass = GetAccountResourcesUseCase(
        entityRepository = entityRepositoryMock,
        nonFungibleRepository = nonFungibleRepositoryMock,
        accountRepository = accountRepositoryMock
    )

    @Test
    fun `account in profile with no resources`() = runTest {
        val expectedProfileAccountAddress = "1234"
        val expectedProfileAccount = SampleDataProvider().sampleAccount(address = expectedProfileAccountAddress)
        coEvery { accountRepositoryMock.getAccounts() } returns listOf(expectedProfileAccount)

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
                    displayName = expectedProfileAccount.displayName,
                    appearanceID = expectedProfileAccount.appearanceID,
                    fungibleTokens = listOf(),
                    nonFungibleTokens = listOf(),
                )
            ),
            accountResourcesResult.value()
        )
    }

    @Test
    fun `account in profile with a fungible resource`() = runTest {
        val expectedProfileAccountAddress = "account_rdx_1234"
        val expectedResource = FungibleResourcesCollectionItemGloballyAggregated(
            aggregationLevel = ResourceAggregationLevel.global,
            resourceAddress = "resource_rdx_5678",
            amount = "1000",
            lastUpdatedAtStateVersion = 0L
        )
        val expectedProfileAccount = SampleDataProvider().sampleAccount(address = expectedProfileAccountAddress)
        coEvery { accountRepositoryMock.getAccounts() } returns listOf(expectedProfileAccount)
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
                    displayName = expectedProfileAccount.displayName,
                    appearanceID = expectedProfileAccount.appearanceID,
                    fungibleTokens = listOf(
                        OwnedFungibleToken(
                            owner = AccountAddress(expectedProfileAccountAddress),
                            amount = BigDecimal("1000"),
                            address = expectedResource.resourceAddress,
                            token = FungibleToken(address = expectedResource.resourceAddress)
                        )
                    ),
                    nonFungibleTokens = listOf(),
                )
            ),
            accountResourcesResult.value()
        )
    }

    @Test
    fun `account in profile with a non fungible resource`() = runTest {
        val expectedProfileAccountAddress = "account_rdx_1234"
        val expectedResource = NonFungibleResourcesCollectionItemGloballyAggregated(
            aggregationLevel = ResourceAggregationLevel.global,
            resourceAddress = "resource_rdx_5678",
            amount = 10L,
            lastUpdatedAtStateVersion = 0L
        )
        val expectedNonFungibleIdContainer = NonFungibleTokenIdContainer(
            ids = listOf("id_container")
        )
        val expectedProfileAccount = SampleDataProvider().sampleAccount(address = expectedProfileAccountAddress)
        coEvery { accountRepositoryMock.getAccounts() } returns listOf(expectedProfileAccount)
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
            nonFungibleRepositoryMock.nonFungibleIds(address = expectedResource.resourceAddress, isRefreshing = true)
        } returns Result.Success(
            expectedNonFungibleIdContainer
        )

        val accountResourcesResult = testedClass.getAccountsFromProfile(isRefreshing = true)

        Assert.assertEquals(
            listOf(
                AccountResources(
                    address = expectedProfileAccountAddress,
                    displayName = expectedProfileAccount.displayName,
                    appearanceID = expectedProfileAccount.appearanceID,
                    fungibleTokens = listOf(),
                    nonFungibleTokens = listOf(
                        OwnedNonFungibleToken(
                            owner = AccountAddress(expectedProfileAccountAddress),
                            amount = 10,
                            tokenResourceAddress = expectedResource.resourceAddress,
                            token = NonFungibleToken(
                                address = expectedResource.resourceAddress,
                                nonFungibleIdContainer = NonFungibleTokenIdContainer(
                                    ids = expectedNonFungibleIdContainer.ids
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
}
