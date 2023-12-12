package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.defaultDepositRule
import com.babylon.wallet.android.data.gateway.extensions.isEntityActive
import com.babylon.wallet.android.data.gateway.extensions.toProfileDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.DefaultDepositRule
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.ShortTimeoutGatewayHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.domain.model.AccountWithOnLedgerStatus
import okhttp3.OkHttpClient
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentGateway
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import retrofit2.Converter
import javax.inject.Inject

class ResolveAccountsLedgerStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @ShortTimeoutGatewayHttpClient private val httpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory
) {

    suspend operator fun invoke(accounts: List<Network.Account>): Result<List<AccountWithOnLedgerStatus>> {
        val activeAddresses = mutableSetOf<String>()
        val defaultDepositRules = mutableMapOf<String, DefaultDepositRule>()
        accounts.map { it.address }.chunked(maxItemsPerRequest).forEach { addressesChunk ->
            buildApi<StateApi>(
                baseUrl = profileRepository.inMemoryProfileOrNull?.currentGateway?.url ?: Radix.Gateway.default.url,
                okHttpClient = httpClient,
                jsonConverterFactory = jsonConverterFactory
            ).stateEntityDetails(
                StateEntityDetailsRequest(
                    addressesChunk,
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
                        activeAddresses.add(item.address)
                        item.defaultDepositRule?.let { defaultDepositRule ->
                            defaultDepositRules[item.address] = defaultDepositRule
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
                    depositRule = defaultDepositRule ?: Network.Account.OnLedgerSettings.ThirdPartyDeposits.DepositRule.AcceptAll,
                    depositorsAllowList = null,
                    assetsExceptionList = null
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

    companion object {
        private const val maxItemsPerRequest = 20
    }
}
