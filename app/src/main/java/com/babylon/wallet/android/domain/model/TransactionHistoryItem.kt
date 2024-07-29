package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.extensions.decode
import com.babylon.wallet.android.data.gateway.generated.models.CommittedTransactionInfo
import com.babylon.wallet.android.data.gateway.generated.models.ManifestClass
import com.babylon.wallet.android.data.gateway.generated.models.TransactionBalanceChanges
import com.babylon.wallet.android.data.gateway.generated.models.TransactionFungibleBalanceChanges
import com.babylon.wallet.android.data.gateway.generated.models.TransactionNonFungibleBalanceChanges
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatus
import com.babylon.wallet.android.utils.timestampHoursMinutes
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.abs
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.isNegative
import com.radixdlt.sargon.extensions.isPositive
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.toDecimal192OrNull
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Resource
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
    val feePaid: Decimal192,
    private val balanceChanges: List<BalanceChange>,
    val transactionClass: TransactionClass?,
    val timestamp: Instant?,
    val message: String?,
    val isFailedTransaction: Boolean
) {
    val deposited: List<BalanceChange> = balanceChanges.filter {
        if (it.entityAddress.string != accountAddress.string) return@filter false
        when (it) {
            is BalanceChange.FungibleBalanceChange -> it.isBalanceChangePositive
            is BalanceChange.NonFungibleBalanceChange -> it.addedIds.isNotEmpty()
        }
    }
    val withdrawn: List<BalanceChange> = balanceChanges.filter {
        if (it.entityAddress.string != accountAddress.string) return@filter false
        when (it) {
            is BalanceChange.FungibleBalanceChange -> it.isBalanceChangeNegative
            is BalanceChange.NonFungibleBalanceChange -> it.removedIds.isNotEmpty()
        }
    }

    val hasNoBalanceChanges: Boolean = deposited.isEmpty() && withdrawn.isEmpty()

    val hoursAndMinutes = timestamp?.timestampHoursMinutes()
}

data class HistoryFilters(
    val start: ZonedDateTime? = null,
    val transactionType: TransactionType? = null,
    val resource: Resource? = null,
    val transactionClass: TransactionClass? = null,
    val sortOrder: SortOrder = SortOrder.Desc,
    val genesisTxDate: ZonedDateTime? = null
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
        val formattedBalanceChange: String,
        val isBalanceChangePositive: Boolean,
        val isBalanceChangeNegative: Boolean,
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
        ManifestClass.General -> TransactionClass.General
        ManifestClass.Transfer -> TransactionClass.Transfer
        ManifestClass.PoolContribution -> TransactionClass.PoolContribution
        ManifestClass.PoolRedemption -> TransactionClass.PoolRedemption
        ManifestClass.ValidatorStake -> TransactionClass.ValidatorStake
        ManifestClass.ValidatorUnstake -> TransactionClass.ValidatorUnstake
        ManifestClass.ValidatorClaim -> TransactionClass.ValidatorClaim
        ManifestClass.AccountDepositSettingsUpdate -> TransactionClass.AccountDespositSettingsUpdate
    }
}

fun TransactionClass.toManifestClass(): ManifestClass {
    return when (this) {
        TransactionClass.General -> ManifestClass.General
        TransactionClass.Transfer -> ManifestClass.Transfer
        TransactionClass.PoolContribution -> ManifestClass.PoolContribution
        TransactionClass.PoolRedemption -> ManifestClass.PoolRedemption
        TransactionClass.ValidatorStake -> ManifestClass.ValidatorStake
        TransactionClass.ValidatorUnstake -> ManifestClass.ValidatorUnstake
        TransactionClass.ValidatorClaim -> ManifestClass.ValidatorClaim
        TransactionClass.AccountDespositSettingsUpdate -> ManifestClass.AccountDepositSettingsUpdate
    }
}

fun TransactionBalanceChanges.toDomainModel(assets: List<Asset>): List<BalanceChange> {
    val fungibleFungibleBalanceChanges = fungibleBalanceChanges.toFungibleBalanceChangeDomainModels(assets)
    val nonFungibleFungibleBalanceChanges = nonFungibleBalanceChanges.toNonFungibleBalanceChangeDomainModels(assets)
    return (fungibleFungibleBalanceChanges + nonFungibleFungibleBalanceChanges).sorted()
}

private fun List<TransactionFungibleBalanceChanges>.toFungibleBalanceChangeDomainModels(assets: List<Asset>): List<BalanceChange> {
    return map { item ->
        val balanceChange = item.balanceChange.toDecimal192()
        val asset = assets.filterIsInstance<Asset.Fungible>()
            .find { it.resource.address.string == item.resourceAddress }

        BalanceChange.FungibleBalanceChange(
            formattedBalanceChange = balanceChange.abs().formatted(),
            isBalanceChangePositive = balanceChange.isPositive,
            isBalanceChangeNegative = balanceChange.isNegative,
            entityAddress = Address.init(item.entityAddress),
            asset = when (asset) {
                is LiquidStakeUnit -> asset.copy(
                    fungibleResource = asset.fungibleResource.copy(
                        ownedAmount = item.balanceChange.toDecimal192().abs()
                    )
                )

                is PoolUnit -> asset.copy(stake = asset.stake.copy(ownedAmount = item.balanceChange.toDecimal192().abs()))
                is Token -> asset.copy(resource = asset.resource.copy(ownedAmount = item.balanceChange.toDecimal192().abs()))
                null -> null
            }
        )
    }
}

private fun List<TransactionNonFungibleBalanceChanges>.toNonFungibleBalanceChangeDomainModels(assets: List<Asset>): List<BalanceChange> {
    return map { item ->
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
            removedIds = item.removed,
            addedIds = item.added,
            entityAddress = Address.init(item.entityAddress),
            asset = relatedAsset
        )
    }
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
        feePaid = feePaid?.toDecimal192OrNull().orZero(),
        balanceChanges = balanceChanges?.toDomainModel(assets = assets).orEmpty(),
        transactionClass = manifestClasses?.firstOrNull()?.toTransactionClass(),
        timestamp = confirmedAt?.toInstant(),
        message = message?.decode(),
        isFailedTransaction = transactionStatus == TransactionStatus.CommittedFailure || transactionStatus == TransactionStatus.Rejected
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
