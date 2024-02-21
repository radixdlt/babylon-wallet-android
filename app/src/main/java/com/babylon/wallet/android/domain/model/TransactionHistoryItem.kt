package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.generated.models.ManifestClass
import com.babylon.wallet.android.data.gateway.generated.models.TransactionBalanceChanges
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.Resource
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class TransactionHistoryData(
    val stateVersion: Long,
    val items: List<TransactionHistoryItem>,
    val nextCursorId: String?,
    val currentFilters: HistoryFilters? = null
) {
    val groupedByDate: Map<String, List<TransactionHistoryItem>>
        get() = items.groupBy {
            val zonedDateTime = it.timestamp?.atZone(ZoneId.systemDefault()) ?: ZonedDateTime.now()
            zonedDateTime.year.toString() + zonedDateTime.dayOfYear
        }
}

data class TransactionHistoryItem(
    val accountAddress: String,
    val txId: String,
    val feePaid: BigDecimal,
    private val balanceChanges: List<BalanceChange>,
    val transactionClass: TransactionClass?,
    val timestamp: Instant?
) {
    val deposited: List<BalanceChange>
        get() = balanceChanges.filter {
            if (it.entityAddress != accountAddress) return@filter false
            when (it) {
                is BalanceChange.FungibleBalanceChange -> it.balanceChange.signum() == 1
                is BalanceChange.NonFungibleBalanceChange -> it.addedIds.isNotEmpty()
            }
        }
    val withdrawn: List<BalanceChange>
        get() = balanceChanges.filter {
            if (it.entityAddress != accountAddress) return@filter false
            when (it) {
                is BalanceChange.FungibleBalanceChange -> it.balanceChange.signum() == -1
                is BalanceChange.NonFungibleBalanceChange -> it.removedIds.isNotEmpty()
            }
        }

    val unknownTransaction: Boolean
        get() = transactionClass == null

    val noBalanceChanges: Boolean
        get() = deposited.isEmpty() && withdrawn.isEmpty()
}

data class HistoryFilters(
    val start: ZonedDateTime? = null,
    val end: ZonedDateTime? = null,
    val transactionType: TransactionType? = null,
    val resources: Set<Resource> = emptySet(),
    val transactionClass: TransactionClass? = null,
    val submittedBy: SubmittedBy? = null
) {
    enum class TransactionType {
        DEPOSIT, WITHDRAWAL
    }

    enum class SubmittedBy {
        Me, ThirdParty
    }

    val isAnyFilterSet: Boolean
        get() = transactionType != null || resources.isNotEmpty() || transactionClass != null || submittedBy != null
}

sealed interface BalanceChange {

    val asset: Asset?
    val entityAddress: String

    data class FungibleBalanceChange(
        val balanceChange: BigDecimal,
        override val entityAddress: String,
        override val asset: Asset.Fungible? = null
    ) : BalanceChange

    data class NonFungibleBalanceChange(
        val removedIds: List<String>,
        val addedIds: List<String>,
        override val entityAddress: String,
        override val asset: Asset.NonFungible? = null
    ) : BalanceChange
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
            item.entityAddress,
            when (val asset = assets.filterIsInstance<Asset.Fungible>().find { it.resource.resourceAddress == item.resourceAddress }) {
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
                it.resource.resourceAddress == item.resourceAddress
            }
        ) {
            is NonFungibleCollection -> {
                val updatedCollection = asset.collection.copy(
                    items = asset.collection.items.filter {
                        relatedLocalIds.contains(it.localId.code)
                    }
                )
                asset.copy(collection = updatedCollection)
            }

            is StakeClaim -> {
                val updatedResource = asset.nonFungibleResource.copy(
                    items = asset.nonFungibleResource.items.filter { relatedLocalIds.contains(it.localId.code) }
                )
                asset.copy(nonFungibleResource = updatedResource)
            }

            null -> null
        }
        BalanceChange.NonFungibleBalanceChange(
            item.removed,
            item.added,
            item.entityAddress,
            relatedAsset
        )
    }
    return fungibleFungibleBalanceChanges + nonFungibleFungibleBalanceChanges
}

enum class TransactionClass {
    General, PoolContribution, PoolRedemption, Transfer, ValidatorClaim, ValidatorStake, ValidatorUnstake, AccountDespositSettingsUpdate
}
