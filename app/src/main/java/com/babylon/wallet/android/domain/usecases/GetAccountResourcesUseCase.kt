package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.extensions.allResourceAddresses
import com.babylon.wallet.android.data.gateway.extensions.amount
import com.babylon.wallet.android.data.gateway.extensions.amountDecimal
import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.extensions.nonFungibleResourceAddresses
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItem
import com.babylon.wallet.android.data.gateway.generated.models.StateNonFungibleDataResponse
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.AccountAddress
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.FungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleMetadataContainer
import com.babylon.wallet.android.domain.model.NonFungibleToken
import com.babylon.wallet.android.domain.model.NonFungibleTokenItemContainer
import com.babylon.wallet.android.domain.model.OwnedFungibleToken
import com.babylon.wallet.android.domain.model.OwnedNonFungibleToken
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountFactorSourceIDOfDeviceKind
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

class GetAccountResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val preferencesManager: PreferencesManager,
    private val getProfileUseCase: GetProfileUseCase,
    private val mnemonicRepository: MnemonicRepository
) {

    /**
     * Retrieves all data related to the accounts saved in the profile.
     *
     * @param isRefreshing When need to override the cache.
     */
    suspend fun getAccountsFromProfile(isRefreshing: Boolean) = getProfileUseCase
        .accountsOnCurrentNetwork()
        .resolveDetailsInGateway(isRefreshing = isRefreshing)

    /**
     * Retrieves all data related to those specific [addresses] that exist in the Profile.
     */
    suspend fun getAccounts(addresses: List<String>, isRefreshing: Boolean) = getProfileUseCase
        .accountsOnCurrentNetwork()
        .filter { it.address in addresses }
        .resolveDetailsInGateway(isRefreshing = isRefreshing)

    /**
     * Retrieves all data related to this certain address that exists in the Profile.
     *
     * @param isRefreshing When need to override the cache.
     */
    suspend fun getAccount(address: String, isRefreshing: Boolean): Result<AccountResources> =
        getAccounts(addresses = listOf(address), isRefreshing = isRefreshing).map { it.first() }

    private suspend fun List<Network.Account>.resolveDetailsInGateway(
        isRefreshing: Boolean
    ): Result<List<AccountResources>> = entityRepository.stateEntityDetails(
        addresses = this.map { it.address },
        isRefreshing = isRefreshing
    ).map { result ->
        val backedUpFactorSourceIds = preferencesManager.getBackedUpFactorSourceIds().first()
        val accountsOnGateway = result.items

        // Compile a list of all accounts' fungible and non fungible resources
        val allResourceAddresses = accountsOnGateway.map { accountItem ->
            accountItem.allResourceAddresses
        }.flatten()

        // Query those resources all at once
        val allResources = allResourceAddresses.getResourcesData(isRefreshing)

        // Query non fungible ids only for non fungible resources contained in all accounts
        // and associate them with the resource address
        val nonFungiblesWithIds = accountsOnGateway.associateWithNonFungibleIds(isRefreshing)

        val nonFungiblesWithData = accountsOnGateway.associateWithNonFungibleData(
            nonFungibleIds = nonFungiblesWithIds.values.map { nf -> nf?.ids.orEmpty() }.flatten(),
            isRefreshing = isRefreshing
        )

        // For every Account stored in the profile, map it to AccountResources
        this.mapNotNull { profileAccount ->
            val accountOnGateway = accountsOnGateway.find {
                it.address == profileAccount.address
            } ?: return@mapNotNull null

            val fungibleTokens = accountOnGateway.resolveFungibleTokens(allResources)
            val nonFungibleTokens = accountOnGateway.resolveNonFungibleTokens(
                allResources,
                nonFungiblesWithData
            )
            val accountFactorSourceIDOfDeviceKind = getProfileUseCase.accountFactorSourceIDOfDeviceKind(profileAccount.address)
            val mnemonic = accountFactorSourceIDOfDeviceKind?.let { mnemonicRepository.readMnemonic(it) }
            val needMnemonicRecovery = accountFactorSourceIDOfDeviceKind != null && mnemonic == null
            val needMnemonicBackup = accountFactorSourceIDOfDeviceKind != null && mnemonic != null &&
                !backedUpFactorSourceIds.contains(accountFactorSourceIDOfDeviceKind.value)
            AccountResources(
                address = profileAccount.address,
                displayName = profileAccount.displayName,
                isOlympiaAccount = profileAccount.isOlympiaAccount(),
                appearanceID = profileAccount.appearanceID,
                needMnemonicBackup = needMnemonicBackup,
                needMnemonicRecovery = needMnemonicRecovery,
                fungibleTokens = fungibleTokens.toPersistentList(),
                nonFungibleTokens = nonFungibleTokens.toPersistentList()
            )
        }
    }

    private suspend fun List<StateEntityDetailsResponseItem>.associateWithNonFungibleIds(
        isRefreshing: Boolean
    ) = map { it.nonFungibleResourceAddresses }
        .flatten()
        .associateWith { address ->
            entityRepository.getNonFungibleIds(
                address = address,
                isRefreshing = isRefreshing
            ).value()
        }

    private suspend fun List<StateEntityDetailsResponseItem>.associateWithNonFungibleData(
        nonFungibleIds: List<String>,
        isRefreshing: Boolean
    ) = map { it.nonFungibleResourceAddresses }
        .flatten()
        .associateWith { address ->
            entityRepository.nonFungibleData(
                address = address,
                nonFungibleIds = nonFungibleIds,
                isRefreshing = isRefreshing
            ).value()
        }

    private suspend fun List<String>.getResourcesData(
        isRefreshing: Boolean
    ) = if (isNotEmpty()) {
        entityRepository.stateEntityDetails(
            addresses = this,
            isRefreshing = isRefreshing
        ).value()?.items.orEmpty()
    } else {
        emptyList()
    }

    private fun StateEntityDetailsResponseItem.resolveFungibleTokens(
        allResources: List<StateEntityDetailsResponseItem>
    ) = fungibleResources?.items?.map {
        val tokenResource = allResources.find { resource ->
            resource.address == it.resourceAddress
        } ?: error("Resource ${it.resourceAddress} not found")

        OwnedFungibleToken(
            owner = AccountAddress(address),
            amount = it.amountDecimal,
            address = it.resourceAddress,
            token = FungibleToken(
                address = tokenResource.address,
                metadata = tokenResource.metadata.asMetadataStringMap()
            )
        )
    }.orEmpty()

    private fun StateEntityDetailsResponseItem.resolveNonFungibleTokens(
        allResources: List<StateEntityDetailsResponseItem>,
        nonFungiblesWithData: Map<String, StateNonFungibleDataResponse?>
    ) = nonFungibleResources?.items?.map {
        val tokenResource = allResources.find { resource ->
            resource.address == it.resourceAddress
        } ?: error("Resource ${it.resourceAddress} not found")

        val nfts = nonFungiblesWithData[it.resourceAddress]?.nonFungibleIds?.map { nftDetailItem ->
            // Temporary hack to loop through all elements and search for a link until
            // we have solid solution with backend support
            val nftImage = nftDetailItem.mutableData.rawJson.elements.find { element ->
                element.value.contains("https") &&
                    (
                        element.value.contains(".jpg") ||
                            element.value.contains(".png") ||
                            element.value.contains(".svg")
                        )
            }
            NonFungibleTokenItemContainer(
                id = nftDetailItem.nonFungibleId,
                nftImage = nftImage?.value.orEmpty()
            )
        }.orEmpty()
        OwnedNonFungibleToken(
            owner = AccountAddress(address),
            amount = it.amount,
            tokenResourceAddress = it.resourceAddress,
            token = NonFungibleToken(
                address = tokenResource.address,
                nfts = nfts,
                metadataContainer = NonFungibleMetadataContainer(
                    metadata = tokenResource.metadata.asMetadataStringMap()
                )
            )
        )
    }?.filterNot { it.amount == 0L }.orEmpty()
}
