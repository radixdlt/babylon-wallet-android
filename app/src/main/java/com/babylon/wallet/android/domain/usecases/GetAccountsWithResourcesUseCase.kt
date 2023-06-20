package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import java.math.BigDecimal
import javax.inject.Inject

class GetAccountsWithResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(accounts: List<Network.Account>, isRefreshing: Boolean): Result<List<AccountWithResources>> {
        return entityRepository.getAccountsWithResources(
            accounts = accounts,
            isRefreshing = isRefreshing
        ).map { accounts ->
            accounts.mapWhen(
                predicate = { accounts.indexOf(it) == 0 },
                mutation = { account ->
                    val fungibles = List(size = 100, init = { index ->
                        Resource.FungibleResource(
                            resourceAddress = "resource_rdx_abcd$index",
                            amount = BigDecimal.TEN,
                            nameMetadataItem = NameMetadataItem(name = "Resource $index"),
                            symbolMetadataItem = SymbolMetadataItem(symbol = "CSTM$index")
                        )
                    }
                    )
                    account.copy(
                        resources = account.resources?.copy(
                            fungibleResources = account.resources.fungibleResources + fungibles
                        )
                    )
                }
            )
        }
    }

    suspend operator fun invoke(isRefreshing: Boolean): Result<List<AccountWithResources>> {
        val accounts = getProfileUseCase.accountsOnCurrentNetwork()
        return invoke(accounts = accounts, isRefreshing = isRefreshing)
    }
}
