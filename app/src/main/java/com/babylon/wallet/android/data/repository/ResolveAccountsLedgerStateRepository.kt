package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.coreapi.DefaultDepositRule
import com.babylon.wallet.android.data.gateway.extensions.defaultDepositRule
import com.babylon.wallet.android.data.gateway.extensions.isEntityActive
import com.babylon.wallet.android.data.gateway.extensions.toProfileDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.di.ShortTimeoutStateApi
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DepositRule
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.ExplicitMetadataKey
import javax.inject.Inject

class ResolveAccountsLedgerStateRepository @Inject constructor(
    @ShortTimeoutStateApi private val stateApi: StateApi
) {

    suspend operator fun invoke(accounts: List<Account>): Result<List<AccountWithOnLedgerStatus>> {
        val activeAddresses = mutableSetOf<AccountAddress>()
        val defaultDepositRules = mutableMapOf<AccountAddress, DefaultDepositRule>()
        accounts.map { it.address }.chunked(Constants.MAX_ITEMS_PER_ENTITY_DETAILS_REQUEST).forEach { addressesChunk ->
            stateApi.stateEntityDetails(
                StateEntityDetailsRequest(
                    addresses = addressesChunk.map { it.string },
                    optIns = StateEntityDetailsOptIns(
                        explicitMetadata = listOf(
                            ExplicitMetadataKey.OWNER_BADGE.key,
                            ExplicitMetadataKey.OWNER_KEYS.key,
                        )
                    )
                )
            ).toResult().onSuccess { response ->
                response.items.forEach { item ->
                    if (item.isEntityActive) {
                        val accountAddress = AccountAddress.init(item.address)
                        activeAddresses.add(accountAddress)
                        item.defaultDepositRule?.let { defaultDepositRule ->
                            defaultDepositRules[accountAddress] = defaultDepositRule
                        }
                    }
                }
            }.onFailure {
                return Result.failure(it)
            }
        }
        return Result.success(
            accounts.map { account ->
                val defaultDepositRule = defaultDepositRules[account.address]?.toProfileDepositRule()
                val updatedThirdPartyDeposits = account.onLedgerSettings.thirdPartyDeposits.copy(
                    depositRule = defaultDepositRule ?: DepositRule.ACCEPT_ALL
                )
                AccountWithOnLedgerStatus(
                    account = account.copy(
                        onLedgerSettings = account.onLedgerSettings.copy(thirdPartyDeposits = updatedThirdPartyDeposits)
                    ),
                    status = if (activeAddresses.contains(account.address)) {
                        AccountWithOnLedgerStatus.Status.Active
                    } else {
                        AccountWithOnLedgerStatus.Status.Inactive
                    }
                )
            }
        )
    }
}
