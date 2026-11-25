package rdx.works.core.sargon

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressBookEntry
import com.radixdlt.sargon.AddressBookEntryTarget
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.extensions.string
import java.text.Collator

/** The address the entry names, if it names an address and not a non-fungible. */
val AddressBookEntry.addressOrNull: Address?
    get() = (target as? AddressBookEntryTarget.GlobalAddress)?.value

/** The non-fungible the entry names, e.g. the signature resource of someone's key. */
val AddressBookEntry.nonFungibleGlobalIdOrNull: NonFungibleGlobalId?
    get() = (target as? AddressBookEntryTarget.NonFungible)?.value

val AddressBookEntry.accountAddressOrNull: AccountAddress?
    get() = (addressOrNull as? Address.Account)?.v1

/** How the target is written, e.g. typed into the address book form. */
val AddressBookEntryTarget.rawString: String
    get() = when (this) {
        is AddressBookEntryTarget.GlobalAddress -> value.string
        is AddressBookEntryTarget.NonFungible -> value.string
    }

fun Iterable<AddressBookEntry>.sortedForDisplay(): List<AddressBookEntry> {
    val collator = Collator.getInstance().apply {
        strength = Collator.PRIMARY
    }

    return sortedWith { lhs, rhs ->
        val nameComparison = collator.compare(lhs.name.value, rhs.name.value)
        if (nameComparison != 0) {
            nameComparison
        } else {
            collator.compare(lhs.target.rawString, rhs.target.rawString)
        }
    }
}
