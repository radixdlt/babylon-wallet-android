package rdx.works.core.sargon

import com.radixdlt.sargon.AuthIntent
import com.radixdlt.sargon.AuthIntentHash
import com.radixdlt.sargon.CompiledSubintent
import com.radixdlt.sargon.CompiledTransactionIntent
import com.radixdlt.sargon.Hash
import com.radixdlt.sargon.SubintentHash
import com.radixdlt.sargon.TransactionIntent
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.extensions.compile
import com.radixdlt.sargon.extensions.decompile
import com.radixdlt.sargon.extensions.hash

/**
 * A generic representation of how a signable looks like in sargon internally.
 *
 * TODO might need to be moved to sargon kotlin
 */
sealed interface Signable {

    fun getPayload(): Payload

    fun getId(): ID

    fun hash(): Hash = getId().hash()

    data class Transaction(
        val value: TransactionIntent
    ): Signable {
        override fun getPayload(): Payload = Payload.Transaction(
            value = value.compile()
        )

        override fun getId(): ID = ID.Transaction(
            value = value.hash()
        )
    }

    data class Subintent(
        val value: com.radixdlt.sargon.Subintent
    ): Signable {
        override fun getPayload(): Payload = Payload.Subintent(
            value = value.compile()
        )

        override fun getId(): ID = ID.Subintent(
            value = value.hash()
        )
    }

    data class Auth(
        val value: AuthIntent
    ): Signable {
        override fun getPayload(): Payload = Payload.Auth(
            value = value
        )

        override fun getId(): ID = ID.Auth(
            value = value.hash()
        )
    }

    sealed interface Payload {

        fun getSignable(): Signable

        data class Transaction(
            val value: CompiledTransactionIntent
        ): Payload {
            override fun getSignable(): Signable = Transaction(value.decompile())
        }

        data class Subintent(
            val value: CompiledSubintent
        ): Payload {
            override fun getSignable(): Signable = Subintent(value.decompile())
        }

        data class Auth(
            val value: AuthIntent
        ): Payload {
            override fun getSignable(): Signable = Signable.Auth(value)
        }

    }

    sealed interface ID {

        fun hash(): Hash

        data class Transaction(
            val value: TransactionIntentHash
        ): ID {
            override fun hash(): Hash = value.hash
        }

        data class Subintent(
            val value: SubintentHash
        ): ID {
            override fun hash(): Hash = value.hash
        }

        data class Auth(
            val value: AuthIntentHash
        ): ID {
            override fun hash(): Hash = value.payload.hash()
        }
    }
}
