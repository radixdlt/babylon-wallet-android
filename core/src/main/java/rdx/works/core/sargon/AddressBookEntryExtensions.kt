package rdx.works.core.sargon

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressBookEntry
import com.radixdlt.sargon.extensions.string
import java.text.Collator

val AddressBookEntry.accountAddressOrNull: AccountAddress?
    get() = (address as? Address.Account)?.v1

fun Iterable<AddressBookEntry>.sortedForDisplay(): List<AddressBookEntry> {
    val collator = Collator.getInstance().apply {
        strength = Collator.PRIMARY
    }

    return sortedWith { lhs, rhs ->
        val nameComparison = collator.compare(lhs.name.value, rhs.name.value)
        if (nameComparison != 0) {
            nameComparison
        } else {
            collator.compare(lhs.address.string, rhs.address.string)
        }
    }
}
