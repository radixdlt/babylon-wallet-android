package rdx.works.core.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.EntityFlags
import com.radixdlt.sargon.EntitySecurityState
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Persona

sealed interface ProfileEntity {
    val networkId: NetworkId
    val address: AddressOfAccountOrPersona
    val securityState: EntitySecurityState
    val flags: EntityFlags

    data class AccountEntity(
        val account: Account
    ): ProfileEntity {
        override val networkId: NetworkId
            get() = account.networkId
        override val address: AddressOfAccountOrPersona
            get() = AddressOfAccountOrPersona.Account(accountAddress)
        override val securityState: EntitySecurityState
            get() = account.securityState
        override val flags: EntityFlags
            get() = account.flags

        val accountAddress: AccountAddress
            get() = account.address
    }

    data class PersonaEntity(
        val persona: Persona
    ): ProfileEntity {
        override val networkId: NetworkId
            get() = persona.networkId
        override val address: AddressOfAccountOrPersona
            get() = AddressOfAccountOrPersona.Identity(identityAddress)
        override val securityState: EntitySecurityState
            get() = persona.securityState
        override val flags: EntityFlags
            get() = persona.flags

        val identityAddress: IdentityAddress
            get() = persona.address
    }
}