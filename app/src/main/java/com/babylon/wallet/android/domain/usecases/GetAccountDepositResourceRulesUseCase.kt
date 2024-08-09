package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.AccountDepositResourceRules
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetAccountDepositResourceRulesUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val transactionRepository: TransactionRepository
) {

    private val _accountDepositResourceRulesList: MutableStateFlow<Set<AccountDepositResourceRules>> = MutableStateFlow(emptySet())

    val accountDepositResourceRulesList = _accountDepositResourceRulesList.asStateFlow()

    private fun Set<AccountDepositResourceRules>.combine(other: Set<AccountDepositResourceRules>): Set<AccountDepositResourceRules> {
        val result = this.toMutableSet()
        other.forEach { item ->
            val existingItem = result.find { it.accountAddress.string == item.accountAddress.string }
            if (existingItem == null) {
                result.add(item)
            } else {
                result.remove(existingItem)
                result.add(item.copy(resourceRules = item.resourceRules + existingItem.resourceRules))
            }
        }
        return result.toSet()
    }

    suspend operator fun invoke(accountAddressesToResourceAddress: Map<String, Set<String>>) {
        val diff = accountAddressesToResourceAddress.entries.mapNotNull { accountAddressToResourceAddresses ->
            val existingRule =
                _accountDepositResourceRulesList.value.find { it.accountAddress.string == accountAddressToResourceAddresses.key }
            if (existingRule == null) {
                accountAddressToResourceAddresses.key to accountAddressToResourceAddresses.value
            } else {
                accountAddressToResourceAddresses.value.subtract(existingRule.resourceRules.map { it.resourceAddress.string }.toSet()).let {
                    if (it.isEmpty()) {
                        null
                    } else {
                        accountAddressToResourceAddresses.key to it
                    }
                }
            }
        }.toMap()
        withContext(ioDispatcher) {
            val newItems = diff.entries.mapNotNull { entry ->
                if (entry.value.isEmpty()) {
                    return@mapNotNull null
                }
                async {
                    transactionRepository.getAccountDepositPreValidation(entry.key, entry.value.toList())
                }
            }.awaitAll().mapNotNull {
                it.getOrNull()
            }.toSet()
            _accountDepositResourceRulesList.update { state ->
                state.combine(newItems)
            }
        }
    }
}
