package rdx.works.core.sargon

import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.extensions.formatted

val ResourceOrNonFungible.resourceAddress: ResourceAddress
    get() = when (this) {
        is ResourceOrNonFungible.NonFungible -> value.resourceAddress
        is ResourceOrNonFungible.Resource -> value
    }

fun ResourceOrNonFungible.formatted(format: AddressFormat = AddressFormat.DEFAULT) = when (this) {
    is ResourceOrNonFungible.NonFungible -> value.formatted(format = format)
    is ResourceOrNonFungible.Resource -> value.formatted(format = format)
}
