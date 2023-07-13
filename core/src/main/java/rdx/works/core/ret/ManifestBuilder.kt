package rdx.works.core.ret

import com.radixdlt.ret.Address
import com.radixdlt.ret.Decimal
import com.radixdlt.ret.Instructions
import com.radixdlt.ret.ManifestBucket
import com.radixdlt.ret.ManifestExpression
import com.radixdlt.ret.ManifestValue
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.TransactionManifest
import rdx.works.core.toByteArray
import rdx.works.core.toHexString
import rdx.works.core.toUByteList

class ManifestBuilder {
    private var instructions = mutableListOf<String>()
    private var blobs: MutableList<ByteArray> = mutableListOf()
    private var latestBucketIndex: UInt = 0u

    fun withdraw(
        instructionIndex: Int = instructions.size,
        fromAddress: Address,
        fungible: Address,
        amount: Decimal
    ) = apply {
        instructions.add(
            index = instructionIndex,
            """
            CALL_METHOD
                 Address("${fromAddress.addressString()}")
                "${ManifestMethod.Withdraw.value}"
                Address("${fungible.addressString()}")
                Decimal("${amount.asStr()}")
            """.trimIndent()
        )
    }

    fun withdraw(
        instructionIndex: Int = instructions.size,
        fromAddress: Address,
        nonFungible: NonFungibleGlobalId
    ) = apply {
        instructions.add(
            index = instructionIndex,
            """
            CALL_METHOD
                 Address("${fromAddress.addressString()}")
                "${ManifestMethod.WithdrawNonFungibles.value}"
                Address("${nonFungible.resourceAddress().addressString()}")
                Array<NonFungibleLocalId>(NonFungibleLocalId("${nonFungible.localId().asStr()}"))
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
            """.trimIndent()
        )
    }

    fun deposit(
        instructionIndex: Int = instructions.size,
        toAddress: Address,
        fromBucket: ManifestBucket
    ) = apply {
        instructions.add(
            index = instructionIndex,
            """
            CALL_METHOD
                Address("${toAddress.addressString()}")
                "${ManifestMethod.TryDepositOrAbort.value}"
                Bucket("${fromBucket.value}")
            """.trimIndent()
        )
    }

    fun depositBatch(
        instructionIndex: Int = instructions.size,
        toAddress: Address,
    ) = apply {
        instructions.add(
            index = instructionIndex,
            """
            CALL_METHOD
                Address("${toAddress.addressString()}")
                "${ManifestMethod.TryDepositBatchOrAbort.value}"
                Expression("${ManifestExpression.ENTIRE_WORKTOP}")
            """.trimIndent()
        )
    }

    fun freeXrd(
        instructionIndex: Int = instructions.size,
        faucetAddress: Address
    ) = apply {
        instructions.add(
            index = instructionIndex,
            """
            CALL_METHOD
                Address("${faucetAddress.addressString()}")
                "${ManifestMethod.Free.value}"
            """.trimIndent()
        )
    }

    fun lockFee(
        instructionIndex: Int = instructions.size,
        fromAddress: Address,
        fee: Decimal
    ) = apply {
        instructions.add(
            index = instructionIndex,
            """
            CALL_METHOD
                Address("${fromAddress.addressString()}")
                "${ManifestMethod.LockFee.value}"
                Decimal("${fee.asStr()}")
            """.trimIndent()
        )
    }

    fun setOwnerKeys(
        instructionIndex: Int = instructions.size,
        address: Address,
        keys: List<Pair<ManifestValue.U8Value, ByteArray>>
    ) = apply {
        instructions.add(
            index = instructionIndex,
            """
            SET_METADATA
                Address("${address.addressString()}")
                "owner_keys"
                Enum<${ManifestValue.U8Value(143u).value}>(
                    Array<Enum>(
                        ${keys.map { "Enum<${it.first.value}>(Bytes(${it.second.toHexString()}))" }}
                    ) 
                )
            """.trimIndent()
        )
    }

    fun build(networkId: Int) = with(blobs.map { it.toUByteList() }) {
        val instructionsStr = instructions.joinToString(separator = ";\n", postfix = ";")
        TransactionManifest(
            instructions = Instructions.fromString(
                string = instructionsStr,
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
