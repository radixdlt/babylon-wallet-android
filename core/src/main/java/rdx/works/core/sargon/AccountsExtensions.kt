package rdx.works.core.sargon

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DerivationPathScheme
import com.radixdlt.sargon.EntityFlag
import com.radixdlt.sargon.FactorSourceId

fun Collection<Account>.notHiddenAccounts(): List<Account> = filter { !it.isHidden }
fun Collection<Account>.hiddenAccounts(): List<Account> = filter { it.isHidden }

val Account.factorSourceId: FactorSourceId
    get() = securityState.factorSourceId

val Account.derivationPathScheme: DerivationPathScheme
    get() = securityState.derivationPathScheme

val Account.hasAuthSigning: Boolean
    get() = securityState.hasAuthSigning

val Account.derivationPathEntityIndex: UInt?
    get() = securityState.transactionFactorInstance.publicKey.derivationPath.entityIndex

val Account.usesEd25519: Boolean
    get() = securityState.usesEd25519

val Account.usesSECP256k1: Boolean
    get() = securityState.usesSECP256k1

val Account.isHidden: Boolean
    get() = EntityFlag.DELETED_BY_USER in flags
