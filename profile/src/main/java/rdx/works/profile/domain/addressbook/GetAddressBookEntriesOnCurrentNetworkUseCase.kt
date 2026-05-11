package rdx.works.profile.domain.addressbook

import com.radixdlt.sargon.AddressBookEntry
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class GetAddressBookEntriesOnCurrentNetworkUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(): List<AddressBookEntry> = withContext(defaultDispatcher) {
        sargonOsManager.sargonOs.addressBookOnCurrentNetwork()
    }
}
