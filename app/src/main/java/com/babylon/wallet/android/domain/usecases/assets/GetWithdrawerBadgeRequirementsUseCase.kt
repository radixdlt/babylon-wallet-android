package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.gateway.model.RequiredBadgeSpecification
import com.babylon.wallet.android.data.gateway.model.extractRequiredBadges
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.presentation.transfer.BadgeRequirementStatus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

class GetWithdrawerBadgeRequirementsUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase
) {
    suspend operator fun invoke(
        fromAccount: Account,
        spendingAssetsResourceAddresses: Set<ResourceAddress>
    ): Result<BadgeResult> {
        return runCatching {
            if (spendingAssetsResourceAddresses.isEmpty()) {
                return@runCatching BadgeResult(BadgeRequirementStatus.None, emptyList())
            }

            // 1. Fetch withdrawer rules for all assets being transferred
            val rulesMap = stateRepository.getResourcesWithWithdrawerRules(spendingAssetsResourceAddresses)
                .getOrThrow()

            // 2. Extract required badges
            val badgeSpecs = rulesMap.values.flatMap { it.extractRequiredBadges() }.distinct()

            if (badgeSpecs.isEmpty()) {
                return@runCatching BadgeResult(BadgeRequirementStatus.None, emptyList())
            }

            // Retrieve all resource addresses for the required badges
            val badgeAddresses = badgeSpecs.map { spec ->
                when (spec) {
                    is RequiredBadgeSpecification.Fungible -> spec.resourceAddress
                    is RequiredBadgeSpecification.NonFungible -> spec.resourceAddress
                }
            }.toSet()

            // Fetch resource details for all badge addresses
            val badgeResources = stateRepository.getResources(
                addresses = badgeAddresses,
                underAccountAddress = fromAccount.address,
                withDetails = true,
                withAllMetadata = false
            ).getOrNull()?.associateBy { it.address } ?: emptyMap()

            // 3. Fetch fromAccount's assets to match badges
            val accountWithAssets = getWalletAssetsUseCase.collect(account = fromAccount, isRefreshing = false)
                .getOrThrow()
            val assets = accountWithAssets.assets

            val resolvedSpecs = badgeSpecs.map { spec ->
                val resourceAddress = when (spec) {
                    is RequiredBadgeSpecification.Fungible -> spec.resourceAddress
                    is RequiredBadgeSpecification.NonFungible -> spec.resourceAddress
                }
                val resource = badgeResources[resourceAddress]
                if (spec is RequiredBadgeSpecification.Fungible && resource is Resource.NonFungibleResource) {
                    RequiredBadgeSpecification.NonFungible(resourceAddress, null)
                } else {
                    spec
                }
            }

            val resolvedBadges = mutableListOf<ResourceSpecifier>()
            var overallStatus: BadgeRequirementStatus = BadgeRequirementStatus.None
            var errorResource: Resource? = null
            var errorReason: BadgeRequirementStatus.Error.Reason? = null

            for (spec in resolvedSpecs) {
                when (spec) {
                    is RequiredBadgeSpecification.Fungible -> {
                        val ownedToken = assets?.tokens?.find { it.resource.address == spec.resourceAddress }
                        val resource = ownedToken?.resource ?: badgeResources[spec.resourceAddress] ?: Resource.FungibleResource(address = spec.resourceAddress, ownedAmount = null)
                        
                        if (errorResource == null) {
                            errorResource = resource
                            errorReason = BadgeRequirementStatus.Error.Reason.MissingBadge
                        }

                        if (ownedToken != null) {
                            val ownedAmount = ownedToken.resource.ownedAmount.orZero()
                            if (ownedAmount > 0.toDecimal192()) {
                                resolvedBadges.add(ResourceSpecifier.Fungible(spec.resourceAddress, 1.toDecimal192()))
                                if (overallStatus is BadgeRequirementStatus.None) {
                                    overallStatus = BadgeRequirementStatus.Success(resource)
                                }
                            } else {
                                if (errorReason == BadgeRequirementStatus.Error.Reason.MissingBadge) {
                                    errorResource = resource
                                    errorReason = BadgeRequirementStatus.Error.Reason.InsufficientBalance
                                }
                            }
                        }
                    }
                    is RequiredBadgeSpecification.NonFungible -> {
                        val ownedCollection = assets?.nonFungibles?.find { it.collection.address == spec.resourceAddress }
                        val resource = ownedCollection?.collection ?: badgeResources[spec.resourceAddress] ?: Resource.NonFungibleResource(address = spec.resourceAddress, amount = 0, items = emptyList())
                        
                        if (errorResource == null) {
                            errorResource = resource
                            errorReason = BadgeRequirementStatus.Error.Reason.MissingBadge
                        }

                        if (ownedCollection != null) {
                            val ownedItems = ownedCollection.collection.items
                            if (ownedItems.isNotEmpty()) {
                                val targetId = spec.localId
                                if (targetId != null) {
                                    if (ownedItems.any { it.localId == targetId }) {
                                        resolvedBadges.add(ResourceSpecifier.NonFungible(spec.resourceAddress, listOf(targetId)))
                                        if (overallStatus is BadgeRequirementStatus.None) {
                                            overallStatus = BadgeRequirementStatus.Success(resource)
                                        }
                                    }
                                } else {
                                    resolvedBadges.add(ResourceSpecifier.NonFungible(spec.resourceAddress, listOf(ownedItems.first().localId)))
                                    if (overallStatus is BadgeRequirementStatus.None) {
                                        overallStatus = BadgeRequirementStatus.Success(resource)
                                    }
                                }
                            } else {
                                if (errorReason == BadgeRequirementStatus.Error.Reason.MissingBadge) {
                                    errorResource = resource
                                    errorReason = BadgeRequirementStatus.Error.Reason.InsufficientBalance
                                }
                            }
                        }
                    }
                }
            }

            if (resolvedBadges.isEmpty() && errorResource != null && errorReason != null) {
                return@runCatching BadgeResult(
                    BadgeRequirementStatus.Error(errorResource, errorReason),
                    emptyList()
                )
            }

            BadgeResult(overallStatus, resolvedBadges)
        }
    }

    data class BadgeResult(
        val status: BadgeRequirementStatus,
        val badges: List<ResourceSpecifier>
    )
}

