package rdx.works.core.sargon

import com.radixdlt.sargon.AccessControllerAddress
import com.radixdlt.sargon.AccountOrPersona
import com.radixdlt.sargon.AddressOfAccountOrPersona

val AccountOrPersona.securityStateAccessControllerAddress: AccessControllerAddress?
    get() = when (this) {
        is AccountOrPersona.AccountEntity -> v1.securityStateAccessControllerAddress
        is AccountOrPersona.PersonaEntity -> v1.securityStateAccessControllerAddress
    }

val AccountOrPersona.address: AddressOfAccountOrPersona
    get() = when (this) {
        is AccountOrPersona.AccountEntity -> AddressOfAccountOrPersona.Account(v1.address)
        is AccountOrPersona.PersonaEntity -> AddressOfAccountOrPersona.Identity(v1.address)
    }
