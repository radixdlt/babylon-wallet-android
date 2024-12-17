package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.defaultDepositRule
import com.babylon.wallet.android.data.gateway.extensions.isEntityActive
import com.babylon.wallet.android.data.gateway.extensions.toProfileDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.di.ShortTimeoutStateApi
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus.Status
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.os.SargonOsManager
import rdx.works.core.domain.resources.ExplicitMetadataKey
import javax.inject.Inject

class ResolveAccountsLedgerStateRepository @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @ShortTimeoutStateApi private val stateApi: StateApi
) {

    suspend operator fun invoke(
        accounts: List<Account>
    ): Result<List<AccountWithOnLedgerStatus>> {
        if (accounts.isEmpty()) return Result.success(emptyList())
        val networkId = accounts.first().networkId

        val statuses = mutableMapOf<AccountAddress, Status>()
        val accountAddressesWithDeletedStatus = sargonOsManager.sargonOs.checkAccountsDeletedOnLedger(
            networkId = networkId,
            accountAddresses = accounts.map { it.address }
        )
        accounts.forEach { account ->
            val isDeleted = accountAddressesWithDeletedStatus.getOrDefault(account.address, false)
            if (isDeleted) {
                statuses[account.address] = Status.Deleted
            }
        }

        val defaultDepositRulesForNonDeletedAccounts = mutableMapOf<AccountAddress, DepositRule>()
        accounts
            // Accounts we know are deleted don't need to be queried
            .filterNot { statuses[it.address] == Status.Deleted }
            .chunked(Constants.MAX_ITEMS_PER_ENTITY_DETAILS_REQUEST)
            .forEach { accountsChunked ->
                accountsChunked.resolveStatusOnLedger(statuses, defaultDepositRulesForNonDeletedAccounts).onFailure {
                    return Result.failure(it)
                }
            }

        return Result.success(
            accounts.map { account ->
                val status = statuses[account.address] ?: Status.Active

                val depositRule = if (status != Status.Deleted) {
                    defaultDepositRulesForNonDeletedAccounts[account.address] ?: DepositRule.ACCEPT_ALL
                } else {
                    DepositRule.DENY_ALL
                }

                AccountWithOnLedgerStatus(
                    account = account.copy(
                        onLedgerSettings = account.onLedgerSettings.copy(
                            thirdPartyDeposits = account.onLedgerSettings.thirdPartyDeposits.copy(depositRule = depositRule)
                        )
                    ),
                    status = status
                )
            }
        )
    }

    private suspend fun List<Account>.resolveStatusOnLedger(
        statuses: MutableMap<AccountAddress, Status>,
        defaultDepositRulesForNonDeletedAccounts: MutableMap<AccountAddress, DepositRule>
    ): Result<Unit> {
        val items = stateApi.stateEntityDetails(
            StateEntityDetailsRequest(
                addresses = map { it.address.string },
                optIns = StateEntityDetailsOptIns(
                    explicitMetadata = listOf(
                        ExplicitMetadataKey.OWNER_BADGE.key,
                        ExplicitMetadataKey.OWNER_KEYS.key,
                    )
                )
            )
        ).toResult().getOrElse { error ->
            return Result.failure(error)
        }.items

        items.forEach { item ->
            val accountAddress = AccountAddress.init(item.address)
            statuses[accountAddress] = if (item.isEntityActive) Status.Active else Status.Inactive
            defaultDepositRulesForNonDeletedAccounts[accountAddress] = if (item.isEntityActive) {
                item.defaultDepositRule?.toProfileDepositRule() ?: DepositRule.ACCEPT_ALL
            } else {
                DepositRule.ACCEPT_ALL
            }
        }

        return Result.success(Unit)
    }
}
