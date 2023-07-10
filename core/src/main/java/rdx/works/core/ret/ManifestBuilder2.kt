package rdx.works.core.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestBucket
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.toByteArray
import rdx.works.core.toUByteList

class ManifestBuilder2 {
    private var instructions = mutableListOf<String>()
    private var blobs: MutableList<ByteArray> = mutableListOf()
    private var latestBucketIndex: UInt = 0u

    fun withdraw(
        instructionIndex: Int = instructions.size,
        fromAccount: Address,
        fungible: Address,
        amount: Decimal
    ) = apply {
        instructions.add(
            index = instructionIndex,
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
        instructionIndex: Int = instructions.size,
        fromAccount: Address,
        nonFungible: NonFungibleGlobalId
    ) = apply {
        instructions.add(
            index = instructionIndex,
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
        instructionIndex: Int = instructions.size,
        fungible: Address,
        amount: Decimal,
        intoBucket: ManifestBucket
    ) = apply {
        instructions.add(
            index = instructionIndex,
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
        instructionIndex: Int = instructions.size,
        nonFungible: NonFungibleGlobalId,
        intoBucket: ManifestBucket
    ) = apply {
        instructions.add(
            index = instructionIndex,
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
        instructionIndex: Int = instructions.size,
        toAccount: Address,
        fromBucket: ManifestBucket
    ) = apply {
        instructions.add(
            index = instructionIndex,
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
                string = instructions.joinToString(separator = "\n"),
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
