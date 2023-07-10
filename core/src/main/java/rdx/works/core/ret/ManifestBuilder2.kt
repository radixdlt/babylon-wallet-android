package rdx.works.core.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestBucket
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.toByteArray
import rdx.works.core.toUByteList

class ManifestBuilder2 {
    private var instructions = StringBuilder()
    private var blobs: MutableList<ByteArray> = mutableListOf()
    private var latestBucketIndex: UInt = 0u

    fun withdraw(
        fromAccount: Address,
        fungible: Address,
        amount: Decimal
    ) = apply {
        instructions.appendLine(
            """
            CALL_METHOD
                 Address("${fromAccount.addressString()}")
                "${ManifestMethod.Withdraw.value}"
                Address("${fungible.addressString()}")
                Decimal("${amount.asStr()}")
            ;
            """.trimIndent()
        )
    }

    fun withdraw(
        fromAccount: Address,
        nonFungible: NonFungibleGlobalId
    ) = apply {
        instructions.appendLine(
            """
            CALL_METHOD
                 Address("${fromAccount.addressString()}")
                "${ManifestMethod.WithdrawNonFungibles.value}"
                Address("${nonFungible.resourceAddress().addressString()}")
                Array<NonFungibleLocalId>(NonFungibleLocalId("${nonFungible.localId().asStr()}"))
            ;
            """.trimIndent()
        )
    }

    fun takeFromWorktop(
        fungible: Address,
        amount: Decimal,
        intoBucket: ManifestBucket
    ) = apply {
        instructions.appendLine(
            """
            TAKE_FROM_WORKTOP
                Address("${fungible.addressString()}")
                Decimal("${amount.asStr()}")
                Bucket("${intoBucket.value}")
            ;   
            """.trimIndent()
        )
    }

    fun takeFromWorktop(
        nonFungible: NonFungibleGlobalId,
        intoBucket: ManifestBucket
    ) = apply {
        instructions.appendLine(
            """
            TAKE_NON_FUNGIBLES_FROM_WORKTOP
                Address("${nonFungible.resourceAddress().addressString()}")
                Array<NonFungibleLocalId>(NonFungibleLocalId("${nonFungible.localId().asStr()}"))
                Bucket("${intoBucket.value}")
            ;   
            """.trimIndent()
        )
    }

    fun deposit(
        toAccount: Address,
        fromBucket: ManifestBucket
    ) = apply {
        instructions.appendLine(
            """
            CALL_METHOD
                Address("${toAccount.addressString()}")
                "${ManifestMethod.TryDepositOrAbort.value}"
                Bucket("${fromBucket.value}")
            ;   
            """.trimIndent()
        )
    }

    fun build(networkId: Int) = with(blobs.map { it.toUByteList() }) {
        TransactionManifest(
            instructions = Instructions.fromString(
                string = instructions.toString(),
                blobs = this,
                networkId = networkId.toUByte()
            ),
            blobs = this
        )
    }

    fun newBucket() = ManifestBucket(value = latestBucketIndex + 1u).also {
        latestBucketIndex += 1u
    }

}

fun NonFungibleLocalId.asStr() = when (this) {
    is NonFungibleLocalId.Bytes -> "[${value.toByteArray()}]"
    is NonFungibleLocalId.Integer -> "#$value#"
    is NonFungibleLocalId.Str -> "<$value>"
    is NonFungibleLocalId.Uuid -> "{$value}"
}
