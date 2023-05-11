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
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.NonFungibleTokenIdContainer
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.mockdata.account
import com.babylon.wallet.android.mockdata.profile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.SecurityState
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.domain.GetProfileUseCase
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class GetAccountsWithResourcesUseCaseTest {

    private val entityRepositoryMock = mockk<EntityRepository>()
    private val getProfileUseCaseMock = mockk<GetProfileUseCase>()
    private val getFactorSourceStateForAccountUseCase = mockk<GetFactorSourceStateForAccountUseCase>()

    private val testedClass = GetAccountsWithResourcesUseCase(
        entityRepository = entityRepositoryMock,
        getProfileUseCase = getProfileUseCaseMock,
        getFactorSourceStateForAccountUseCase = getFactorSourceStateForAccountUseCase
    )

    private val securityState = SecurityState.Unsecured(
        unsecuredEntityControl = SecurityState.UnsecuredEntityControl(
            genesisFactorInstance = FactorInstance(
                derivationPath = DerivationPath.forIdentity(
                    networkId = Radix.Gateway.default.network.networkId(),
                    identityIndex = 0,
                    keyType = KeyType.TRANSACTION_SIGNING
                ),
                factorSourceId = FactorSource.ID("IDIDDIIDD"),
                publicKey = FactorInstance.PublicKey.curve25519PublicKey("")
            )
        )
    )

    @Before
    fun setUp() {
        coEvery { getFactorSourceStateForAccountUseCase.invoke(any()) } returns AccountWithResources.FactorSourceState.Valid
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
                AccountWithResources(
                    account = Network.Account(
                        address = expectedProfileAccountAddress,
                        displayName = expectedProfileAccountName,
                        appearanceID = 1,
                        networkID = 1,
                        securityState = securityState
                    ),
                    resources = Resources(
                        fungibleResources = persistentListOf(),
                        nonFungibleResources = persistentListOf(),
                    )
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
                AccountWithResources(
                    account = Network.Account(
                        address = expectedProfileAccountAddress,
                        displayName = expectedProfileAccountName,
                        appearanceID = 1,
                        networkID = Radix.Gateway.default.network.networkId().value,
                        securityState = securityState
                    ),
                    resources = Resources(
                        fungibleResources = persistentListOf(
                            Resource.FungibleResource(
                                amount = BigDecimal("1000"),
                                resourceAddress = expectedResource.resourceAddress,
                            )
                        ),
                        nonFungibleResources = persistentListOf()
                    )
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
                AccountWithResources(
                    account = Network.Account(
                        address = expectedProfileAccountAddress,
                        displayName = expectedProfileAccountName,
                        appearanceID = 1,
                        networkID = Radix.Gateway.default.network.networkId().value,
                        securityState = securityState
                    ),
                    resources = Resources(
                        fungibleResources = persistentListOf(),
                        nonFungibleResources = persistentListOf(
                            Resource.NonFungibleResource(
                                resourceAddress = expectedResource.resourceAddress,
                                amount = 10,
                                nftIds = listOf("1")
                            )
                        )
                    )
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
                data = ScryptoSborValue(
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
