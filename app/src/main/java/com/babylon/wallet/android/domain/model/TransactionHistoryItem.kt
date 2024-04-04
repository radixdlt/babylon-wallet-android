package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.extensions.decode
import com.babylon.wallet.android.data.gateway.generated.models.CommittedTransactionInfo
import com.babylon.wallet.android.data.gateway.generated.models.ManifestClass
import com.babylon.wallet.android.data.gateway.generated.models.TransactionBalanceChanges
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class TransactionHistoryData(
    val stateVersion: Long,
    val prevCursorId: String?,
    val nextCursorId: String?,
    val filters: HistoryFilters,
    val items: List<TransactionHistoryItem>
) {
    val groupedByDate: Map<String, List<TransactionHistoryItem>>
        get() = items.groupBy {
            val zonedDateTime = it.timestamp?.atZone(ZoneId.systemDefault()) ?: ZonedDateTime.now()
            zonedDateTime.year.toString() + zonedDateTime.dayOfYear
        }

    fun append(items: List<TransactionHistoryItem>, nextCursorId: String?): TransactionHistoryData {
        return copy(
            nextCursorId = nextCursorId,
            items = (this.items + items).distinctBy { it.txId }.sortedByDescending { it.timestamp }
        )
    }

    fun prepend(items: List<TransactionHistoryItem>, prevCursorId: String?): TransactionHistoryData {
        return copy(
            prevCursorId = prevCursorId,
            items = (items + this.items).distinctBy { it.txId }.sortedByDescending { it.timestamp }
        )
    }
}

data class TransactionHistoryItem(
    val accountAddress: AccountAddress,
    val txId: String,
    val feePaid: BigDecimal,
    private val balanceChanges: List<BalanceChange>,
    val transactionClass: TransactionClass?,
    val timestamp: Instant?,
    val message: String?,
    val isFailedTransaction: Boolean
) {
    val deposited: List<BalanceChange>
        get() = balanceChanges.filter {
            if (it.entityAddress.string != accountAddress.string) return@filter false
            when (it) {
                is BalanceChange.FungibleBalanceChange -> it.balanceChange.signum() == 1
                is BalanceChange.NonFungibleBalanceChange -> it.addedIds.isNotEmpty()
            }
        }
    val withdrawn: List<BalanceChange>
        get() = balanceChanges.filter {
            if (it.entityAddress.string != accountAddress.string) return@filter false
            when (it) {
                is BalanceChange.FungibleBalanceChange -> it.balanceChange.signum() == -1
                is BalanceChange.NonFungibleBalanceChange -> it.removedIds.isNotEmpty()
            }
        }

    val hasNoBalanceChanges: Boolean
        get() = deposited.isEmpty() && withdrawn.isEmpty()
}

data class HistoryFilters(
    val start: ZonedDateTime? = null,
    val transactionType: TransactionType? = null,
    val resource: Resource? = null,
    val transactionClass: TransactionClass? = null,
    val sortOrder: SortOrder = SortOrder.Desc
) {

    enum class SortOrder {
        Asc, Desc
    }

    enum class TransactionType {
        DEPOSIT, WITHDRAWAL
    }

    val isAnyFilterSet: Boolean
        get() = transactionType != null || resource != null || transactionClass != null
}

sealed interface BalanceChange : Comparable<BalanceChange> {

    val asset: Asset?
    val entityAddress: Address

    data class FungibleBalanceChange(
        val balanceChange: BigDecimal,
        override val entityAddress: Address,
        override val asset: Asset.Fungible? = null
    ) : BalanceChange {
        @Suppress("UnsafeCallOnNullableType", "ReturnCount", "MagicNumber")
        override fun compareTo(other: BalanceChange): Int {
            val currentAsset = asset
            val otherAsset = other.asset
            if (currentAsset == null && otherAsset == null) return 0
            if (currentAsset == null) return -1
            if (other.asset == null) return 1
            val assetOrderComparison = currentAsset.assetHistoryDisplayOrder().compareTo(otherAsset!!.assetHistoryDisplayOrder())
            if (assetOrderComparison == 0) {
                val otherResource = otherAsset.resource as Resource.FungibleResource
                return currentAsset.resource.compareTo(otherResource)
            }
            return assetOrderComparison
        }
    }

    data class NonFungibleBalanceChange(
        val removedIds: List<String>,
        val addedIds: List<String>,
        override val entityAddress: Address,
        override val asset: Asset.NonFungible? = null
    ) : BalanceChange {
        @Suppress("UnsafeCallOnNullableType", "ReturnCount", "MagicNumber")
        override fun compareTo(other: BalanceChange): Int {
            val currentAsset = asset
            val otherAsset = other.asset
            if (currentAsset == null && otherAsset == null) return 0
            if (currentAsset == null) return -1
            if (other.asset == null) return 1
            val assetOrderComparison = currentAsset.assetHistoryDisplayOrder().compareTo(otherAsset!!.assetHistoryDisplayOrder())
            if (assetOrderComparison == 0) {
                val otherResource = otherAsset.resource as Resource.NonFungibleResource
                return currentAsset.resource.compareTo(otherResource)
            }
            return assetOrderComparison
        }
    }
}

fun ManifestClass.toTransactionClass(): TransactionClass {
    return when (this) {
        ManifestClass.general -> TransactionClass.General
        ManifestClass.transfer -> TransactionClass.Transfer
        ManifestClass.poolContribution -> TransactionClass.PoolContribution
        ManifestClass.poolRedemption -> TransactionClass.PoolRedemption
        ManifestClass.validatorStake -> TransactionClass.ValidatorStake
        ManifestClass.validatorUnstake -> TransactionClass.ValidatorUnstake
        ManifestClass.validatorClaim -> TransactionClass.ValidatorClaim
        ManifestClass.accountDepositSettingsUpdate -> TransactionClass.AccountDespositSettingsUpdate
    }
}

fun TransactionClass.toManifestClass(): ManifestClass {
    return when (this) {
        TransactionClass.General -> ManifestClass.general
        TransactionClass.Transfer -> ManifestClass.transfer
        TransactionClass.PoolContribution -> ManifestClass.poolContribution
        TransactionClass.PoolRedemption -> ManifestClass.poolRedemption
        TransactionClass.ValidatorStake -> ManifestClass.validatorStake
        TransactionClass.ValidatorUnstake -> ManifestClass.validatorUnstake
        TransactionClass.ValidatorClaim -> ManifestClass.validatorClaim
        TransactionClass.AccountDespositSettingsUpdate -> ManifestClass.accountDepositSettingsUpdate
    }
}

fun TransactionBalanceChanges.toDomainModel(assets: List<Asset>): List<BalanceChange> {
    val fungibleFungibleBalanceChanges = fungibleBalanceChanges.map { item ->
        BalanceChange.FungibleBalanceChange(
            item.balanceChange.toBigDecimal(),
            Address.init(item.entityAddress),
            when (val asset = assets.filterIsInstance<Asset.Fungible>().find { it.resource.address.string == item.resourceAddress }) {
                is LiquidStakeUnit -> asset.copy(
                    fungibleResource = asset.fungibleResource.copy(
                        ownedAmount = item.balanceChange.toBigDecimal().abs()
                    )
                )

                is PoolUnit -> asset.copy(stake = asset.stake.copy(ownedAmount = item.balanceChange.toBigDecimal().abs()))
                is Token -> asset.copy(resource = asset.resource.copy(ownedAmount = item.balanceChange.toBigDecimal().abs()))
                null -> null
            }
        )
    }
    val nonFungibleFungibleBalanceChanges = nonFungibleBalanceChanges.map { item ->
        val relatedLocalIds = item.removed.toSet() + item.added
        val relatedAsset = when (
            val asset = assets.filterIsInstance<Asset.NonFungible>().find {
                it.resource.address.string == item.resourceAddress
            }
        ) {
            is NonFungibleCollection -> {
                val updatedCollection = asset.collection.copy(
                    items = asset.collection.items.filter {
                        relatedLocalIds.contains(it.localId.string)
                    }
                )
                asset.copy(collection = updatedCollection)
            }

            is StakeClaim -> {
                val updatedResource = asset.nonFungibleResource.copy(
                    items = asset.nonFungibleResource.items.filter { relatedLocalIds.contains(it.localId.string) }
                )
                asset.copy(nonFungibleResource = updatedResource)
            }

            null -> null
        }
        BalanceChange.NonFungibleBalanceChange(
            item.removed,
            item.added,
            Address.init(item.entityAddress),
            relatedAsset
        )
    }
    return (fungibleFungibleBalanceChanges + nonFungibleFungibleBalanceChanges).sorted()
}

enum class TransactionClass {
    General,
    Transfer,
    ValidatorStake,
    ValidatorUnstake,
    ValidatorClaim,
    AccountDespositSettingsUpdate,
    PoolContribution,
    PoolRedemption
}

fun CommittedTransactionInfo.toDomainModel(accountAddress: AccountAddress, assets: List<Asset>): TransactionHistoryItem {
    return TransactionHistoryItem(
        accountAddress = accountAddress,
        txId = intentHash.orEmpty(),
        feePaid = feePaid?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        balanceChanges = balanceChanges?.toDomainModel(assets = assets).orEmpty(),
        transactionClass = manifestClasses?.firstOrNull()?.toTransactionClass(),
        timestamp = confirmedAt?.toInstant(),
        message = message?.decode(),
        isFailedTransaction = transactionStatus == TransactionStatus.committedFailure || transactionStatus == TransactionStatus.rejected
    )
}

@Suppress("MagicNumber")
private fun Asset.assetHistoryDisplayOrder(): Int {
    return when (this) {
        is LiquidStakeUnit -> 2
        is PoolUnit -> 4
        is Token -> 0
        is NonFungibleCollection -> 1
        is StakeClaim -> 3
    }
}
