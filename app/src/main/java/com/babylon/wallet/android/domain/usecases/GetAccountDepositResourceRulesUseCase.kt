package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.AccountDepositResourceRules
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAccountDepositResourceRulesUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(accountAddressesToResourceAddress: Map<String, Set<String>>): Set<AccountDepositResourceRules> {
        return withContext(ioDispatcher) {
            accountAddressesToResourceAddress.entries.mapNotNull { entry ->
                if (entry.value.isEmpty()) {
                    return@mapNotNull null
                }
                async {
                    transactionRepository.getAccountDepositPreValidation(entry.key, entry.value.toList())
                }
            }.awaitAll().mapNotNull {
                it.getOrNull()
            }.toSet()
        }
    }
}
