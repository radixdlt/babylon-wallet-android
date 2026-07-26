package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.gateway.model.AccessRule
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.presentation.transfer.BadgeRequirementStatus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sampleMainnet
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import com.radixdlt.sargon.NonFungibleLocalId
import rdx.works.core.domain.assets.NonFungibleCollection

class GetWithdrawerBadgeRequirementsUseCaseTest {

    private val stateRepository = mockk<StateRepository>()
    private val getWalletAssetsUseCase = mockk<GetWalletAssetsUseCase>()
    private val useCase = GetWithdrawerBadgeRequirementsUseCase(
        stateRepository = stateRepository,
        getWalletAssetsUseCase = getWalletAssetsUseCase
    )

    private val fromAccount = Account.sampleMainnet()
    private val resourceAddress = ResourceAddress.sampleMainnet.xrd
    private val badgeAddress = ResourceAddress.sampleMainnet.candy

    @Test
    fun `given no badge is required when assets are checked then returns None`() = runTest {
        coEvery {
            stateRepository.getResourcesWithWithdrawerRules(setOf(resourceAddress))
        } returns Result.success(mapOf(resourceAddress to AccessRule(AccessRule.Type.AllowAll)))

        val result = useCase(fromAccount, setOf(resourceAddress)).getOrThrow()

        assertEquals(BadgeRequirementStatus.None, result.status)
        assertTrue(result.badges.isEmpty())
    }

    @Test
    fun `given badge is required and owned when assets are checked then returns Success`() = runTest {
        val rule = AccessRule(
            type = AccessRule.Type.Protected,
            accessRule = AccessRule.AccessRuleDetail(
                type = "ProofRule",
                proofRule = AccessRule.ProofRule(
                    type = "Require",
                    requirement = AccessRule.Requirement(
                        type = "Resource",
                        resource = badgeAddress.string
                    )
                )
            )
        )
        coEvery {
            stateRepository.getResourcesWithWithdrawerRules(setOf(resourceAddress))
        } returns Result.success(mapOf(resourceAddress to rule))

        val badgeToken = Token(
            resource = Resource.FungibleResource(
                address = badgeAddress,
                ownedAmount = Decimal192.init("1.5")
            )
        )
        coEvery {
            stateRepository.getResources(
                addresses = setOf(badgeAddress),
                underAccountAddress = fromAccount.address,
                withDetails = true,
                withAllMetadata = false
            )
        } returns Result.success(listOf(badgeToken.resource))

        val accountWithAssets = AccountWithAssets(
            account = fromAccount,
            assets = Assets(tokens = listOf(badgeToken))
        )
        coEvery { getWalletAssetsUseCase.collect(fromAccount, false) } returns Result.success(accountWithAssets)

        val result = useCase(fromAccount, setOf(resourceAddress)).getOrThrow()

        assertEquals(BadgeRequirementStatus.Success(badgeToken.resource), result.status)
        assertEquals(1, result.badges.size)
        val badge = result.badges.first() as ResourceSpecifier.Fungible
        assertEquals(badgeAddress, badge.resourceAddress)
        assertEquals(Decimal192.init("1"), badge.amount)
    }

    @Test
    fun `given badge is required and not owned when assets are checked then returns Error with MissingBadge`() = runTest {
        val rule = AccessRule(
            type = AccessRule.Type.Protected,
            accessRule = AccessRule.AccessRuleDetail(
                type = "ProofRule",
                proofRule = AccessRule.ProofRule(
                    type = "Require",
                    requirement = AccessRule.Requirement(
                        type = "Resource",
                        resource = badgeAddress.string
                    )
                )
            )
        )
        coEvery {
            stateRepository.getResourcesWithWithdrawerRules(setOf(resourceAddress))
        } returns Result.success(mapOf(resourceAddress to rule))

        val badgeResource = Resource.FungibleResource(
            address = badgeAddress,
            ownedAmount = Decimal192.init("0")
        )
        coEvery {
            stateRepository.getResources(
                addresses = setOf(badgeAddress),
                underAccountAddress = fromAccount.address,
                withDetails = true,
                withAllMetadata = false
            )
        } returns Result.success(listOf(badgeResource))

        val accountWithAssets = AccountWithAssets(
            account = fromAccount,
            assets = Assets(tokens = emptyList())
        )
        coEvery { getWalletAssetsUseCase.collect(fromAccount, false) } returns Result.success(accountWithAssets)

        val result = useCase(fromAccount, setOf(resourceAddress)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(
                resource = badgeResource,
                reason = BadgeRequirementStatus.Error.Reason.MissingBadge
            ),
            result.status
        )
        assertTrue(result.badges.isEmpty())
    }

    @Test
    fun `given badge is required and owned with 0 balance when assets are checked then returns Error with InsufficientBalance`() = runTest {
        val rule = AccessRule(
            type = AccessRule.Type.Protected,
            accessRule = AccessRule.AccessRuleDetail(
                type = "ProofRule",
                proofRule = AccessRule.ProofRule(
                    type = "Require",
                    requirement = AccessRule.Requirement(
                        type = "Resource",
                        resource = badgeAddress.string
                    )
                )
            )
        )
        coEvery {
            stateRepository.getResourcesWithWithdrawerRules(setOf(resourceAddress))
        } returns Result.success(mapOf(resourceAddress to rule))

        val badgeToken = Token(
            resource = Resource.FungibleResource(
                address = badgeAddress,
                ownedAmount = Decimal192.init("0")
            )
        )
        coEvery {
            stateRepository.getResources(
                addresses = setOf(badgeAddress),
                underAccountAddress = fromAccount.address,
                withDetails = true,
                withAllMetadata = false
            )
        } returns Result.success(listOf(badgeToken.resource))

        val accountWithAssets = AccountWithAssets(
            account = fromAccount,
            assets = Assets(tokens = listOf(badgeToken))
        )
        coEvery { getWalletAssetsUseCase.collect(fromAccount, false) } returns Result.success(accountWithAssets)

        val result = useCase(fromAccount, setOf(resourceAddress)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(
                resource = badgeToken.resource,
                reason = BadgeRequirementStatus.Error.Reason.InsufficientBalance
            ),
            result.status
        )
        assertTrue(result.badges.isEmpty())
    }

    @Test
    fun `given non-fungible badge is required (generic, without localId) and owned when assets are checked then returns Success`() = runTest {
        val rule = AccessRule(
            type = AccessRule.Type.Protected,
            accessRule = AccessRule.AccessRuleDetail(
                type = "ProofRule",
                proofRule = AccessRule.ProofRule(
                    type = "Require",
                    requirement = AccessRule.Requirement(
                        type = "Resource",
                        resource = badgeAddress.string
                    )
                )
            )
        )
        coEvery {
            stateRepository.getResourcesWithWithdrawerRules(setOf(resourceAddress))
        } returns Result.success(mapOf(resourceAddress to rule))

        val localId = NonFungibleLocalId.init("<id_1>")
        val badgeResource = Resource.NonFungibleResource(
            address = badgeAddress,
            amount = 1,
            items = listOf(
                Resource.NonFungibleResource.Item(
                    collectionAddress = badgeAddress,
                    localId = localId
                )
            )
        )
        coEvery {
            stateRepository.getResources(
                addresses = setOf(badgeAddress),
                underAccountAddress = fromAccount.address,
                withDetails = true,
                withAllMetadata = false
            )
        } returns Result.success(listOf(badgeResource))

        val ownedCollection = NonFungibleCollection(collection = badgeResource)
        val accountWithAssets = AccountWithAssets(
            account = fromAccount,
            assets = Assets(nonFungibles = listOf(ownedCollection))
        )
        coEvery { getWalletAssetsUseCase.collect(fromAccount, false) } returns Result.success(accountWithAssets)

        val result = useCase(fromAccount, setOf(resourceAddress)).getOrThrow()

        assertEquals(BadgeRequirementStatus.Success(badgeResource), result.status)
        assertEquals(1, result.badges.size)
        val badge = result.badges.first() as ResourceSpecifier.NonFungible
        assertEquals(badgeAddress, badge.resourceAddress)
        assertEquals(listOf(localId), badge.ids)
    }

    @Test
    fun `given non-fungible badge with specific localId is required and owned when assets are checked then returns Success`() = runTest {
        val targetLocalIdStr = "<target_id>"
        val targetLocalId = NonFungibleLocalId.init(targetLocalIdStr)
        val rule = AccessRule(
            type = AccessRule.Type.Protected,
            accessRule = AccessRule.AccessRuleDetail(
                type = "ProofRule",
                proofRule = AccessRule.ProofRule(
                    type = "Require",
                    requirement = AccessRule.Requirement(
                        type = "NonFungible",
                        nonFungible = AccessRule.NonFungibleRequirement(
                            resourceAddress = badgeAddress.string,
                            localId = AccessRule.LocalIdRequirement(
                                simpleRep = targetLocalIdStr
                            )
                        )
                    )
                )
            )
        )
        coEvery {
            stateRepository.getResourcesWithWithdrawerRules(setOf(resourceAddress))
        } returns Result.success(mapOf(resourceAddress to rule))

        val badgeResource = Resource.NonFungibleResource(
            address = badgeAddress,
            amount = 1,
            items = listOf(
                Resource.NonFungibleResource.Item(
                    collectionAddress = badgeAddress,
                    localId = targetLocalId
                )
            )
        )
        coEvery {
            stateRepository.getResources(
                addresses = setOf(badgeAddress),
                underAccountAddress = fromAccount.address,
                withDetails = true,
                withAllMetadata = false
            )
        } returns Result.success(listOf(badgeResource))

        val ownedCollection = NonFungibleCollection(collection = badgeResource)
        val accountWithAssets = AccountWithAssets(
            account = fromAccount,
            assets = Assets(nonFungibles = listOf(ownedCollection))
        )
        coEvery { getWalletAssetsUseCase.collect(fromAccount, false) } returns Result.success(accountWithAssets)

        val result = useCase(fromAccount, setOf(resourceAddress)).getOrThrow()

        assertEquals(BadgeRequirementStatus.Success(badgeResource), result.status)
        assertEquals(1, result.badges.size)
        val badge = result.badges.first() as ResourceSpecifier.NonFungible
        assertEquals(badgeAddress, badge.resourceAddress)
        assertEquals(listOf(targetLocalId), badge.ids)
    }

    @Test
    fun `given non-fungible badge is required but not owned when assets are checked then returns Error with MissingBadge`() = runTest {
        val targetLocalIdStr = "<target_id>"
        val rule = AccessRule(
            type = AccessRule.Type.Protected,
            accessRule = AccessRule.AccessRuleDetail(
                type = "ProofRule",
                proofRule = AccessRule.ProofRule(
                    type = "Require",
                    requirement = AccessRule.Requirement(
                        type = "NonFungible",
                        nonFungible = AccessRule.NonFungibleRequirement(
                            resourceAddress = badgeAddress.string,
                            localId = AccessRule.LocalIdRequirement(
                                simpleRep = targetLocalIdStr
                            )
                        )
                    )
                )
            )
        )
        coEvery {
            stateRepository.getResourcesWithWithdrawerRules(setOf(resourceAddress))
        } returns Result.success(mapOf(resourceAddress to rule))

        val badgeResource = Resource.NonFungibleResource(
            address = badgeAddress,
            amount = 0,
            items = emptyList()
        )
        coEvery {
            stateRepository.getResources(
                addresses = setOf(badgeAddress),
                underAccountAddress = fromAccount.address,
                withDetails = true,
                withAllMetadata = false
            )
        } returns Result.success(listOf(badgeResource))

        val accountWithAssets = AccountWithAssets(
            account = fromAccount,
            assets = Assets(nonFungibles = emptyList())
        )
        coEvery { getWalletAssetsUseCase.collect(fromAccount, false) } returns Result.success(accountWithAssets)

        val result = useCase(fromAccount, setOf(resourceAddress)).getOrThrow()

        assertEquals(
            BadgeRequirementStatus.Error(
                resource = badgeResource,
                reason = BadgeRequirementStatus.Error.Reason.MissingBadge
            ),
            result.status
        )
        assertTrue(result.badges.isEmpty())
    }
}

