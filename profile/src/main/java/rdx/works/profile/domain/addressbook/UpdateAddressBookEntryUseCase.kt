package rdx.works.profile.domain.addressbook

import com.radixdlt.sargon.AddressBookEntryTarget
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class UpdateAddressBookEntryUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        target: AddressBookEntryTarget,
        name: DisplayName,
        note: String?
    ): Boolean = withContext(defaultDispatcher) {
        sargonOsManager.sargonOs.updateAddressBookEntry(target, name, note)
    }
}
